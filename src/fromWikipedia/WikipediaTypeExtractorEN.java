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
  
   
//  @Override
//  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
////    Announce.setLevel(Level.MUTE);
//    ExtendedFactCollection infoboxTypes = getInfoboxTypesFactCollection(input.get(InfoboxExtractor.INFOBOXTYPES_MAP.get(language)));
//    ExtendedFactCollection infoboxAtts = getInfoboxFactCollection(input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(language)));
//    ExtendedFactCollection categoryAtts = getCategoryFactCollection(input);
//
//    nonConceptualInfoboxes = new HashSet<>();
//    for (Fact f : new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS)).getBySecondArgSlow(RDFS.type, "<_yagoNonConceptualInfobox>")) {
//      nonConceptualInfoboxes.add(f.getArgJavaString(1));
//    }
//    nonConceptualCategories = new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)).asStringSet("<_yagoNonConceptualWord>");
//    preferredMeanings = WordnetExtractor.preferredMeanings(input);
//    wordnetClasses = new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES),true);
//    wordnetClasses.load(input.get(HardExtractor.HARDWIREDFACTS));
//    categoryClassFacts = new FactCollection();
//    yagoBranches = new HashMap<String, String>();
//    TitleExtractor titleExtractor = new TitleExtractor(input);
//    
// 
//    // Extract the information
//    Announce.progressStart("Extracting", 3_900_000);
////    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
//    Set<String> typesOfCurrentEntity = new HashSet<>();
//    List<Fact> factsWithSubject = new ArrayList<Fact>();
//    
//    Set<String> titles= new HashSet<String>();
//    for(Fact f2:categoryAtts){
//      titles.add(f2.getArg(1));
//    }
//    for(Fact f2:infoboxAtts){
//      titles.add(f2.getArg(1));
//    }
//
//    String currentEntity = null; 
//    for(String title:titles){
//      flush(currentEntity, typesOfCurrentEntity, writers);
//      currentEntity = title;
//      if (currentEntity == null) continue;
//
//      factsWithSubject = categoryAtts.getFactsWithSubject(currentEntity);  
//      for(Fact f: factsWithSubject){
//        String category = f.getArgJavaString(2);
//        extractType(f.getArg(1), category, typesOfCurrentEntity);
//      }
//      
//      factsWithSubject =infoboxTypes.getFactsWithSubject(currentEntity);
//      if(factsWithSubject.size()<1) continue; 
//      String cls = FactComponent.stripBrackets(infoboxTypes.getFactsWithSubject(currentEntity).get(0).getArg(2).replace("_", " "));
//      if (Character.isDigit(Char.last(cls))) cls = Char.cutLast(cls);
//      if (!nonConceptualInfoboxes.contains(cls)) {
//        String type = preferredMeanings.get(cls);
//        if (type != null) typesOfCurrentEntity.add(type);
//      }
//      
//    }
//    flush(currentEntity, typesOfCurrentEntity, writers);
//    
//    Announce.progressDone();
//
//    Announce.doing("Writing classes");
//    for (Fact f : categoryClassFacts) {
//      if (FactComponent.isFactId(f.getArg(1))) writers.get(WIKIPEDIATYPESOURCES_MAP.get(language)).write(f);
//      else writers.get(WIKIPEDIACLASSES_MAP.get(language)).write(f);
//    }
//    Announce.done();
//
//    Announce.doing("Writing hard wired types");
//    for (Fact f : input.get(HardExtractor.HARDWIREDFACTS)) {
//      if (f.getRelation().equals(RDFS.type)) write(writers, YAGOTYPES_MAP.get(language), f, WIKIPEDIATYPESOURCES_MAP.get(language), YAGO.yago, "Manual");
//    }
//    Announce.done();
//
//    this.categoryClassFacts=null;
//    this.nonConceptualCategories=null;
//    this.nonConceptualInfoboxes=null;
//    this.preferredMeanings=null;
//    this.wordnetClasses=null;
//    this.yagoBranches=null;
////    in.close();
//  }


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
