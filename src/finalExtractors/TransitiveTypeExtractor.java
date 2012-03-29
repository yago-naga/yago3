package finalExtractors;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.Extractor;

/**
 * YAGO2s - TransitiveTypeExtractor
 * 
 * Extracts all transitive rdf:type facts.
 * 
 * @author Fabian M. Suchanek
 *
 */
public class TransitiveTypeExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(TypeExtractor.YAGOTAXONOMY,TypeExtractor.YAGOTYPES);
	}

	/** All type facts*/
	public static final Theme TRANSITIVETYPE=new Theme("yagoTransitiveType","Transitive closure of all type/subclassof facts");
	
	@Override
	public Set<Theme> output() {	
		return new FinalSet<>(TRANSITIVETYPE);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		FactCollection classes=new FactCollection(input.get(TypeExtractor.YAGOTAXONOMY));
		Announce.doing("Computing the transitive closure");
		for(Theme theme : Arrays.asList(TypeExtractor.YAGOTYPES)) {
			Announce.doing("Treating entities in",theme);
			String lastEntity=null;
			Set<String> lastClasses=new TreeSet<>();
			for(Fact f : input.get(theme)) {
				if(f.getRelation().equals(RDFS.type)) {
					if(lastEntity==null) lastEntity=f.getArg(1);
					else if(!lastEntity.equals(f.getArg(1))) {
						flush(lastEntity,lastClasses,output.get(TRANSITIVETYPE));
						lastEntity=f.getArg(1);
						lastClasses.clear();
					}
					classes.superClasses(f.getArg(2), lastClasses);
					lastClasses.remove(f.getArg(2));
				}
			}
			flush(lastEntity,lastClasses,output.get(TRANSITIVETYPE));
			Announce.done();
		}
		Announce.done();
	}

	/** Writes the rdf:type facts*/
	protected static void flush(String lastEntity, Set<String> lastClasses, FactWriter factWriter) throws IOException {
		for(String clss : lastClasses) {
		   factWriter.write(new Fact(lastEntity,RDFS.type,clss));
		}
	}

  /** Loads and returns the entire transitive YAGO taxonomy. This may be large, so discard properly after use.*/
  public static Map<String,Set<String>> yagoTaxonomy(Map<Theme,FactSource> input) {
    return(yagoTaxonomy(input.get(TRANSITIVETYPE)));
  }
  
	 /** Loads and returns the entire transitive YAGO taxonomy. This may be large, so discard properly after use.*/
	public static Map<String,Set<String>> yagoTaxonomy(FactSource transitiveTaxonomy) {
	  Map<String,Set<String>> taxonomy=new TreeMap<>();
	  Announce.doing("Loading entire transitive YAGO taxonomy");
	  for(Fact f : transitiveTaxonomy) {
	    if(!f.getRelation().equals(RDFS.type)) continue;
	    D.addKeyValue(taxonomy, f.getArg(1), f.getArg(2),TreeSet.class);
	  }
	  Announce.done();
	  return(taxonomy);
	}
	
	public static void main(String[] args) throws Exception {
		new TransitiveTypeExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
