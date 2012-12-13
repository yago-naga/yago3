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
import fromOtherSources.HardExtractor;
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
  
  @Override
  public Set<Extractor> followUp() {
    return(new HashSet<Extractor> (Arrays.asList(new SPOTLXDeduplicator())));
  }
  
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.HARDWIREDFACTS,
                          PatternHardExtractor.SPOTLX_ENTITY_RULES,
                          PatternHardExtractor.SPOTLX_FACT_RULES,
                          SPOTLXRuleExtractor.RULERESULTS,
                          FactExtractor.YAGOFACTS,
                          MetaFactExtractor.YAGOMETAFACTS,
                          LiteralFactExtractor.YAGOLITERALFACTS,
                          GeoNamesDataImporter.GEONAMESDATA);
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
    FactSource spotlxRelationRules = input.get(PatternHardExtractor.SPOTLX_ENTITY_RULES);
    FactSource spotlxFactRules = input.get(PatternHardExtractor.SPOTLX_FACT_RULES);
    
    FactCollection collection = new FactCollection(spotlxRelationRules);
    collection.load(spotlxFactRules);
    return collection;
  }
  
}
