package extractors;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalMap;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;

/**
 * YAGO2s - SimpleTaxonomyExtractor 
 * 
 * Extracts a simple taxonomy of just Wikicategories, wordnet leaves and main branches
 * 
 * @author Fabian M. Suchanek
 *
 */
public class SimpleTaxonomyExtractor extends Extractor {

	/** Branches of YAGO*/
	public static final Set<String> yagoBranches=new FinalSet<>("<wordnet_person_100007846>","<wordnet_location_100027167>","<wordnet_artifact_100021939>",
		      "<wordnet_physical_entity_100001930>",
		      "<wordnet_abstraction_100002137>");
	
	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYCLASSES,WordnetExtractor.WORDNETCLASSES);
	}

	/** Simple taxonomy*/
	public static final Theme SIMPLETAXONOMY=new Theme("yagoSimpleTaxonomy");
	
	@Override
	public Map<Theme, String> output() {
		return new FinalMap<>(SIMPLETAXONOMY,"A subset if the YAGO taxonomy");
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
	    FactCollection wordnet=new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES));
	    Set<String> done=new TreeSet<>();
	    Announce.doing("Extracting simple taxonomy");
	    for(String b : yagoBranches) {
			output.get(SIMPLETAXONOMY).write(new Fact(b,RDFS.subclassOf,YAGO.entity));
	    }
	    for(Fact f : input.get(CategoryExtractor.CATEGORYCLASSES)) {
	    	if(!f.getRelation().equals(RDFS.subclassOf)) continue;
	    	if(done.contains(f.getArg(1))) continue;
	    	done.add(f.getArg(1));
	    	if(done.contains(f.getArg(2))) {
		    	output.get(SIMPLETAXONOMY).write(f);
	    		continue;
	    	}
	    	Set<String> supr=wordnet.superClasses(f.getArg(2));
	    	supr.retainAll(yagoBranches);
	    	if(supr.isEmpty()) continue;
	    	String superBranch=supr.contains("<wordnet_person_100007846>")?"<wordnet_person_100007846>":D.pick(supr); 
	    	output.get(SIMPLETAXONOMY).write(f);
	    	output.get(SIMPLETAXONOMY).write(new Fact(f.getArg(2),RDFS.subclassOf,superBranch));
	    	done.add(f.getArg(2));
	    }
	    Announce.done();
	}

	public static void main(String[] args) throws Exception {
		new SimpleTaxonomyExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
