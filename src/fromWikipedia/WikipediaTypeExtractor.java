package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.SimpleTypeExtractor;

/**
 * WikipediaTypeExtractor - YAGO2s
 * 
 * Extracts types from categories and infoboxes
 * 
 * @author Fabian
 * 
 */
public class WikipediaTypeExtractor extends Extractor {

  /** The file from which we read */
  protected File wikipedia;

  @Override
  public File inputDataFile() {   
    return wikipedia;
  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, PatternHardExtractor.TITLEPATTERNS, HardExtractor.HARDWIREDFACTS,
        WordnetExtractor.WORDNETWORDS, WordnetExtractor.WORDNETCLASSES, PatternHardExtractor.INFOBOXPATTERNS));
  }

  /** Types deduced from categories */
  public static final Theme YAGOTYPES = new Theme("yagoTypes", "All rdf:type facts of YAGO", ThemeGroup.TAXONOMY);

  /** Sources for category facts*/
  public static final Theme WIKIPEDIATYPESOURCES = new Theme("wikipediaTypeSources", "The sources of category type facts");

  /** Classes deduced from categories */
  public static final Theme WIKIPEDIACLASSES = new Theme("wikipediaClasses",
      "Classes derived from the Wikipedia categories, with their connection to the WordNet class hierarchy leaves");

  /** Holds the nonconceptual infoboxes*/
  protected Set<String> nonConceptualInfoboxes;

  /** Holds the nonconceptual categories*/
  protected Set<String> nonConceptualCategories;

  /** Holds the preferred meanings*/
  private Map<String, String> preferredMeanings;

  /** Caches the YAGO branches*/
  private Map<String, String> yagoBranches;

  /** Holds the facts about categories that we accumulate*/
  private FactCollection categoryClassFacts;

  /** Holds all the classes from Wordnet*/
  protected FactCollection wordnetClasses;

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(WIKIPEDIATYPESOURCES, YAGOTYPES, WIKIPEDIACLASSES);
  }

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
    nonConceptualInfoboxes = new HashSet<>();
    for (Fact f : new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS)).getBySecondArgSlow(RDFS.type, "<_yagoNonConceptualInfobox>")) {
      nonConceptualInfoboxes.add(f.getArgJavaString(1));
    }
    nonConceptualCategories = new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)).asStringSet("<_yagoNonConceptualWord>");
    preferredMeanings = WordnetExtractor.preferredMeanings(input);
    wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES),true);
    wordnetClasses.load(input.get(HardExtractor.HARDWIREDFACTS));
    categoryClassFacts = new FactCollection();
    yagoBranches = new HashMap<String, String>();
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String currentEntity = null;
    Set<String> typesOfCurrentEntity = new HashSet<>();
    loop: while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:", "{{Infobox", "{{ Infobox")) {
        case -1: // End of file
          flush(currentEntity, typesOfCurrentEntity, writers);
          break loop;
        case 0: // New entity
          Announce.progressStep();
          flush(currentEntity, typesOfCurrentEntity, writers);
          currentEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1: // Category
          if (currentEntity == null) continue;
          // Some categories are not correctly terminated by ]]
          String category = FileLines.readTo(in, ']', '|','\n','[','<','}','{').toString();
          category = category.trim();
          extractType(currentEntity, category, typesOfCurrentEntity);
          break;
        case 2: // Infobox
        case 3:// Infobox
          String cls = FileLines.readTo(in, '}', '|').toString().trim().toLowerCase();
          if (Character.isDigit(Char.last(cls))) cls = Char.cutLast(cls);
          if (!nonConceptualInfoboxes.contains(cls)) {
            String type = preferredMeanings.get(cls);
            if (type != null) typesOfCurrentEntity.add(type);
          }
      }
    }
    Announce.progressDone();

    Announce.doing("Writing classes");
    for (Fact f : categoryClassFacts) {
      if (FactComponent.isFactId(f.getArg(1))) writers.get(WIKIPEDIATYPESOURCES).write(f);
      else writers.get(WIKIPEDIACLASSES).write(f);
    }
    Announce.done();

    Announce.doing("Writing hard wired types");
    for (Fact f : input.get(HardExtractor.HARDWIREDFACTS)) {
      if (f.getRelation().equals(RDFS.type)) write(writers, YAGOTYPES, f, WIKIPEDIATYPESOURCES, YAGO.yago, "Manual");
    }
    Announce.done();

    this.categoryClassFacts=null;
    this.nonConceptualCategories=null;
    this.nonConceptualInfoboxes=null;
    this.preferredMeanings=null;
    this.wordnetClasses=null;
    this.yagoBranches=null;
    in.close();
  }

  /** Writes the facts */
  public void flush(String entity, Set<String> types, Map<Theme, FactWriter> writers) throws IOException {
    if (entity == null || types.isEmpty()) {
      types.clear();
      return;
    }
    String yagoBranch = yagoBranchForEntity(entity, types);
    Announce.debug("Branch of", entity, "is", yagoBranch);
    if (yagoBranch == null) {
      types.clear();
      return;
    }
    for (String type : types) {
      String branch = yagoBranchForClass(type);
      if (branch == null || !branch.equals(yagoBranch)) {
        Announce.debug("Wrong branch:", type, branch);
      } else {
        write(writers, YAGOTYPES, new Fact(entity, RDFS.type, type), WIKIPEDIATYPESOURCES, FactComponent.wikipediaURL(entity),
            "WikipediaTypeExtractor from category");
      }
    }
    types.clear();
  }

  /** Returns the YAGO branch for a category class */
  public String yagoBranchForClass(String arg) {
    if (yagoBranches.containsKey(arg)) return (yagoBranches.get(arg));
    String yagoBranch = SimpleTypeExtractor.yagoBranch(arg, wordnetClasses);
    if (yagoBranch != null) {
      yagoBranches.put(arg, yagoBranch);
      return (yagoBranch);
    }
    String sup = categoryClassFacts.getArg2(arg, RDFS.subclassOf);
    if (sup != null) {
      yagoBranch = SimpleTypeExtractor.yagoBranch(sup, wordnetClasses);
      if (yagoBranch != null) {
        yagoBranches.put(arg, yagoBranch);
        return (yagoBranch);
      }
    }
    return null;
  }

  /** Returns the YAGO branch for a an entity */
  public String yagoBranchForEntity(String entity, Set<String> types) {
    IntHashMap<String> branches = new IntHashMap<>();
    for (String type : types) {
      String yagoBranch = yagoBranchForClass(type);
      if (yagoBranch != null) {
        Announce.debug(entity, type, yagoBranch);
        // Give higher priority to the stuff extracted from infoboxes
        branches.increase(yagoBranch);
        if(type.startsWith("<wordnet")) branches.increase(yagoBranch);
      }
    }
    String bestSoFar = null;
    for (String candidate : branches.keys()) {
      if (bestSoFar == null || branches.get(candidate) > branches.get(bestSoFar) || branches.get(candidate) == branches.get(bestSoFar)
          && SimpleTypeExtractor.yagoBranches.indexOf(candidate) < SimpleTypeExtractor.yagoBranches.indexOf(bestSoFar)) bestSoFar = candidate;
    }
    return (bestSoFar);
  }

  /** Constructor from source file */
  public WikipediaTypeExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    //new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    //new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new WikipediaTypeExtractor(new File("c:/fabian/data/wikipedia/testset/esan.xml")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
  }
}
