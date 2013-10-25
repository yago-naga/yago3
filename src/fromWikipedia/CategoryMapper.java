package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
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
public class CategoryMapper extends Extractor {



  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(CategoryExtractor.CATEGORYATTS, PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(new Redirector(CATEGORYFACTS_TOREDIRECT, CATEGORYFACTS_TOTYPECHECK, this), new TypeChecker(
        CATEGORYFACTS_TOTYPECHECK, CATEGORYFACTS, this)));
  }

  /** Sources for category facts*/
  public static final Theme CATEGORYSOURCES = new Theme("categorySources", "The sources of category facts");

  /** Facts deduced from categories */
  public static final Theme CATEGORYFACTS = new Theme("categoryFacts", "Facts about Wikipedia instances, derived from the Wikipedia categories");

  /** Facts deduced from categories */
  public static final Theme CATEGORYFACTS_TOREDIRECT = new Theme("categoryFactsToBeRedirected",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected");

  /** Facts deduced from categories */
  public static final Theme CATEGORYFACTS_TOTYPECHECK = new Theme("categoryFactsToBeTypeChecked",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYSOURCES, CATEGORYFACTS_TOREDIRECT);
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),   "<_categoryPattern>");

    Announce.progressStart("Extracting", 3_900_000);

      /*previouis version*/
//      for (Fact fact : categoryPatterns.extract(category, titleEntity))
      for (Fact f : input.get(CategoryExtractor.CATEGORYATTS)){
        for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(f.getArg(2)),f.getArg(1))){
          write(writers, CATEGORYFACTS_TOREDIRECT, fact, CATEGORYSOURCES, FactComponent.wikipediaURL(f.getArg(1)), "CategoryExtractor");
        }
      }


  }

  /** Constructor from source file */
  public CategoryMapper() {}

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    String yago = "D:/data2/yago2s/";
//    new HardExtractor(new File("D:/data/")).extract(new File("D:/data2/yago2s/"), "test");
//    new PatternHardExtractor(new File("D:/data")).extract(new File("D:/data2/yago2s/"), "test");
    new CategoryMapper().extract(new File(yago), "Test on 1 wikipedia article");
  }
}
