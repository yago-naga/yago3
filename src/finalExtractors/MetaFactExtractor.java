package finalExtractors;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.YAGO;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.RuleExtractor;
import extractors.TemporalCategoryExtractor;
import extractors.TemporalInfoboxExtractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class MetaFactExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS, 
                RuleExtractor.RULERESULTS,                 
                TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }
  /** All meta facts of YAGO */
  public static final Theme YAGOMETAFACTS = new Theme("yagoMetaFacts", "All meta facts of YAGO");

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(YAGO.extractionSource, YAGO.extractionTechnique);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOMETAFACTS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactWriter w=output.get(YAGOMETAFACTS);

    // We don't need any more taxonomy beyond this point
    // But: other extractors that run in parallel might still need it
    //TransitiveTypeExtractor.freeMemory();
    //WordnetExtractor.freeMemory();
    
    // Collect themes where we find the relations
    Map<String, Set<Theme>> relationsToDo = new HashMap<>();
    // Start with some standard relation
    relationsToDo.put("<happenedOnDate>", new HashSet<Theme>(input.keySet()));
    boolean isFirstRun = true;
    while (!relationsToDo.isEmpty()) {
      String relation = D.pick(relationsToDo.keySet());
      Announce.doing("Reading", relation);
      FactCollection facts = new FactCollection();
      for (Theme theme : relationsToDo.get(relation)) {
        Announce.doing("Reading", theme);
        for (Fact fact : input.get(theme)) {
          if (isFirstRun && !fact.getRelation().startsWith("<_") && !relationsExcluded.contains(fact.getRelation()) && FactComponent.isFactId(fact.getArg(1))) {
            D.addKeyValue(relationsToDo, fact.getRelation(), theme, HashSet.class);
          }
          if (!relation.equals(fact.getRelation())) continue;
          facts.add(fact);
        }
        Announce.done();
        relationsToDo.remove(relation);
      }
      isFirstRun = false;
      Announce.done();
      Announce.doing("Writing", relation);
      for (Fact fact : facts)
        w.write(fact);
      Announce.done();
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new MetaFactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }
}
