package finalExtractors;

import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.InfoboxExtractor;
import extractors.PersonNameExtractor;
import extractors.RuleExtractor;
import extractors.TemporalInfoboxExtractor;

/**
 * YAGO2s - SourceExtractor
 * 
 * Deduplicates all source facts. This extractor is different from FactExtractor so that it can run in parallel. 
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class SourceExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(InfoboxExtractor.INFOBOXSOURCES,PersonNameExtractor.PERSONNAMESOURCES,
        RuleExtractor.RULESOURCES,CategoryExtractor.CATEGORYSOURCES,
        TemporalInfoboxExtractor.TEMPORALINFOBOXSOURCES);
  }

  /** All source facts of YAGO */
  public static final Theme YAGOSOURCES = new Theme("yagoSources", "All sources of YAGO facts", ThemeGroup.META);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOSOURCES);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    Announce.doing("Extracting sources");
    FactWriter w=output.get(YAGOSOURCES);
    for(Theme theme : input.keySet()) {
      Announce.doing("Extracting sources from",theme);
      for(Fact fact : input.get(theme)) {
        if(fact.getRelation().equals(YAGO.extractionSource) || fact.getRelation().equals(YAGO.extractionTechnique)) {
          w.write(fact);
        }
      }
      Announce.done();
    }
    Announce.done();
  }
}
