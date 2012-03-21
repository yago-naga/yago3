package finalExtractors;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalMap;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import extractors.CategoryExtractor;
import extractors.DisambiguationPageExtractor;
import extractors.Extractor;
import extractors.HardExtractor;
import extractors.InfoboxExtractor;
import extractors.PersonNameExtractor;
import extractors.RuleExtractor;
import extractors.WordnetExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryExtractor.CATEGORYFACTS, HardExtractor.HARDWIREDFACTS, RuleExtractor.RULERESULTS, InfoboxExtractor.INFOBOXFACTS,
        DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS, RuleExtractor.RULERESULTS, PersonNameExtractor.PERSONNAMES,
        WordnetExtractor.WORDNETWORDS, WordnetExtractor.WORDNETGLOSSES, WordnetExtractor.WORDNETIDS, RuleExtractor.RULESOURCES,
        InfoboxExtractor.INFOBOXSOURCES);
  }

  /** All facts of YAGO */
  public static final Theme YAGOFACTS = new Theme("yagoFacts", "All instance facts of YAGO");

  /** All facts of YAGO */
  public static final Theme YAGOSCHEMA = new Theme("yagoSchema", "The schema of YAGO relations");

  /** All facts of YAGO */
  public static final Theme YAGOLABELS = new Theme("yagoLabels", "All labels of YAGO instances");

  /** All meta facts of YAGO */
  public static final Theme YAGOMETAFACTS = new Theme("yagoMetaFacts", "All meta facts of YAGO");

  /** All source facts of YAGO */
  public static final Theme YAGOSOURCES = new Theme("yagoSources", "All sources of YAGO facts");

  /** which relations go to which theme */
  public static final Map<Theme, Set<String>> theme2relations = new FinalMap<>(YAGOSCHEMA,
      new FinalSet<>(RDFS.domain, RDFS.range, RDFS.subpropertyOf), YAGOLABELS, new FinalSet<>(RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>",
          "<hasGivenName>", "<hasFamilyName>", "<hasGloss>"), YAGOSOURCES, new FinalSet<>(YAGO.extractionSource, YAGO.extractionTechnique));

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(RDFS.type, RDFS.subclassOf);

  @Override
  public Set<Theme> output() {
    Set<Theme> themes = new HashSet<>(theme2relations.keySet());
    themes.add(YAGOFACTS);
    themes.add(YAGOMETAFACTS);
    return themes;
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    // Fix where we write the relations
    Map<String, FactWriter> relation2Writer = new TreeMap<>();
    for (Theme t : theme2relations.keySet()) {
      for (String r : theme2relations.get(t)) {
        relation2Writer.put(r, output.get(t));
      }
    }
    Set<String> relationsToDo = new TreeSet<>();
    // Start with some standard relation
    relationsToDo.add(RDFS.label);
    // Pretend we already did the relations that we want to exclude
    Set<String> relationsDone = new TreeSet<>(relationsExcluded);
    while (!relationsToDo.isEmpty()) {
      String relation = D.pick(relationsToDo);
      relationsToDo.remove(relation);
      relationsDone.add(relation);
      Announce.doing("Reading", relation);
      FactCollection facts = new FactCollection();
      boolean isMetaRelation = false;
      for (Theme theme : input.keySet()) {
        Announce.doing("Reading", theme);
        for (Fact fact : input.get(theme)) {
          isMetaRelation = FactComponent.isFactId(fact.getArg(1));
          if (!relationsDone.contains(fact.getRelation()) && !fact.getRelation().startsWith("<_")) {
            relationsToDo.add(fact.getRelation());
          }
          if (!relation.equals(fact.getRelation())) continue;
          facts.add(fact);
        }
        Announce.done();
      }
      Announce.done();
      Announce.doing("Writing", relation);
      FactWriter w = relation2Writer.get(relation);
      // By default, meta relations go to METAFACTS, and the others to FACTS
      if (w == null) {
        if (isMetaRelation) w = output.get(YAGOMETAFACTS);
        else w = output.get(YAGOFACTS);
      }
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
