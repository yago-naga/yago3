/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Ghazaleh Haratinezhad Torbati.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/

package fromOtherSources;

import basics.*;
import extractors.DataExtractor;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromThemes.TransitiveTypeSubgraphExtractor;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.FactCollection;
import utils.Theme;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract images for entities from Wikidata dump.
 * 
*/

public class WikidataImageExtractor extends DataExtractor {

	public static final Theme WIKIDATAIMAGES = new Theme("wikidataImages", 
	    "Images in wikidata dump for entities");
	
	public static final Theme WIKIDATAIMAGESNEEDSTYPECHECK = new Theme("wikidataImagesNeedsTypeCheck", 
      "Images in wikidata dump for entities");

	private static final String WIKIDATA = "wikidata";
	 
	private static final String IMAGE_ORIGINALURL_TEMPLATE = "https://upload.wikimedia.org/wikipedia/commons/";
	private static final String IMAGETYPE = "_image_";
	
  private static Map<String, Set<String>> transitiveTypes = null;
	private static FactCollection reverseWikidataInstances = new FactCollection();
	
	// Order of image relations to use for each entity category.
	private static final Map<String, List<String>> imageRelationsInOrder;
	static {
		Map<String, List<String>> tempMap = new HashMap<String, List<String>>();
		tempMap.put("person", Arrays.asList(ImageTypes.image, ImageTypes.imageOfGrave, ImageTypes.coatOfArmsImage, ImageTypes.signature));
		tempMap.put("organization", Arrays.asList(ImageTypes.logoImage, ImageTypes.coatOfArmsImage, ImageTypes.image));
		tempMap.put("event", Arrays.asList(ImageTypes.image));
		tempMap.put("artifact", Arrays.asList(ImageTypes.logoImage, ImageTypes.image));
		tempMap.put("location", Arrays.asList(ImageTypes.flagImage, ImageTypes.sealImage, ImageTypes.coatOfArmsImage,
											  ImageTypes.detailMap, ImageTypes.locatorMapImage, ImageTypes.image));
		tempMap.put("other", Arrays.asList(ImageTypes.image));
		
		imageRelationsInOrder = Collections.unmodifiableMap(tempMap);
	}
		
	public WikidataImageExtractor(File wikidata) {
		super(wikidata);
	}
	
	public WikidataImageExtractor() {
		this(Parameters.getFile(WIKIDATA));
	}

	@Override
	public Set<Theme> input() {
		return (new FinalSet<>(WikidataLabelExtractor.WIKIDATAINSTANCES, TransitiveTypeSubgraphExtractor.YAGOTRANSITIVETYPE));
	}

	@Override
	public Set<Theme> output() {
		return (new FinalSet<>(WIKIDATAIMAGESNEEDSTYPECHECK, WIKIDATAIMAGES));
	}
	
  @Override
  public Set<followUp.FollowUpExtractor> followUp() {
    Set<FollowUpExtractor> result = new HashSet<FollowUpExtractor>();
    
    result.add(new TypeChecker(WIKIDATAIMAGESNEEDSTYPECHECK, WIKIDATAIMAGES));
    
    return result;
  }

	@Override
	public void extract() throws Exception {
	  // Example of the facts in reverseWikidataInstances:
	  // <http://www.wikidata.org/entity/Q23>  owl:sameAs <George_Washington>      
	  reverseWikidataInstances = WikidataLabelExtractor.WIKIDATAINSTANCES.factCollection().getReverse();
    transitiveTypes = TransitiveTypeSubgraphExtractor.getSubjectToTypes();
		
		N4Reader nr = new N4Reader(inputData);
		String yagoEntityMostEnglish = null;
		Fact prevImage = null;
		int imageCounter = 1;
		Map<String, String> images = new HashMap<String, String>();
		while(nr.hasNext()) {
			Fact f = nr.next();
			 
			// We reached a new entity and until the next appearance of "#Item", the statements are about this entity.
			// example: <http://www.wikidata.org/entity/Q1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wikidata.org/ontology#Item>
			if(f.getObject().endsWith("#Item>")) {
				if(!images.isEmpty()) {
				  String image = Char17.decodePercentage(FactComponent.stripBrackets(pickImage(yagoEntityMostEnglish, images))).replaceAll(" ", "_");
					String originalUrl = getOriginalImageUrl(image);
					
					// Get image's wiki page:
					String imageWikipage = image.replace("wiki/Special:FilePath/", "wiki/File:");
					String imageID = FactComponent.forYagoEntity(IMAGETYPE + imageCounter);
					imageCounter++;
					
					// Saving imageID along with image wiki page url and image original url to the theme
					WIKIDATAIMAGESNEEDSTYPECHECK.write(new Fact(yagoEntityMostEnglish, YAGO.hasImageID, imageID));
					WIKIDATAIMAGESNEEDSTYPECHECK.write(new Fact(imageID, YAGO.hasWikiPage, FactComponent.forUri(imageWikipage)));
					WIKIDATAIMAGESNEEDSTYPECHECK.write(new Fact(imageID, YAGO.hasImageUrl, FactComponent.forUri(originalUrl)));
	        images.clear();
				}
				// reverseWikidataInstances facts like: <http://www.wikidata.org/entity/Q23>  owl:sameAs <George_Washington>
				yagoEntityMostEnglish = getMostEnglishEntityName(reverseWikidataInstances.getFactsWithSubjectAndRelation(f.getSubject(), RDFS.sameas));
				prevImage = null;
			}
			
			// Select the first image per relation for an entity, unless there is a PreferredRank.
			// Format of regular expression is taken from: https://www.wikidata.org/wiki/Property:P18
			// example: <some random id> <https://www.wikidata.org/wiki/Property:P18> <filename.jpg> .
			else if(f.getObject().matches(".*commons\\.wikimedia.*\\.(jpg|jpeg|png|svg|tif|tiff|gif)>$") && yagoEntityMostEnglish != null) {
				if(!images.containsKey(f.getRelation())) {
					images.put(getImageRelationType(f.getRelation()), Char17.decodeBackslash(f.getObject()));
				}
				prevImage = f;
			}
			// If the rank of the image was "PreferredRank", replace the existing image with this preferred image.
			// example : <random id> <http://www.wikidata.org/ontology#rank> <http://www.wikidata.org/ontology#NormalRank> . 
			else if(prevImage != null && prevImage.getSubject().equals(f.getSubject()) && f.getRelation().endsWith("#rank>")) {
				if(f.getObject().endsWith("#PreferredRank>")) {
					images.put(getImageRelationType(prevImage.getRelation()), prevImage.getObject());
				}
				prevImage = null;
			}
		}
		
    // Saving information of the last entity in the file
		if (!images.isEmpty() && yagoEntityMostEnglish != null) {
		  String image = Char17.decodePercentage(pickImage(yagoEntityMostEnglish, images)).replaceAll(" ", "_");
      String originalUrl = getOriginalImageUrl(FactComponent.stripBrackets(image));
      String imageWikipage = image.replace("wiki/Special:FilePath/", "wiki/File:");
		  String imageID = FactComponent.forYagoEntity(IMAGETYPE + imageCounter);
		  
		  WIKIDATAIMAGESNEEDSTYPECHECK.write(new Fact(yagoEntityMostEnglish, YAGO.hasImageID, imageID));
		  WIKIDATAIMAGESNEEDSTYPECHECK.write(new Fact(imageID, YAGO.hasWikiPage, FactComponent.forUri(imageWikipage)));
		  WIKIDATAIMAGESNEEDSTYPECHECK.write(new Fact(imageID, YAGO.hasImageUrl, FactComponent.forUri(originalUrl)));
      images.clear();
		}
		
		nr.close();
		
	}
	
	/**
	 * Retrun the image type.
	 * Example: P18
	 * 
	 * @param relationUrl The relation url to find which relation it is from.
	 * @return The relation type.
	 */
	private String getImageRelationType(String relationUrl) {
	  Pattern relationPattern = Pattern.compile("<http:\\/\\/www.wikidata.org\\/(?:entity|prop)\\/(?:direct|statement|qualifier)\\/(P\\d+)>");
    Matcher matcher = relationPattern.matcher(relationUrl);
    if(matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
	 * Return the most English entity name given all entity names available.
	 * 
	 * @param entityFacts yago entity in different languages.
	 * @return The most English entity name.
	 */
	private static String getMostEnglishEntityName(Set<Fact> entityFacts) {
	  // Map of entity names for each language 
    Map<String, String> languageEntityName = new HashMap<>();
    // each entityFact is like: <http://www.wikidata.org/entity/Q23>  owl:sameAs <George_Washington>
    for(Fact f:entityFacts) {
      String language = FactComponent.getLanguageOfEntity(f.getObject());
      if (language != null) {
        languageEntityName.put(language, f.getObject());
      }
      else {
        languageEntityName.put("en", f.getObject());
      }
    }
    
    String mostEnglishLanguage = DictionaryExtractor.mostEnglishLanguage(languageEntityName.keySet());
    return languageEntityName.get(mostEnglishLanguage);
	}
	
	
	/**
	 * Pick the best image with regard to its category using manual order for images for each category.
	 * 
	 * @param yagoEntity The yago entity.
	 * @param images The images extracted for the yagoEntity.
	 * @return Picked image for the input entity.
	 * @throws IOException
	 */
	private static String pickImage(String yagoEntity, Map<String, String> images) throws IOException {
	  String image = null;
	  String category = getHighlevelCategory(yagoEntity);
    for(String key : imageRelationsInOrder.get(category)) {
      if(images.containsKey(key)) {
        image = images.get(key);
        break;
      }
    }
    // If there were some image(s) but not in the expected types, select first one.
    if(image == null) {
      image = images.entrySet().iterator().next().getValue();
    }
	  return image;
	}
	
	
	/**
	 * To make the image's original url, we use the first 2 characters of md5 hashed version of the file name.
   * example: input= "http://commons.wikimedia.org/wiki/Special:FilePath/Spelterini_Blüemlisalp.jpg"
   * file name = "Spelterini_Blüemlisalp.jpg" hashed = "ae1a26d34d6a674d4400c8a1e6fe73f8"
   * original url = https://upload.wikimedia.org/wikipedia/commons/a/ae/Spelterini_Bl%C3%BCemlisalp.jpg
   * 
   * @see https://commons.wikimedia.org/wiki/Commons:FAQ#What_are_the_strangely_named_components_in_file_paths.3F 
	 * @param wikiUrl Url to image's wiki page.
	 * @return Image's original url .
	 * @throws NoSuchAlgorithmException
	 */
	public static String getOriginalImageUrl(String wikiUrl) throws NoSuchAlgorithmException {
	  MessageDigest md = MessageDigest.getInstance("MD5");
	  
	  String imageName = wikiUrl.substring(wikiUrl.indexOf("/wiki/Special:FilePath/") + "/wiki/Special:FilePath/".length());
	  
	  StringBuffer hashedName = new StringBuffer();
	  md.update(imageName.getBytes(StandardCharsets.UTF_8));
	  byte byteData[] = md.digest();
	  
	  for (int i = 0; i < byteData.length; i++) {
	    hashedName.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
	  }
    return (IMAGE_ORIGINALURL_TEMPLATE + hashedName.charAt(0) + "/" + hashedName.charAt(0) + hashedName.charAt(1) + "/" + imageName);
	}
	
	/**
	 * Return the high level category of the entity based on yago transitive types.
	 * 
	 * @param entity Yago entity.
	 * @return High level category of the entity.
	 * @throws IOException
	 */
	private static String getHighlevelCategory(String entity) throws IOException {
    Set<String> types = transitiveTypes.get(entity);
		String category = "other";
		
		if (types != null) {
      for (String type : types) {
        if (type.contains("person")) {
  				category = "person";
  				break;
  			}
        if (type.contains("location")) {
  				category = "location";
  				break;
  			}
        if (type.contains("organization")) {
  				category = "organization";
  				break;
  			}
        if (type.contains("artifact")) {
  				category = "artifact";
  				break;
  			}
        if (type.contains("event")) {
  				category = "event";
  				break;
  			}		
  		}
		}
		return category;
	}
}
