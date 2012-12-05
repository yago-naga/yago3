package fromThemes;

import java.io.File;
import java.util.Map;
import java.util.Set;

import basics.FactSource;
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
    return new FinalSet<>(PatternHardExtractor.SPOTLX_RULES,
                          PatternHardExtractor.HARDWIREDFACTS,
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
  protected FactSource getInputRuleSources(Map<Theme, FactSource> input) throws Exception {
    return input.get(PatternHardExtractor.SPOTLX_RULES);
  }

  public static void main(String[] args) throws Exception {
    new PatternHardExtractor(new File("./data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    new HardExtractor(new File("../basics2s/data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    Announce.setLevel(Announce.Level.DEBUG);
    new SPOTLXRuleExtractor().extract(new File("/home/jbiega/data/yago2s"), "test");
  }
}
