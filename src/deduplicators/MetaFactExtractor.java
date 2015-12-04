package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromThemes.RuleExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.TemporalInfoboxExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all meta facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class MetaFactExtractor extends SimpleDeduplicator {

  @Override
  public List<Theme> inputOrdered() {
    return Arrays.asList(SchemaExtractor.YAGOSCHEMA, HardExtractor.HARDWIREDFACTS, RuleExtractor.RULERESULTS, FlightExtractor.FLIGHTS,
    //				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }

  /** All meta facts of YAGO */
  public static final Theme YAGOMETAFACTS = new Theme("yagoMetaFacts",
      "All temporal and geospatial meta facts of YAGO, complementing the CORE facts", ThemeGroup.META);

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
    if (fact.getRelation().startsWith("<_")) return (false);
    if (relationsExcluded.contains(fact.getRelation())) return (false);
    return (FactComponent.isFactId(fact.getArg(1)));
  }
}
