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


  /** Sources for category facts*/
  public static final Theme CATEGORYATTSOURCES = new Theme("categoryAttSources", "The sources of category facts");

  /** Facts deduced from categories */
  public static final Theme CATEGORYATTS = new Theme("categoryAttributes",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected");


  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYATTSOURCES, CATEGORYATTS);
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
          
          write(writers, CATEGORYATTS, new Fact(titleEntity, "<hasWikiCategory>", FactComponent.forString(category)),CATEGORYATTSOURCES, 
              FactComponent.wikipediaURL(titleEntity), "CategoryExtractor" );
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
//    new HardExtractor(new File("D:/data/")).extract(new File("D:/data2/yago2s/"), "test");
//    new PatternHardExtractor(new File("D:/data")).extract(new File("D:/data2/yago2s/"), "test");
    new CategoryExtractor(new File("D:/en_wikitest.xml")).extract(new File("D:/Data2/yago2s"), "Test on 1 wikipedia article");
  }
}
