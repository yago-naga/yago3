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
 * @author Fabian
 * 
 */
public class CategoryExtractor extends Extractor {

  /** The file from which we read */
  protected File wikipedia;

  @Override
  public File inputDataFile() {   
    return wikipedia;
  }
  
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
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
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),
        "<_categoryPattern>");
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:","#REDIRECT")) {
        case -1:
          Announce.progressDone();
          in.close();
          return;
        case 0:
          Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1:
          if (titleEntity == null) continue;
          String category = FileLines.readTo(in, ']', '|').toString();
          category = category.trim();
          for (Fact fact : categoryPatterns.extract(category, titleEntity)) {
            if (fact != null) {
              write(writers, CATEGORYFACTS_TOREDIRECT, fact, CATEGORYSOURCES, FactComponent.wikipediaURL(titleEntity), "CategoryExtractor");
            }
          }
          break;
        case 2:
          // Redirect pages have to go away
          titleEntity=null;
          break;
      }
    }
  }

  /** Constructor from source file */
  public CategoryExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    new CategoryExtractor(new File("c:/fabian/data/wikipedia/testset/puettlingen.xml")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
  }
}
