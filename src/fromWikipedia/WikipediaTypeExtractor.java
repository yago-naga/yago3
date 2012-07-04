package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import utils.TitleExtractor;


import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import javatools.util.FileUtils;
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

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(WIKIPEDIATYPESOURCES, YAGOTYPES, WIKIPEDIACLASSES);
  }

  /** Maps a category to a wordnet class */
  public static String category2class(String categoryName, Set<String> nonconceptual, Map<String, String> preferredMeaning) {
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
    if (nonconceptual.contains(stemmedHead)) {
      Announce.debug("Could not find type in", categoryName, "(is non-conceptual)");
      return (null);
    }

    // Try all premodifiers (reducing the length in each step) + head
    if (category.preModifier() != null) {
      String wordnet = null;
      String preModifier = category.preModifier().replace('_', ' ');

      for (int start = 0; start != -1 && start < preModifier.length() - 2; start = preModifier.indexOf(' ', start + 1)) {
        wordnet = preferredMeaning.get((start == 0 ? preModifier : preModifier.substring(start + 1)) + " " + stemmedHead);
        // take the longest matching sequence
        if (wordnet != null) return (wordnet);
      }
    }

    // Try postmodifiers to catch "head of state"
    if (category.postModifier() != null && category.preposition() != null && category.preposition().equals("of")) {
      String wordnet = preferredMeaning.get(stemmedHead + " of " + category.postModifier().head());
      if (wordnet != null) return (wordnet);
    }

    // Try head
    String wordnet = preferredMeaning.get(stemmedHead);
    if (wordnet != null) return (wordnet);
    Announce.debug("Could not find type in", categoryName, "("+stemmedHead+") (no wordnet match)");
    return (null);
  }

  /**
   * Extracts type from the category name
   * 
   * @param classWriter
   */
  protected void extractType(String titleEntity, String category, Set<String> types, FactCollection categoryFacts, Set<String> nonconceptual,
      Map<String, String> preferredMeaning) throws IOException {
    String concept = category2class(category, nonconceptual, preferredMeaning);
    if (concept == null) return;
    types.add(FactComponent.forWikiCategory(category));
    categoryFacts.add(new Fact(null, FactComponent.forWikiCategory(category), RDFS.subclassOf, concept), FactComponent.wikipediaURL(titleEntity),
        "WikipediaTypeExtractor from category");
    String name = new NounGroup(category).stemmed().replace('_', ' ');
    if (!name.isEmpty()) categoryFacts.add(
        new Fact(null, FactComponent.forWikiCategory(category), RDFS.label, FactComponent.forStringWithLanguage(name, "en")),
        FactComponent.wikipediaURL(titleEntity), "WikipediaTypeExtractor from stemmed name");
  }

  /** Returns the set of non-conceptual words */
  public static Set<String> nonConceptualWords(FactCollection categoryPatterns) {
    return (categoryPatterns.asStringSet("<_yagoNonConceptualWord>"));
  }

  /** Returns the set of non-conceptual words 
   * @throws IOException */
  public static Set<String> nonConceptualWords(FactSource categoryPatterns) throws IOException {
    return (nonConceptualWords(new FactCollection(categoryPatterns)));
  }

  /** Returns the set of non-conceptual words 
   * @throws IOException */
  public static Set<String> nonConceptualWords(Map<Theme, FactSource> themes) throws IOException {
    return (nonConceptualWords(themes.get(PatternHardExtractor.CATEGORYPATTERNS)));
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    Set<String> nonConceptualInfoboxes = new HashSet<>();
    for (Fact f : new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS)).getBySecondArgSlow(RDFS.type, "<_yagoNonConceptualInfobox>")) {
      nonConceptualInfoboxes.add(f.getArg(1));
    }
    Set<String> nonConceptualCategories = nonConceptualWords(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)));
    Map<String, String> preferredMeanings = WordnetExtractor.preferredMeanings(input);
    TitleExtractor titleExtractor = new TitleExtractor(input);
    FactCollection wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES));
    FactCollection categoryClasses = new FactCollection();

    // Extract the information
    Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    Set<String> types = new HashSet<>();
    loop: while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:", "{{Infobox", "{{ Infobox")) {
        case -1: // End of file
          flush(titleEntity, types, writers, categoryClasses, wordnetClasses);
          break loop;
        case 0: // New entity
          Announce.progressStep();
          flush(titleEntity, types, writers, categoryClasses, wordnetClasses);          
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1: // Category
          if (titleEntity == null) continue;
          String category = FileLines.readTo(in, ']', '|').toString();
          category = category.trim();
          extractType(titleEntity, category, types, categoryClasses, nonConceptualCategories, preferredMeanings);
          break;
        case 2: // Infobox
        case 3:// Infobox
          String cls = FileLines.readTo(in, '}', '|').toString().trim().toLowerCase();
          if (Character.isDigit(Char.last(cls))) cls = Char.cutLast(cls);
          if (!nonConceptualInfoboxes.contains(cls)) {
            String type = preferredMeanings.get(cls);
            if (type != null) types.add(type);
          }
      }
    }
    Announce.progressDone();

    Announce.doing("Writing classes");
    for (Fact f : categoryClasses) {
      if (FactComponent.isFactId(f.getArg(1))) writers.get(WIKIPEDIATYPESOURCES).write(f);
      else writers.get(WIKIPEDIACLASSES).write(f);
    }
    Announce.done();

    Announce.doing("Writing hard wired types");
    for (Fact f : input.get(HardExtractor.HARDWIREDFACTS)) {
      if (f.getRelation().equals(RDFS.type)) write(writers, YAGOTYPES, f, WIKIPEDIATYPESOURCES, YAGO.yago, "Manual");
    }
    Announce.done();
    
    in.close();
  }

  /** Writes the facts */
  public void flush(String entity, Set<String> types, Map<Theme, FactWriter> writers, FactCollection categoryClasses, FactCollection wordnetClasses)
      throws IOException {
    if (entity == null || types.isEmpty()) {
      types.clear();
      return;
    }
    String yagoBranch = yagoBranch(entity, types, categoryClasses, wordnetClasses);
    Announce.debug("Branch of", entity, "is", yagoBranch);
    if (yagoBranch == null) {
      types.clear();
      return;
    }
    for (String type : types) {
      String branch = yagoBranch(type, categoryClasses, wordnetClasses);
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
  public static String yagoBranch(String arg, FactCollection categoryClasses, FactCollection wordnetClasses) {
    String yagoBranch = SimpleTypeExtractor.yagoBranch(arg, wordnetClasses);
    if (yagoBranch != null) return (yagoBranch);
    for (String sup : categoryClasses.getArg2s(arg, RDFS.subclassOf)) {
      yagoBranch = SimpleTypeExtractor.yagoBranch(sup, wordnetClasses);
      if (yagoBranch != null) return (yagoBranch);
    }
    return null;
  }

  /** Returns the YAGO branch for a an entity */
  public static String yagoBranch(String entity, Set<String> types, FactCollection categoryClasses, FactCollection wordnetClasses) {
    Map<String, Integer> branches = new TreeMap<>();
    for (String type : types) {
      String yagoBranch = yagoBranch(type, categoryClasses, wordnetClasses);
      if (yagoBranch != null) D.addKeyValue(branches, yagoBranch, 1);
    }
    String yagoBranch = null;
    for (String b : branches.keySet()) {
      if (yagoBranch == null || branches.get(b) > branches.get(yagoBranch)) yagoBranch = b;
    }
    return (yagoBranch);
  }

  /** returns the (trivial) names of an entity */
  public static Set<String> namesOf(String titleEntity) {
    Set<String> result = new TreeSet<>();
    if (titleEntity.startsWith("<")) titleEntity = titleEntity.substring(1);
    if (titleEntity.endsWith(">")) titleEntity = Char.cutLast(titleEntity);
    String name = Char.decode(titleEntity.replace('_', ' '));
    result.add(FactComponent.forStringWithLanguage(name, "en"));
    String norm = Char.normalize(name);
    if (!norm.contains("[?]")) result.add(FactComponent.forStringWithLanguage(norm, "en"));
    if (name.contains(" (")) {
      result.add(FactComponent.forStringWithLanguage(name.substring(0, name.indexOf(" (")).trim(), "en"));
    }
    if (name.contains(",") && !name.contains("(")) {
      result.add(FactComponent.forStringWithLanguage(name.substring(0, name.indexOf(",")).trim(), "en"));
    }
    return (result);
  }

  /** Constructor from source file */
  public WikipediaTypeExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new WikipediaTypeExtractor(new File("./testCases/fromWikipedia.InfoboxExtractor/wikitest.xml")).extract(new File("c:/fabian/data/yago2s"),
        "Test on 1 wikipedia article");
  }
}
