package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.N4Reader;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import extractors.MultilingualExtractor;
import fromThemes.TransitiveTypeExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * WikidataImageExtractor
 * 
 * Extract images from wikidata
 * 
 * @author Ghazaleh Haratinezhad
 *
 */
public class WikidataImageExtractor extends DataExtractor {

	private static final String WIKIDATA_STATEMENTS = "wikidata_statements";
	
	public static final Theme WIKIDATAIMAGES = new Theme("wikidataImages", 
			"Images in wikidata dump for entities");
	
	//How to keep these?
	private static final Map<String, List<String>> imageRelations;
	static {
		Map<String, List<String>> tempMap = new HashMap<String, List<String>>();
		tempMap.put("person", Arrays.asList(ImageTypes.image, ImageTypes.imageOfGrave, ImageTypes.coatOfArmsImage, ImageTypes.signature));
		tempMap.put("organization", Arrays.asList(ImageTypes.logoImage, ImageTypes.coatOfArmsImage, ImageTypes.image));
		tempMap.put("event", Arrays.asList(ImageTypes.image));
		tempMap.put("artifact", Arrays.asList(ImageTypes.logoImage, ImageTypes.image));
		tempMap.put("location", Arrays.asList(ImageTypes.flagImage, ImageTypes.sealImage, ImageTypes.coatOfArmsImage,
											  ImageTypes.detailMap, ImageTypes.locatorMapImage, ImageTypes.image));
		tempMap.put("other", Arrays.asList(ImageTypes.image));
		
		imageRelations = Collections.unmodifiableMap(tempMap);
	}
		
	public WikidataImageExtractor(File wikidata) {
		super(wikidata);
	}
	
	public WikidataImageExtractor() {
		this(Parameters.getFile(WIKIDATA_STATEMENTS));
	}

	@Override
	public Set<Theme> input() {
		return (new FinalSet<>(WikidataLabelExtractor.WIKIDATAINSTANCES, TransitiveTypeExtractor.TRANSITIVETYPE));
	}

	@Override
	public Set<Theme> output() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(WIKIDATAIMAGES);
		return result;
	}

	@Override
	public void extract() throws Exception {
		Map<String, String> wikiDataIdEntityMap = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getReverseMap(RDFS.sameas);
		N4Reader nr = new N4Reader(inputData);
		String yagoEntity = null;
		Fact prevImage = null;
		Map<String, String> images = new HashMap<String, String>();
		while(nr.hasNext()) {
			Fact f = nr.next();
			
			if(f.getObject().endsWith("#Item>")){
				if(!images.isEmpty()){
					String image = null;
					String category = getHighlevelCategory(yagoEntity);
					for(String key : imageRelations.get(category)){
						if(images.containsKey(key)){
							image = images.get(key);
							break;
						}
					}
					// If there were some image(s) but not in the expected types, select first one
					if(image == null){
						image = images.entrySet().iterator().next().getValue();
					}
					WIKIDATAIMAGES.write(new Fact(yagoEntity, YAGO.hasWikiDataImageUrl, image));
				}
				images.clear();
				yagoEntity = wikiDataIdEntityMap.get(f.getSubject());
				prevImage = null;
			}
			// Select the first image per relation for an entity, unless there is a PreferredRank which is replaced
			else if(f.getObject().matches(".*(jpg|jpeg|png|svg|tif|tiff|gif)>$") && yagoEntity != null){
				if(!images.containsKey(f.getRelation()))
					images.put(f.getRelation(), f.getObject());
				prevImage = f;
			}
			else if(prevImage != null && prevImage.getSubject().equals(f.getSubject()) && f.getRelation().endsWith("#rank>")){
				if(f.getObject().endsWith("#PreferredRank>"))
					images.put(prevImage.getRelation(), prevImage.getObject());
				prevImage = null;
			}
		}
		nr.close();
	}
	
	private String getHighlevelCategory(String entity) throws IOException {
		Set<Fact> facts = TransitiveTypeExtractor.TRANSITIVETYPE.factCollection().getFactsWithSubjectAndRelation(entity, RDFS.type);
		String category = "other";
		
		for (Fact fact:facts){
			String factObject = fact.getObject();
			if (factObject.contains("person")){
				category = "person";
				break;
			}
			if (factObject.contains("location")){
				category = "location";
				break;
			}
			if (factObject.contains("organization")){
				category = "organization";
				break;
			}
			if (factObject.contains("artifact")){
				category = "artifact";
				break;
			}
			if (factObject.contains("event")){
				category = "event";
				break;
			}		
		}
		
		return category;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
