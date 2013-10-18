package fromThemes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import basics.Theme.ThemeGroup;
import fromWikipedia.Extractor;
import fromWikipedia.WikipediaTypeExtractor;

/**
 * YAGO2s - TransitiveTypeExtractor
 * 
 * Extracts all transitive rdf:type facts.
 * 
 * It also loads these transitive facts directly into memory to save time for the following extractors. You can free this memory by saying freeMemory().
 * 
 * @author Fabian M. Suchanek
 *
 */
public class TransitiveTypeExtractor extends Extractor {

  /** We cache the entire YAGO taxonomy here for later calls. This may be too large. Call freeMamory() to free cache*/
  protected static Map<String, Set<String>> yagoTaxonomy = null;

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(ClassExtractor.YAGOTAXONOMY, WikipediaTypeExtractor.YAGOTYPES);
  }

  /** All type facts*/
  public static final Theme TRANSITIVETYPE = new Theme("yagoTransitiveType", "Transitive closure of all rdf:type/rdfs:subClassOf facts",
      ThemeGroup.TAXONOMY);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(TRANSITIVETYPE);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactCollection classes = new FactCollection(input.get(ClassExtractor.YAGOTAXONOMY),true);
    yagoTaxonomy = new HashMap<>();
    Announce.doing("Computing the transitive closure");
    for (Fact f : input.get(WikipediaTypeExtractor.YAGOTYPES)) {
      if (f.getRelation().equals(RDFS.type)) {
        D.addKeyValue(yagoTaxonomy, f.getArg(1), f.getArg(2), TreeSet.class);
        for (String c : classes.superClasses(f.getArg(2))) {
          D.addKeyValue(yagoTaxonomy, f.getArg(1), c, TreeSet.class);
        }
      }
    }
    Announce.done();
    Announce.doing("Writing data");
    FactWriter w = output.get(TRANSITIVETYPE);
    for (Entry<String, Set<String>> type : yagoTaxonomy.entrySet()) {
      for (String c : type.getValue()) {
    	Fact f = new Fact(type.getKey(), RDFS.type, c);
    	f.makeId();
        w.write(f);
      }
    }
    Announce.done();
    Announce.done();
  }

  /** Loads and returns the entire transitive YAGO taxonomy. It is being loaded by default already when it's being written. This may be large, so discard if you don't need it.*/
  public static Map<String, Set<String>> yagoTaxonomy(Map<Theme, FactSource> input) {
    return (yagoTaxonomy(input.get(TRANSITIVETYPE)));
  }

  /** Returns all YAGO entities. This is being loaded by default already when it's being written. This may be large, so discard if you don't need it.*/
  public static Set<String> entities(Map<Theme, FactSource> input) {
    return (yagoTaxonomy(input).keySet());
  }

  /** Frees the memory of the yagoTaxonomy*/
  public synchronized static void freeMemory() {
    yagoTaxonomy = null;
  }

  /** Loads and returns the entire transitive YAGO taxonomy. It is being loaded by default already when it's being written. This may be large, so discard if you don't need it.*/
  public synchronized static Map<String, Set<String>> yagoTaxonomy(FactSource transitiveTaxonomy) {
    if (yagoTaxonomy != null) return (yagoTaxonomy);
    yagoTaxonomy = new HashMap<>();
    Announce.doing("Loading entire transitive YAGO taxonomy");
    for (Fact f : transitiveTaxonomy) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      D.addKeyValue(yagoTaxonomy, f.getArg(1), f.getArg(2), TreeSet.class);
    }
    Announce.done();
    return (yagoTaxonomy);
  }

  public static void main(String[] args) throws Exception {
    new TransitiveTypeExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
