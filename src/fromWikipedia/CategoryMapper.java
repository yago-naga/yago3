package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import utils.FactTemplateExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.Redirector;
import fromThemes.TypeChecker;

/**
 * CategoryMapper - YAGO2s
 * 
 * Maps the facts obtained from CategoryExtractor (Previously translated for other languages).
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */
public class CategoryMapper extends Extractor {

	  public static final Theme CATEGORYFACTS_TOREDIRECT = new Theme("categoryFactsToBeRedirected",
	          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected");
	  public static final Theme CATEGORYFACTS_TOTYPECHECK= new Theme("categoryFactsToBeTypechecked",
	          "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");
	  
  public static final Theme CATEGORYFACTS=new Theme("categoryFacts","en",
          "Facts about Wikipedia instances, derived from the Wikipedia categories"); 
  public static final Theme CATEGORYSOURCES=new Theme("categorySources","en",
          "Sources for the facts about Wikipedia instances, derived from the Wikipedia categories"); 

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS,
        CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language), CategoryExtractor.CATEGORYMEMBERS.inLanguage(language)));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYFACTS_TOREDIRECT.inLanguage(language), CATEGORYSOURCES.inLanguage(language));
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(
        new Redirector(CATEGORYFACTS_TOREDIRECT.inLanguage(language), CATEGORYFACTS_TOTYPECHECK.inLanguage(language), this, this.language),
        new TypeChecker(CATEGORYFACTS_TOTYPECHECK.inLanguage(language), CATEGORYFACTS.inLanguage(language), this)));
  }


  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),
        "<_categoryPattern>");

    //Announce.progressStart("Extracting", 3_900_000);

    FactCollection factCollection;
    if (language.equals("en")) factCollection = new FactCollection(input.get(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language)));
    else factCollection = new FactCollection(input.get(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language)));

    for (Fact f : factCollection) {
      for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(f.getArg(2).replace('_', ' ')), f.getArg(1))) {
        if (fact != null) {
          write(writers, CATEGORYFACTS_TOREDIRECT.inLanguage(language), fact, CATEGORYSOURCES.inLanguage(language),
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
