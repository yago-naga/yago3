package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
  public static final HashMap<String, Theme> CATEGORYFACTS_TOREDIRECT_MAP = new HashMap<String, Theme>(); 
  public static final HashMap<String, Theme> CATEGORYSOURCES_MAP = new HashMap<String, Theme>(); //new Theme("categorySources", "The sources of category facts");
  
  static {
    for (String s : Extractor.languages) {
      CATEGORYFACTS_TOREDIRECT_MAP.put(s, new Theme("categoryFactsToBeRedirected_" + s, 
          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected", ThemeGroup.OTHER));
      CATEGORYSOURCES_MAP.put(s, new Theme("categorySources_" + s, "The sources of category facts", ThemeGroup.OTHER));
    }

  }
  
 
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
  }  
  
  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYFACTS_TOREDIRECT_MAP.get(language), CATEGORYSOURCES_MAP.get(language));
  }
 
  
//  public static final Theme CATEGORYFACTS = new Theme("categoryFacts", "Facts about Wikipedia instances, derived from the Wikipedia categories");
//  /** Facts deduced from categories */
//  public static final Theme CATEGORYFACTS_TOTYPECHECK = new Theme("categoryFactsToBeTypeChecked",
//      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");


//  @Override
//  public Set<Extractor> followUp() {
//    return new HashSet<Extractor>(Arrays.asList(new Redirector(CATEGORYFACTS_TOREDIRECT_MAP.get(language), CATEGORYFACTS_TOTYPECHECK, this), new TypeChecker(
//        CATEGORYFACTS_TOTYPECHECK, CATEGORYFACTS, this)));
//  }

 
  /** Constructor from source file */
  public CategoryMapper(String lang) {
    language=lang;
  }
  

}
