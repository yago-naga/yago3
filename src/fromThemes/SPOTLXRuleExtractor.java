package fromThemes;

import java.io.File;
import java.util.Map;
import java.util.Set;

import basics.FactSource;
import basics.FactCollection;
import basics.Theme;

import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;


/**
 * YAGO2s - SPOTLXRuleExtractor
 * 
 * Generates the results of rules needed in the SPOTLX representation.
 * 
 * @author Joanna Biega
 */
public class SPOTLXRuleExtractor extends BaseRuleExtractor {
  
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.HARDWIREDFACTS,
                          PatternHardExtractor.SPOTLX_ENTITY_RULES,
                          PatternHardExtractor.SPOTLX_FACT_RULES,
                          SPOTLXRelationRuleExtractor.RULERESULTS,
                          FactExtractor.YAGOFACTS,
                          MetaFactExtractor.YAGOMETAFACTS);
  }
  
  /** Themes of spotlx deductions */
  public static final Theme RULERESULTS = new Theme("spotlxFacts", "SPOTLX deduced facts");
  public static final Theme RULESOURCES = new Theme("spotlxSources", "SPOTLX deduced facts");
  
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
    FactSource spotlxFactRules = input.get(PatternHardExtractor.SPOTLX_FACT_RULES);
    
    FactCollection collection = new FactCollection(spotlxRelationRules);
    collection.load(spotlxFactRules);
    return collection;
  }

  public static void main(String[] args) throws Exception {
    new PatternHardExtractor(new File("./data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    new HardExtractor(new File("../basics2s/data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    Announce.setLevel(Announce.Level.DEBUG);
    new SPOTLXRelationRuleExtractor().extract(new File("/home/jbiega/data/yago2s"), "test");
    new SPOTLXRuleExtractor().extract(new File("/home/jbiega/data/yago2s"), "test");
  }
}
