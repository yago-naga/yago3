package fromThemes;

import java.io.File;
import java.util.Map;
import java.util.Set;

import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.InfoboxExtractor;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.FactCollection;
import basics.FactSource;
import basics.Theme;

/**
 * YAGO2s - RuleExtractor
 * 
 * Generates the results of rules.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class RuleExtractor extends BaseRuleExtractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.RULES, TransitiveTypeExtractor.TRANSITIVETYPE, ClassExtractor.YAGOTAXONOMY,
        HardExtractor.HARDWIREDFACTS, InfoboxExtractor.INFOBOXFACTS, CategoryExtractor.CATEGORYFACTS,WordnetExtractor.WORDNETCLASSES);
  }

  /** Theme of deductions */
  public static final Theme RULERESULTS = new Theme("ruleResults", "Results of rule applications");

  /** Theme of sources deductions */
  public static final Theme RULESOURCES = new Theme("ruleSources", "Source information for results of rule applications");

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
  
  /** Extract rule collection from fact sources */
  @Override
  public FactCollection getInputRuleCollection(Map<Theme, FactSource> input) throws Exception {
    FactSource rules = input.get(PatternHardExtractor.RULES);
    FactCollection collection = new FactCollection(rules);
    return collection;
  }

  public static void main(String[] args) throws Exception {
    new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "test");
    Announce.setLevel(Announce.Level.DEBUG);
    new RuleExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
