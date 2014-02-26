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
 * Extracts types from category facts.
 * 
 * @author Fabian
 * 
 */
public class CategoryTypeExtractor extends Extractor {

  /** Sources for category facts*/
  public static final HashMap<String, Theme> CATEGORYTYPESOURCES_MAP = new HashMap<String, Theme>();

  /** Types deduced from categories */
  public static final HashMap<String, Theme> CATEGORYTYPES_MAP = new HashMap<String, Theme>();

  /** Classes deduced from categories */
  public static final HashMap<String, Theme> CATEGORYCLASSES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      CATEGORYTYPESOURCES_MAP.put(s, new Theme("cateogryTypeSources" + Extractor.langPostfixes.get(s), "The sources of category type facts"));
      CATEGORYTYPES_MAP.put(s, new Theme("categoryTypes" + Extractor.langPostfixes.get(s), "All rdf:type facts of YAGO", ThemeGroup.TAXONOMY));
      CATEGORYCLASSES_MAP.put(s, new Theme("categoryClasses" + Extractor.langPostfixes.get(s),
          "Classes derived from the Wikipedia categories, with their connection to the WordNet class hierarchy leaves"));
    }

  }

  public Set<Theme> input() {
    Set<Theme> temp = new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, PatternHardExtractor.TITLEPATTERNS,
        HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETWORDS, WordnetExtractor.WORDNETCLASSES, PatternHardExtractor.INFOBOXPATTERNS));

    if (this.language.equals("en")) temp.add(CategoryExtractor.CATEGORYMEMBERS_MAP.get(language));
    else temp.add(CategoryExtractor.CATEGORYMEMBERSBOTHTRANSLATED_MAP.get(language));

    return temp;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYTYPESOURCES_MAP.get(language), CATEGORYTYPES_MAP.get(language), CATEGORYCLASSES_MAP.get(language));
  }

  /** Holds the nonconceptual infoboxes*/
  protected Set<String> nonConceptualInfoboxes;

  /** Holds the nonconceptual categories*/
  protected Set<String> nonConceptualCategories;

  /** Holds the preferred meanings*/
  protected Map<String, String> preferredMeanings;

  /** Holds the facts about categories that we accumulate*/
  protected FactCollection categoryClassFacts;

  /** Holds all the classes from Wordnet*/
  protected FactCollection wordnetClasses;

  /** Maps a infobox to a wordnet class */

  /** Maps a category to a wordnet class */
  public String category2class(String categoryName) {
    // Check out whether the new category is worth being added
    NounGroup category = new NounGroup(categoryName);
    if (category.head() == null) {
      Announce.debug("Could not find type in", categoryName, "(has empty head)");
      return (null);
    }

    // If the category is an acronym, drop it 
    if (Name.isAbbreviation(category.head())) {
      Announce.debug("Could not find type in", categoryName, "(is abbreviation)");
      return (null);
    }
    category = new NounGroup(categoryName.toLowerCase());

    // Only plural words are good hypernyms
    if (PlingStemmer.isSingular(category.head()) && !category.head().equals("people")) {
      Announce.debug("Could not find type in", categoryName, "(is singular)");
      return (null);
    }
    String stemmedHead = PlingStemmer.stem(category.head());

    // Exclude the bad guys
    if (nonConceptualCategories.contains(stemmedHead)) {
      Announce.debug("Could not find type in", categoryName, "(is non-conceptual)");
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
    Announce.debug("Could not find type in", categoryName, "(" + stemmedHead + ") (no wordnet match)");
    return (null);
  }

  /**
   * Extracts type from the category name
   * 
   * @param classWriter
   */
  protected void extractType(String titleEntity, String category, Set<String> types) throws IOException {
    String concept = category2class(category);
    if (concept == null) return;
    types.add(FactComponent.forWikiCategory(category));
    categoryClassFacts.add(new Fact(null, FactComponent.forWikiCategory(category), RDFS.subclassOf, concept),
        FactComponent.wikipediaURL(titleEntity), "WikipediaTypeExtractor from category");
    String name = new NounGroup(category).stemmed().replace('_', ' ');
    if (!name.isEmpty()) categoryClassFacts.add(
        new Fact(null, FactComponent.forWikiCategory(category), RDFS.label, FactComponent.forStringWithLanguage(name, "eng")),
        FactComponent.wikipediaURL(titleEntity), "WikipediaTypeExtractor from stemmed name");
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    ExtendedFactCollection categoryMembs;
    if (this.language.equals("en")) categoryMembs = ExtendedFactCollection.loadFacts(input.get(CategoryExtractor.CATEGORYMEMBERS_MAP.get(language)));
    else categoryMembs = ExtendedFactCollection.loadFacts(input.get(CategoryExtractor.CATEGORYMEMBERSBOTHTRANSLATED_MAP.get(language)));

    nonConceptualInfoboxes = new HashSet<>();
    for (Fact f : new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS)).getBySecondArgSlow(RDFS.type, "<_yagoNonConceptualInfobox>")) {
      nonConceptualInfoboxes.add(f.getArgJavaString(1));
    }
    nonConceptualCategories = new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)).asStringSet("<_yagoNonConceptualWord>");
    preferredMeanings = WordnetExtractor.preferredMeanings(input);
    wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES), true);
    wordnetClasses.load(input.get(HardExtractor.HARDWIREDFACTS));
    categoryClassFacts = new FactCollection();

    // Extract the information
    Set<String> typesOfCurrentEntity = new HashSet<>();
    List<Fact> factsWithSubject = new ArrayList<Fact>();

    Set<String> titles = categoryMembs.getSubjects();

    String currentEntity = null;

    for (String title : titles) {
      flush(currentEntity, typesOfCurrentEntity, writers);
      currentEntity = title;
      if (currentEntity == null) continue;

      factsWithSubject = categoryMembs.getFactsWithSubject(currentEntity);
      for (Fact f : factsWithSubject) {
        String category = f.getArgJavaString(2); //Asia
        extractType(f.getArg(1), category, typesOfCurrentEntity);
      }

    }
    flush(currentEntity, typesOfCurrentEntity, writers);

    //Announce.progressDone();

    Announce.doing("Writing classes");
    for (Fact f : categoryClassFacts) {
      if (FactComponent.isFactId(f.getArg(1))) writers.get(CATEGORYTYPESOURCES_MAP.get(language)).write(f);
      else writers.get(CATEGORYCLASSES_MAP.get(language)).write(f);
    }
    Announce.done();

    this.categoryClassFacts = null;
    this.nonConceptualCategories = null;
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
      write(writers, CATEGORYTYPES_MAP.get(language), new Fact(entity, RDFS.type, type), CATEGORYTYPESOURCES_MAP.get(language),
          FactComponent.wikipediaURL(entity), "WikipediaTypeExtractor from category");
    }
    types.clear();
  }

  public CategoryTypeExtractor(String lang) {
    language = lang;
  }

  public static void main(String[] args) throws Exception {
    new CategoryTypeExtractor("de").extract(new File("D:/data3/yago2s/"), "Test");
  }

}
