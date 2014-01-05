package fromWikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javatools.administrative.Announce;
import javatools.administrative.Announce.Level;
import javatools.datatypes.FrequencyVector;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import fromOtherSources.HardExtractor;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.ExtendedFactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.N4Reader;
import basics.Theme;
import basics.YAGO;
import basics.Theme.ThemeGroup;

/**
 * YAGO2s - AttributeMatcher
 * 
 * This Extractor matches cross-lingual infobox categories. 
 * 
 * @author Farzaneh Mahdisoltani
 */

public class CategoryMatcher extends Extractor { 
  
  private static ExtendedFactCollection yagoFactCollection = null;
  /*the reverse dictionary*/
  
  Map<String, Map<String,Pair<Integer, Integer>>> statistics;
  private String language;
  private Map<String, String> rdictionary; 
  final double WILSON_THRESHOLD = 0.2;
  final double SUPPORT_THRESHOLD = 10;

  public CategoryMatcher(String secondLang) throws FileNotFoundException, IOException{
    language = secondLang;
  }

  public static final HashMap<String, Theme> MATCHED_CAT_MAP = new HashMap<String, Theme>();
  public static final HashMap<String, Theme> MATCHEDCATSOURCES_MAP = new HashMap<String, Theme>();

  static {
    for (String s : Extractor.languages) {
      MATCHED_CAT_MAP.put(s, new Theme("matchedCategories"+ Extractor.langPostfixes.get(s),
          "Attributes of the Wikipedia infoboxes in different languages are matched.", ThemeGroup.OTHER));
      MATCHEDCATSOURCES_MAP.put(s, new Theme("matchedCatSources" + Extractor.langPostfixes.get(s), "Sources of infobox", ThemeGroup.OTHER));
    }

  }


  @Override
  public Set<Theme> input() {
    HashSet<Theme> result = new HashSet<Theme>(Arrays.asList(PatternHardExtractor.INFOBOXPATTERNS, 
        HardExtractor.HARDWIREDFACTS, 
        WordnetExtractor.WORDNETWORDS,
        InterLanguageLinks.INTERLANGUAGELINKS,
        CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get("en"),
        CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language)
        ));
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new HashSet<>(Arrays.asList(MATCHED_CAT_MAP.get(language),MATCHEDCATSOURCES_MAP.get(language)));
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    rdictionary = Dictionary.get(language);
    statistics = new HashMap<String, Map<String,Pair <Integer,Integer>>>();
    ExtendedFactCollection enFactCollection = getFactCollection(input.get(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get("en")));

    FactSource lang2FactSource= input.get(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language));

    Announce.progressStart("Running through foreign Wikipedia", 12713794);
    for (Fact f2 : lang2FactSource){
      Announce.progressStep();
      String secondLangSubject = FactComponent.stripBrackets(f2.getArg(1));
      String secondLangObject = f2.getArg(2);
      
      String enSubject = rdictionary.get(secondLangSubject);
      if(enSubject==null) continue;

      List<Fact> enFactsWithSubject = enFactCollection.getFactsWithSubject(FactComponent.forYagoEntity(enSubject));

      for(Fact f: enFactsWithSubject){
        String enObject = FactComponent.stripBrackets(f.getArg(2));
        deduce(enObject, secondLangObject, true);
      }


    }
    
    Announce.progressDone();
    
    for (Entry<String, Map<String, Pair<Integer, Integer>>> entry : statistics.entrySet())
    {
      Map<String, Pair<Integer, Integer>> temp = entry.getValue(); 
      if(temp != null){
        for (Entry <String, Pair<Integer, Integer>> subEntry : temp.entrySet() ){
         int total =  subEntry.getValue().second;
         int correct = subEntry.getValue().first;
           double[] ws=  FrequencyVector.wilson(total, correct);
            
          if((float)subEntry.getValue().first/subEntry.getValue().second >= 0 && total > 1){
            Fact fact = new Fact(entry.getKey(), (double)correct /total + " <" +
               correct + "/" +total +">" + "     " +ws[0] + "    " + ws[1] , subEntry.getKey());
            /*filtering out */
//            if(ws[0] - ws[1] > WILSON_THRESHOLD  && correct> SUPPORT_THRESHOLD){
//              Fact fact = new Fact(subEntry.getKey(),"<sameCategoryAs>", entry.getKey());
              write(writers, MATCHED_CAT_MAP.get(language), fact, MATCHEDCATSOURCES_MAP.get(language),
                  FactComponent.wikipediaURL(entry.getKey()),"");
//            }
            
          }
        }
      }
      
    }

  }
  
  
  private static synchronized ExtendedFactCollection getFactCollection(FactSource infoboxFacts) {
    if(yagoFactCollection!=null) return(yagoFactCollection);
    yagoFactCollection=new ExtendedFactCollection();
    //File f2 = new File("C:/Users/Administrator/data2/yago2s/");
    //FactSource yagoFactSource = FactSource.from(InfoboxMapper.INFOBOXFACTS_TOREDIRECT.file(f2)); 
    for(Fact f: infoboxFacts){
      yagoFactCollection.add(f);
    }
    return(yagoFactCollection);
  }
  
   private static synchronized ExtendedFactCollection getFactCollection(File yagoFolder) throws FileNotFoundException, IOException {
      if(yagoFactCollection!=null) return(yagoFactCollection);
      yagoFactCollection=new ExtendedFactCollection();
      for (File factsFile : yagoFolder.listFiles()) {
        
        if (factsFile.getName().endsWith("Facts.ttl")) {
          System.out.println("loading "+factsFile.getName());
          N4Reader nr = new N4Reader(FileUtils.getBufferedUTF8Reader(factsFile));
          while(nr.hasNext()){
            Fact f = nr.next();
            yagoFactCollection.add(f);
          }
   

        }
      }
      return(yagoFactCollection);
    }

  public boolean isEntity(String type){
    switch (type) {
        case YAGO.entity:
        case "rdf:Resource":
        case "<yagoLegalActorGeo>":
          return true;
    }
    return false;
  }
 
 
  public void deduce(String yagoRelation, String secondLangRelation, boolean both) throws IOException{

    secondLangRelation = preprocess(secondLangRelation);
    if(!statistics.containsKey(yagoRelation)){
      statistics.put(yagoRelation, new HashMap<String,Pair<Integer,Integer>>());
    }
    if(!statistics.get(yagoRelation).containsKey(secondLangRelation)){
      statistics.get(yagoRelation).put(secondLangRelation,new Pair<Integer, Integer>(0, 0));
    }
    statistics.get(yagoRelation).get(secondLangRelation).first+= both?1:0;
    statistics.get(yagoRelation).get(secondLangRelation).second+= 1;
  }






  public static String preprocess(String input) throws IOException{
    if(input.contains("\n"))
      input=input.replace("\n", "");
    StringReader reader = new StringReader(input);
    switch(FileLines.findIgnoreCase(reader, "<ref>", "<br />")){
      case 0:
        CharSequence temp = FileLines.readTo(reader, "</ref>");
        input = input.replace("<ref>"+temp, "");
        break;
      case 1:
        input = input.replace("<br />", "");
        break;
    }
    
    
    for (int i = 1; i < input.length()-1; i++) 
      if((input.charAt(i) == '.') && Character.isDigit(input.charAt(i-1)) && Character.isDigit(input.charAt(i+1)))
        input = input.replace(".", "");

    for (int i = 1; i < input.length()-1; i++) 
      if((input.charAt(i) == ',') && Character.isDigit(input.charAt(i-1)) && Character.isDigit(input.charAt(i+1)))
        input = input.replace(",", ".");

    return input;
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    String mylang= "de"; 

    Announce.setLevel(Level.MESSAGES);
    new CategoryMatcher(mylang)
    .extract(new File("D:/data2/yago2s"), 
        "mapping infobox attributes in different languages");



  } 


}
