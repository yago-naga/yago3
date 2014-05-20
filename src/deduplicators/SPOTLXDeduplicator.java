package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import fromThemes.SPOTLXDeductiveExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.Theme;

/**
 * YAGO2s - SPOTLXDeduplicator
 * 
 * An clean-up extractor for the SPOTLX deduction process.
 * It produces the final results of SPOTLX deduction,
 * filtering only occurs[In, Since, Until] relations and removing the duplicates.
 * 
 * @author Joanna Biega
 */
public class SPOTLXDeduplicator extends SimpleDeduplicator {
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(SPOTLXDeductiveExtractor.RULERESULTS, SchemaExtractor.YAGOSCHEMA);
  }
  
  public static final Theme SPOTLXFACTS = new Theme("spotlxFacts", "SPOTLX deduced facts");
  
  public static final List<String> SPOTLX_FINAL_RELATIONS = new ArrayList<String> (Arrays.asList(
        "<occursIn>",
        "<occursSince>",
        "<occursUntil>"
      ));
  
  @Override
  public Theme myOutput() {
    return SPOTLXFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    return SPOTLX_FINAL_RELATIONS.contains(fact.getRelation());
  }
  
  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new SPOTLXDeduplicator().extract(new File("/home/jbiega/data/yago2s"), "test");
//    new SPOTLXDeduplicator().extract(new File("/local/jbiega/yagofacts"), "test");
  }
}
