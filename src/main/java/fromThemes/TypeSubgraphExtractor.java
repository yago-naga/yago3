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
public class TypeSubgraphExtractor extends Extractor {

  /** All types of YAGO */
  public static final Theme YAGOTYPES = new Theme("yagoTypes", "The coherent types extracted from different wikipedias, potentially filtered by subgraph.", Theme.ThemeGroup.TAXONOMY);

  public static final Theme YAGOTYPESSOURCES = new Theme("yagoTypesSources", "Sources for the coherent types extracted from different wikipedias, potentially filtered by subgraph.");

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CoherentTypeExtractor.TYPES, CoherentTypeExtractor.TYPESSOURCES);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOTYPES, YAGOTYPESSOURCES);
  }

  @Override
  public void extract() throws Exception {
    Set<String> entitySubgraph = TransitiveTypeExtractor.getSubgraphEntities();

    Announce.doing("Extracting supgraph from types from ", CoherentTypeExtractor.TYPES);
    for (Fact f : CoherentTypeExtractor.TYPES) {
      if (TransitiveTypeSubgraphExtractor.checkInSubgraph(f, entitySubgraph)) {
        YAGOTYPES.write(f);
      }
    }
    Announce.done();

    Announce.doing("Extracting supgraph from types from ", CoherentTypeExtractor.TYPESSOURCES);
    for (Fact f : CoherentTypeExtractor.TYPESSOURCES) {
      if (TransitiveTypeSubgraphExtractor.checkInSubgraph(f, entitySubgraph)) {
        YAGOTYPESSOURCES.write(f);
      }
    }
    Announce.done();
  }
}
