package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * WikipediaTypeExtractor - YAGO2s
 * 
 * Extracts types from infoboxes
 * 
 * @author Fabian
 * 
 */
public class InfoboxTypeExtractor extends Extractor {

  protected String language;

  /** Sources for category facts*/
  public static final HashMap<String, Theme> INFOBOXTYPESOURCES_MAP = new HashMap<String, Theme>();

  /** Types deduced from categories */
  public static final HashMap<String, Theme> INFOBOXRAWTYPES_MAP = new HashMap<String, Theme>();

  /** Classes deduced from categories */
  public static final HashMap<String, Theme> INFOBOXCLASSES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      INFOBOXTYPESOURCES_MAP.put(s, new Theme("infoboxTypeSources" + Extractor.langPostfixes.get(s), "The sources of category type facts"));
      INFOBOXRAWTYPES_MAP.put(s, new Theme("infoboxRawTypes" + Extractor.langPostfixes.get(s), "All rdf:type facts of YAGO", ThemeGroup.TAXONOMY));
      INFOBOXCLASSES_MAP.put(s, new Theme("infoboxClasses" + Extractor.langPostfixes.get(s),
          "Classes derived from the Wikipedia categories, with their connection to the WordNet class hierarchy leaves"));
    }

  }

  public Set<Theme> input() {
    Set<Theme> temp = new TreeSet<Theme>(Arrays.asList(InfoboxExtractor.INFOBOXTYPESBOTHTRANSLATED_MAP.get(language),
        PatternHardExtractor.TITLEPATTERNS, HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETWORDS, WordnetExtractor.WORDNETCLASSES,
        PatternHardExtractor.INFOBOXPATTERNS));

    return temp;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(INFOBOXTYPESOURCES_MAP.get(language), INFOBOXRAWTYPES_MAP.get(language), INFOBOXCLASSES_MAP.get(language));
  }

  protected ExtendedFactCollection loadFacts(FactSource factSource, ExtendedFactCollection result) {
    for (Fact f : factSource) {
      result.add(f);
    }
    return (result);
  }

  protected ExtendedFactCollection loadFacts(FactSource factSource) {
    return loadFacts(factSource, new ExtendedFactCollection());
  }

  /** Holds the nonconceptual infoboxes*/
  protected Set<String> nonConceptualInfoboxes;

  /** Holds the preferred meanings*/
  protected Map<String, String> preferredMeanings;

  /** Holds the facts about infobox types that we accumulate*/
  protected FactCollection infoboxTypesFacts;

  /** Holds all the classes from Wordnet*/
  protected FactCollection wordnetClasses;

  /** Maps a infobox to a wordnet class */

  /** Maps a category to a wordnet class */
  public String infobox2class(String infoboxName) {
    NounGroup category = new NounGroup(infoboxName);
    if (category.head() == null) {
      Announce.debug("Could not find type in", infoboxName, "(has empty head)");
      return (null);
    }

    // If the category is an acronym, drop it 
    if (Name.isAbbreviation(category.head())) {
      Announce.debug("Could not find type in", infoboxName, "(is abbreviation)");
      return (null);
    }
    category = new NounGroup(infoboxName.toLowerCase());

    String stemmedHead = PlingStemmer.stem(category.head());

    // Exclude the bad guys
    if (nonConceptualInfoboxes.contains(stemmedHead)) {
      Announce.debug("Could not find type in", infoboxName, "(is non-conceptual)");
      return (null);
    }

    // Try all premodifiers (reducing the length in each step) + head
    if (category.preModifier() != null) {
      String wordnet = null;
      String preModifier = category.preModifier().replace('_', ' ');

      for (int start = 0; start != -1 && start < preModifier.length() - 2; start = preModifier.indexOf(' ', start + 1)) {
        wordnet = preferredMeanings.get((start == 0 ? preModifier : preModifier.substring(start + 1)) + " " + stemmedHead);
        // take the longest matching sequence
        if (wordnet != null) return (wordnet);
      }
    }

    // Try postmodifiers to catch "head of state"
    if (category.postModifier() != null && category.preposition() != null && category.preposition().equals("of")) {
      String wordnet = preferredMeanings.get(stemmedHead + " of " + category.postModifier().head());
      if (wordnet != null) return (wordnet);
    }

    // Try head
    String wordnet = preferredMeanings.get(stemmedHead);
    if (wordnet != null) return (wordnet);
    Announce.debug("Could not find type in", infoboxName, "(" + stemmedHead + ") (no wordnet match)");
    return (null);
  }

  /**
   * Extracts type from the category name
   * 
   * @param classWriter
   */
  protected void extractType(String titleEntity, String infobox, Set<String> types) throws IOException {
    String concept = infobox2class(infobox);
    if (concept == null) return;
    types.add(FactComponent.forInfoboxType(infobox));
    infoboxTypesFacts.add(new Fact(null, FactComponent.forInfoboxType(infobox), RDFS.subclassOf, concept), FactComponent.wikipediaURL(titleEntity),
        "WikipediaTypeExtractor from category");
    String name = new NounGroup(infobox).stemmed().replace('_', ' ');
    if (!name.isEmpty()) infoboxTypesFacts.add(
        new Fact(null, FactComponent.forInfoboxType(infobox), RDFS.label, FactComponent.forStringWithLanguage(name, "eng")),
        FactComponent.wikipediaURL(titleEntity), "WikipediaTypeExtractor from stemmed name");
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {

    ExtendedFactCollection infoboxTypes = loadFacts(input.get(InfoboxExtractor.INFOBOXTYPESBOTHTRANSLATED_MAP.get(language)));

    nonConceptualInfoboxes = new HashSet<>();
    for (Fact f : new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS)).getBySecondArgSlow(RDFS.type, "<_yagoNonConceptualInfobox>")) {
      nonConceptualInfoboxes.add(f.getArgJavaString(1));
    }
    preferredMeanings = WordnetExtractor.preferredMeanings(input);
    wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES), true);
    wordnetClasses.load(input.get(HardExtractor.HARDWIREDFACTS));
    infoboxTypesFacts = new FactCollection();

    Set<String> typesOfCurrentEntity = new HashSet<>();
    List<Fact> factsWithSubject = new ArrayList<Fact>();

    Set<String> titles = infoboxTypes.getSubjects();

    String currentEntity = null;

    for (String title : titles) {
      flush(currentEntity, typesOfCurrentEntity, writers);
      currentEntity = title;
      if (currentEntity == null) continue;

      factsWithSubject = infoboxTypes.getFactsWithSubject(currentEntity);
      for (Fact f : factsWithSubject) {
        String infobox = f.getArgJavaString(2);
        extractType(f.getArg(1), infobox, typesOfCurrentEntity);
      }

      factsWithSubject = infoboxTypes.getFactsWithSubject(currentEntity);
      if (factsWithSubject.size() < 1) continue;
      String cls = FactComponent.stripQuotes(factsWithSubject.get(0).getArg(2).replace("_", " "));
      if (Character.isDigit(Char.last(cls))) cls = Char.cutLast(cls);

      if (!nonConceptualInfoboxes.contains(cls)) {
        String type = preferredMeanings.get(cls);
        if (type != null) {
          typesOfCurrentEntity.add(type);
        }
      }

    }
    flush(currentEntity, typesOfCurrentEntity, writers);

    //Announce.progressDone();

    Announce.doing("Writing classes");
    for (Fact f : infoboxTypesFacts) {
      if (FactComponent.isFactId(f.getArg(1))) writers.get(INFOBOXTYPESOURCES_MAP.get(language)).write(f);
      else writers.get(INFOBOXCLASSES_MAP.get(language)).write(f);
    }

    this.infoboxTypesFacts = null;
    this.nonConceptualInfoboxes = null;
    this.preferredMeanings = null;
    this.wordnetClasses = null;
  }

  /** Writes the facts */
  public void flush(String entity, Set<String> types, Map<Theme, FactWriter> writers) throws IOException {
    if (entity == null || types.isEmpty()) {
      types.clear();
      return;
    }
    for (String type : types) {
      write(writers, INFOBOXRAWTYPES_MAP.get(language), new Fact(entity, RDFS.type, type), INFOBOXTYPESOURCES_MAP.get(language),
          FactComponent.wikipediaURL(entity), "WikipediaTypeExtractor from category");
    }
    types.clear();
  }

  public InfoboxTypeExtractor(String lang) {
    language = lang;
  }

  public static void main(String[] args) throws Exception {
    InfoboxTypeExtractor extractor = new InfoboxTypeExtractor("en");
    extractor.extract(new File("D:/data3/yago2s/"), "Test");
  }

}
