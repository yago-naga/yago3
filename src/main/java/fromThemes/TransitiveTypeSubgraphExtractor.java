package fromThemes;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts the type subgraph specified by subgraphEntities and subgraphClasses in the yago.ini
 */
public class TransitiveTypeSubgraphExtractor extends Extractor {

  /** All type facts */
  public static final Theme YAGOTRANSITIVETYPE = new Theme("yagoTransitiveTypes", "Transitive closure of all rdf:type/rdfs:subClassOf facts, potentially filtered by subgraph.",
          Theme.ThemeGroup.TAXONOMY);

  /** Cache for transitive type */
  protected static SoftReference<Map<String, Set<String>>> cache = new SoftReference<>(null);

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOTRANSITIVETYPE);
  }

  @Override
  public void extract() throws Exception {
    Set<String> entitySubgraph = TransitiveTypeExtractor.getSubgraphEntities();

    Announce.doing("Extracting supgraph from transitive types from ", TransitiveTypeExtractor.TRANSITIVETYPE);
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (checkInSubgraph(f, entitySubgraph)) {
        YAGOTRANSITIVETYPE.write(f);
      }
    }
    Announce.done();
  }

  public static synchronized Map<String, Set<String>> getSubjectToTypes() {
    Announce.doing("Loading transitive type");
    Map<String, Set<String>> map = cache.get();
    if (map == null) {
      cache = new SoftReference<>(map = new HashMap<>());
      for (Fact f : YAGOTRANSITIVETYPE) {
        if (RDFS.type.equals(f.getRelation())) {
          map.computeIfAbsent(f.getSubject(), k -> new HashSet<>()).add(f.getObject());
        }
      }
    }
    return map;
  }

  /**
   * Checks if either subject or object of fact are part of the entity subgraph to keep. If so, returns true,
   * otherwise false. Keeps all metafacts and internal entities (starting with _).
   *
   * @param f Fact to check.
   * @param subgraph  Entity subgraph to check fact against.
   * @return  True if f is in entitySubgraph or if f is a meta fact, false otherwise.
   */
  public static boolean checkInSubgraph(Fact f, Set<String> subgraph) {
    if (subgraph != null && !subgraph.isEmpty()) {
      return (
              subgraph.contains(f.getSubject()) ||
              subgraph.contains(f.getObject()) ||
              FactComponent.isFactId(f.getSubject()) ||
              FactComponent.isEmptyNode(f.getSubject()));
    } else {
      // If there is no subgraph restriction, write all facts.
      return true;
    }
  }
}
