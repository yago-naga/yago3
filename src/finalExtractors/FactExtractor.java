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
import basics.RDFS;
import basics.Theme;
import extractors.CategoryExtractor;
import extractors.Extractor;
import extractors.GenderExtractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.RuleExtractor;
import extractors.TemporalCategoryExtractor;
import extractors.TemporalInfoboxExtractor;
import extractors.WordnetExtractor;
import extractors.geonames.GeoNamesDataImporter;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryExtractor.CATEGORYFACTS,
        GenderExtractor.PERSONS_GENDER,        
        HardExtractor.HARDWIREDFACTS, 
        InfoboxExtractor.INFOBOXFACTS,        
        RuleExtractor.RULERESULTS,      
        GeoNamesDataImporter.GEONAMESDATA,
         TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }

  /** All facts of YAGO */
  public static final Theme YAGOFACTS = new Theme("yagoFacts", "All instance-instance facts of YAGO");

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range, RDFS.subpropertyOf,
      RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>", "<hasGloss>");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOFACTS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactWriter w=output.get(YAGOFACTS);

    // We don't need any more taxonomy beyond this point
    TransitiveTypeExtractor.freeMemory();
    WordnetExtractor.freeMemory();
   
    // Collect themes where we find the relations
    Map<String, Set<Theme>> relationsToDo = new HashMap<>();
    // Start with some standard relation
    relationsToDo.put("<actedIn>", new HashSet<Theme>(input.keySet()));
    boolean isFirstRun = true;
    while (!relationsToDo.isEmpty()) {
      String relation = D.pick(relationsToDo.keySet());
      Announce.doing("Reading", relation);
      FactCollection facts = new FactCollection();
      for (Theme theme : relationsToDo.get(relation)) {
        Announce.doing("Reading", theme);
        for (Fact fact : input.get(theme)) {
          if (isFirstRun && !fact.getRelation().startsWith("<_") && !relationsExcluded.contains(fact.getRelation()) && !FactComponent.isFactId(fact.getArg(1)) && !FactComponent.isLiteral(fact.getArg(2))) {
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
    new FactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }
}
