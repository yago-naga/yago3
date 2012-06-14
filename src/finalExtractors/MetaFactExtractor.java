package finalExtractors;

import java.io.File;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import basics.YAGO;
import basics.Theme.ThemeGroup;
import extractors.HardExtractor;
import extractors.RuleExtractor;
import extractors.TemporalCategoryExtractor;
import extractors.TemporalInfoboxExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class MetaFactExtractor extends Deduplicator {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS, 
                RuleExtractor.RULERESULTS,                 
                TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }
  /** All meta facts of YAGO */
  public static final Theme YAGOMETAFACTS = new Theme("yagoMetaFacts", "All temporal and geospatial meta facts of YAGO. This complements the raw facts of yagoFacts.", ThemeGroup.META);

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(YAGO.extractionSource, YAGO.extractionTechnique);

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new MetaFactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }

  @Override
  public Theme myOutput() {
    return YAGOMETAFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if(fact.getRelation().startsWith("<_")) return(false);
    if (relationsExcluded.contains(fact.getRelation())) return (false);
    return (FactComponent.isFactId(fact.getArg(1)));
  }
}
