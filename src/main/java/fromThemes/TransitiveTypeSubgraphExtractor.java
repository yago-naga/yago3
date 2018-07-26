package fromThemes;

import basics.Fact;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;

import java.util.Set;

/**
 * Extracts the type subgraph specified by subgraphEntities and subgraphClasses in the yago.ini
 */
public class TransitiveTypeSubgraphExtractor extends Extractor {

  /** All type facts */
  public static final Theme YAGOTRANSITIVETYPE = new Theme("yagoTransitiveTypes", "Transitive closure of all rdf:type/rdfs:subClassOf facts, potentially filtered by subgraph.",
          Theme.ThemeGroup.TAXONOMY);

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

  public static boolean checkInSubgraph(Fact f, Set<String> entitySubgraph) {
    if (entitySubgraph != null && !entitySubgraph.isEmpty()) {
      return (entitySubgraph.contains(f.getSubject()) || entitySubgraph.contains(f.getObject()));
    } else {
      // If there is no subgraph restriction, write all facts.
      return true;
    }
  }
}
