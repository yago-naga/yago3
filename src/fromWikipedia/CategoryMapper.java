package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.FactTemplateExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TypeChecker;

/**
 * CategoryExtractor - YAGO2s
 * 
 * Extracts facts from categories
 * 
 * @author Farzaneh
 * 
 */
public abstract class CategoryMapper extends Extractor {



  protected String language; 
  public static final HashMap<String, Theme> CATEGORYSOURCES_MAP = new HashMap<String, Theme>(); //new Theme("categorySources", "The sources of category facts");
  public static final HashMap<String, Theme> CATEGORYFACTS_TOREDIRECT_MAP = new HashMap<String, Theme>(); 
  
  
  static {
    for (String s : Extractor.languages) {
      CATEGORYFACTS_TOREDIRECT_MAP.put(s, new Theme("categoryFactsToBeRedirected_" + s, 
          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected", ThemeGroup.OTHER));
      CATEGORYSOURCES_MAP.put(s, new Theme("categorySources_" + s, "The sources of category facts", ThemeGroup.OTHER));
    }

  }

  
  
  public static final Theme CATEGORYFACTS = new Theme("categoryFacts", "Facts about Wikipedia instances, derived from the Wikipedia categories");
  /** Facts deduced from categories */
  public static final Theme CATEGORYFACTS_TOTYPECHECK = new Theme("categoryFactsToBeTypeChecked",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(CategoryExtractor.CATEGORYATTS_MAP.get(language), PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(new Redirector(CATEGORYFACTS_TOREDIRECT_MAP.get(language), CATEGORYFACTS_TOTYPECHECK, this), new TypeChecker(
        CATEGORYFACTS_TOTYPECHECK, CATEGORYFACTS, this)));
  }



  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYSOURCES_MAP.get(language), CATEGORYFACTS_TOREDIRECT_MAP.get(language));
  }



  /** Constructor from source file */
  public CategoryMapper(String lang) {
    language=lang;
  }
  

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    String yago = "D:/data2/yago2s/";
//    new HardExtractor(new File("D:/data/")).extract(new File("D:/data2/yago2s/"), "test");
//    new PatternHardExtractor(new File("D:/data")).extract(new File("D:/data2/yago2s/"), "test");
//    new CategoryMapper().extract(new File(yago), "Test on 1 wikipedia article");
  }
}
