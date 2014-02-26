package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import utils.FactTemplateExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TypeChecker;
import fromThemes.Redirector;

/**
 * CategoryMapper - YAGO2s
 * 
 * Maps the facts obtained from CategoryExtractor (Previously translated for other languages).
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */
public class CategoryMapper extends Extractor {

  public static final HashMap<String, Theme> CATEGORYFACTS_TOREDIRECT_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CATEGORYFACTS_TOTYPECHECK_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CATEGORYFACTS_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> CATEGORYSOURCES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      CATEGORYFACTS_TOREDIRECT_MAP.put(s, new Theme("categoryFactsToBeRedirected" + Extractor.langPostfixes.get(s),
          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected", ThemeGroup.OTHER));
      CATEGORYFACTS_TOTYPECHECK_MAP.put(s, new Theme("categoryFactsToBeTypechecked" + Extractor.langPostfixes.get(s),
          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked", ThemeGroup.OTHER));
      CATEGORYFACTS_MAP.put(s, new Theme("categoryFacts" + Extractor.langPostfixes.get(s),
          "Facts about Wikipedia instances, derived from the Wikipedia categories", ThemeGroup.OTHER));
      CATEGORYSOURCES_MAP.put(s, new Theme("categorySources" + Extractor.langPostfixes.get(s), "The sources of category facts", ThemeGroup.OTHER));
    }

  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS,
        CategoryExtractor.CATEGORYMEMBERSBOTHTRANSLATED_MAP.get(language), CategoryExtractor.CATEGORYMEMBERS_MAP.get(language)));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYFACTS_TOREDIRECT_MAP.get(language), CATEGORYSOURCES_MAP.get(language));
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(
        new Redirector(CATEGORYFACTS_TOREDIRECT_MAP.get(language), CATEGORYFACTS_TOTYPECHECK_MAP.get(language), this, this.language),
        new TypeChecker(CATEGORYFACTS_TOTYPECHECK_MAP.get(language), CATEGORYFACTS_MAP.get(language), this)));
  }


  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),
        "<_categoryPattern>");

    //Announce.progressStart("Extracting", 3_900_000);

    FactCollection factCollection;
    if (language == "en") factCollection = new FactCollection(input.get(CategoryExtractor.CATEGORYMEMBERS_MAP.get(language)));
    else factCollection = new FactCollection(input.get(CategoryExtractor.CATEGORYMEMBERSBOTHTRANSLATED_MAP.get(language)));

    for (Fact f : factCollection) {
      String temp = f.getArg(2);
      if (f.getArg(2).contains("_")) {
        temp = f.getArg(2).replace("_", " ");
      }
      for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(temp), f.getArg(1))) {
        if (fact != null) {
          write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language),
              FactComponent.wikipediaURL(f.getArg(1)), "CategoryMapper");
        }
      }
    }

  }

  /** Constructor from source file */
  public CategoryMapper(String lang) {
    language = lang;
  }

  public static void main(String[] args) throws Exception {
    new CategoryMapper("en").extract(new File("D:/data3/yago2s/"), "mapping categories into facts");
  }

}
