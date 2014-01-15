package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.Announce.Level;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.ExtendedFactCollection;
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
public class WikipediaTypeExtractorEN extends WikipediaTypeExtractor {
  
 
  
  protected ExtendedFactCollection getCategoryFactCollection( Map<Theme, FactSource> input) {
    return loadFacts( input.get(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language)));
  }

 

  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language));
    return temp;
  }
  

  public WikipediaTypeExtractorEN(){
    super("en");
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    WikipediaTypeExtractorEN extractor = new WikipediaTypeExtractorEN();
    extractor.extract(new File("D:/data2/yago2s/"),
        "");
    //new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    //new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
//    new WikipediaTypeExtractorEN(new File("D:/en_wikitest.xml")).extract(new File("D:/data2/yago2s/"), "Test on 1 wikipedia article");
  }
}
