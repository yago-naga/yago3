package finalExtractors;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * YAGO2s - LiteralFactExtractor
 * 
 * Deduplicates all facts with literals and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class LiteralFactExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryExtractor.CATEGORYFACTS, HardExtractor.HARDWIREDFACTS, RuleExtractor.RULERESULTS, InfoboxExtractor.INFOBOXFACTS,
        GenderExtractor.PERSONS_GENDER, /*GeoNamesDataImporter.GEONAMESDATA,*/ TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }

  /** All facts of YAGO */
  public static final Theme YAGOLITERALFACTS = new Theme("yagoLiteralFacts", "All facts of YAGO with literals");

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range, RDFS.subpropertyOf,
      RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>", "<hasGloss>");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOLITERALFACTS);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactWriter w=output.get(YAGOLITERALFACTS);

    // We don't need any more taxonomy beyond this point
    TransitiveTypeExtractor.freeMemory();
    WordnetExtractor.freeMemory();
   
    // Collect themes where we find the relations
    Map<String, Set<Theme>> relationsToDo = new TreeMap<>();
    // Start with some standard relation
    relationsToDo.put("<wasBornOnDate>", new HashSet<Theme>(input.keySet()));
    boolean isFirstRun = true;
    while (!relationsToDo.isEmpty()) {
      String relation = D.pick(relationsToDo.keySet());
      Announce.doing("Reading", relation);
      FactCollection facts = new FactCollection();
      for (Theme theme : relationsToDo.get(relation)) {
        Announce.doing("Reading", theme);
        for (Fact fact : input.get(theme)) {
          if (isFirstRun && !fact.getRelation().startsWith("<_") && !relationsExcluded.contains(fact.getRelation()) && !FactComponent.isFactId(fact.getArg(1)) && FactComponent.isLiteral(fact.getArg(2))) {
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
    new LiteralFactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }
}
