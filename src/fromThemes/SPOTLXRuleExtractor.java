package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.FactCollection;
import basics.FactSource;
import basics.Theme;

import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.Extractor;
import fromGeonames.GeoNamesDataImporter;

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
  public Set<Extractor> followUp() {
    return(new HashSet<Extractor> (Arrays.asList(new SPOTLXDeductiveExtractor())));
  }
  
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
  
  public static void main(String[] args) throws Exception {
    new PatternHardExtractor(new File("./data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    new HardExtractor(new File("../basics2s/data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    Announce.setLevel(Announce.Level.DEBUG);
    new SPOTLXRuleExtractor().extract(new File("/home/jbiega/data/yago2s"), "test");
  }
  
}