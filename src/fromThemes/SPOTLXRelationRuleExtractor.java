package fromThemes;

import java.util.Map;
import java.util.Set;

import basics.FactCollection;
import basics.FactSource;
import basics.Theme;

import fromOtherSources.PatternHardExtractor;
import fromGeonames.GeoNamesDataImporter;

import javatools.datatypes.FinalSet;

/**
 * YAGO2s - SPOTLXRuleExtractor
 * 
 * Generates the results of rules needed in the SPOTLX representation.
 * 
 * @author Joanna Biega
 */
public class SPOTLXRelationRuleExtractor extends BaseRuleExtractor {
  
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.SPOTLX_ENTITY_RULES,
                          PatternHardExtractor.HARDWIREDFACTS,
                          FactExtractor.YAGOFACTS,
                          MetaFactExtractor.YAGOMETAFACTS,
                          LiteralFactExtractor.YAGOLITERALFACTS,
                          GeoNamesDataImporter.GEONAMESDATA);
  }
  
  /** Themes of spotlx deductions */
  public static final Theme RULERESULTS = new Theme("spotlxRelationFacts", "SPOTLX deduced facts");
  public static final Theme RULESOURCES = new Theme("spotlxRelationSources", "SPOTLX deduced facts");
  
  public Theme getRULERESULTS() {
    return RULERESULTS;
  }
  
  public Theme getRULESOURCES() {
    return RULESOURCES;
  }
  
  @Override
  public Set<Theme> output() {
    return new FinalSet<>(RULERESULTS, RULESOURCES);
  }
  
  @Override
  public FactCollection getInputRuleCollection(Map<Theme, FactSource> input) throws Exception {
    FactSource spotlxRelationRules = input.get(PatternHardExtractor.SPOTLX_ENTITY_RULES);
    FactCollection collection = new FactCollection(spotlxRelationRules);
    return collection;
  }
  
}