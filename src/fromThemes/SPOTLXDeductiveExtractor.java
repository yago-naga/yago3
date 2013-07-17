package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.FactSource;
import basics.FactCollection;
import basics.Theme;

import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.Extractor;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;


/**
 * YAGO2s - SPOTLXDeductiveExtractor
 * 
 * A SPOTLX extractor which follows the SPOTLSRuleExtractor. 
 * It uses previously generated general creation&destruction facts
 * and infers time&location facts (occursSince, occursUntil, occursIn).   
 * 
 * @author Joanna Biega
 */
public class SPOTLXDeductiveExtractor extends BaseRuleExtractor {
	
  private int maxRuleSetSize;
	
  public SPOTLXDeductiveExtractor(int maxRuleSize) {
	  maxRuleSetSize = maxRuleSize;
  }
  
  public SPOTLXDeductiveExtractor() {
	  /*Defaults to 0, which means the number of rules processed at once is unlimited*/
	  maxRuleSetSize = 0;
  }
  
  @Override
  public int maxRuleSetSize() {
	  return maxRuleSetSize;
  }
  
  @Override
  public Set<Extractor> followUp() {
    return(new HashSet<Extractor> (Arrays.asList(new SPOTLXDeduplicator())));
  }
  
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.HARDWIREDFACTS,
                          PatternHardExtractor.SPOTLX_ENTITY_RULES,
                          PatternHardExtractor.SPOTLX_FACT_RULES,
                          TransitiveTypeExtractor.TRANSITIVETYPE,
                          SPOTLXRuleExtractor.RULERESULTS,
                          FactExtractor.YAGOFACTS,
                          LiteralFactExtractor.YAGOLITERALFACTS);
  }
  
  /** Themes of spotlx deductions */
  public static final Theme RULERESULTS = new Theme("spotlxDeducedFacts", "SPOTLX deduced facts");
  public static final Theme RULESOURCES = new Theme("spotlxDeducedSources", "SPOTLX deduced facts");
  
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
//    FactSource spotlxRelationRules = input.get(PatternHardExtractor.SPOTLX_ENTITY_RULES);
    FactSource spotlxFactRules = input.get(PatternHardExtractor.SPOTLX_FACT_RULES);
    

    FactCollection collection = new FactCollection(spotlxFactRules);
//    collection.load(spotlxFactRules);
    return collection;
  }
  
  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
//    new SPOTLXDeductiveExtractor(/*maxRuleSetSize*/1).extract(new File("/home/jbiega/data/yago2s"), "test");
    new SPOTLXDeductiveExtractor(/*maxRuleSetSize*/0).extract(new File("/home/jbiega/data/yago2s"), "test");
//    new SPOTLXDeductiveExtractor().extract(new File("/local/jbiega/yagofacts"), "test");
  }
  
}
