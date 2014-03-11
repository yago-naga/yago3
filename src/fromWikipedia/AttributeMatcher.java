package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javatools.datatypes.FrequencyVector;
import javatools.datatypes.Pair;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromThemes.InfoboxTermExtractor;

/**
 * YAGO2s - AttributeMatcher
 * 
 * This Extractor matches multilingual Infobox attributes to English attributes. 
 * 'German' can be replaced by any arbitrary language as second language. 
 * 
 * @author Farzaneh Mahdisoltani
 */

public class AttributeMatcher extends Extractor {
  
  private static ExtendedFactCollection yagoFactCollection = null;
  
  /*Map of German attribute to YAGO relations to |Test & Gold|, |Test| */
  Map<String, Map<String, Pair<Integer, Integer>>> statistics;
  
 
  private String language;
 
  private double WILSON_THRESHOLD = 0;
  
  private double SUPPORT_THRESHOLD = 1;
  
  public static final HashMap<String, Theme> MATCHED_INFOBOXATTS_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> MATCHEDATTSOURCES_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> MATCHED_INFOBOXATTS_SCORES_MAP = new HashMap<String, Theme>();
  
  static {
    for (String s : Extractor.languages) {
      MATCHED_INFOBOXATTS_MAP.put(s, new Theme("matchedAttributes" + Extractor.langPostfixes.get(s),
          "Attributes of the Wikipedia infoboxes in different languages are matched.", ThemeGroup.OTHER));
      MATCHEDATTSOURCES_MAP.put(s, new Theme("matchedAttributesSources" + Extractor.langPostfixes.get(s), "Sources of matched attributes",
          ThemeGroup.OTHER));
      MATCHED_INFOBOXATTS_SCORES_MAP.put(s, new Theme("matchedAttributesScores" + Extractor.langPostfixes.get(s),
              "Attributes of the Wikipedia infoboxes in different languages are matched.", ThemeGroup.OTHER));
    }

  }
  
  @Override
  public Set<Theme> input() {
    HashSet<Theme> result = new HashSet<Theme>(Arrays.asList(InfoboxMapper.INFOBOXFACTS_MAP.get("en"),
    
    InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language)));
    return result;
  }
  
  @Override
  public Set<Theme> output() {
    return new HashSet<>(Arrays.asList(MATCHED_INFOBOXATTS_MAP.get(language), MATCHEDATTSOURCES_MAP.get(language)));
  }
  
  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    statistics = new HashMap<String, Map<String, Pair<Integer, Integer>>>();
    ExtendedFactCollection englishFactCollection = getFactCollection(input.get(InfoboxMapper.INFOBOXFACTS_MAP.get("en")));
    FactCollection germanFactCollection = new FactCollection(input.get(InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language)));
    
    /*Keeps the German attributes which are already processed.*/
    Set<String> germanProcessed = new HashSet<String>();
    
    for (Fact f1 : germanFactCollection) {
      
      String germanRelation = f1.getRelation();
      String germanSubject = f1.getArg(1);
      String germanObject = f1.getArg(2);
      if (germanProcessed.contains(germanRelation)) continue;
      
      /* We look for German attributes where both subject and object (in translated form) appear in YAGO. 
       * If the attribute is skipped at this level, it still has the chance to be processed if appears 
       * with 'good' subject and object in other facts */
      if (englishFactCollection.getFactsWithSubject(germanSubject) == null || englishFactCollection.getFactsWithObject(germanObject) == null) continue;
      List<Fact> germanFactsWithRelation = germanFactCollection.get(germanRelation);
      
      /* <Subject, Object> pairs appearing in YAGO from the facts with specific German attribute are added to test.  */
      Set<Pair<String, String>> test = new HashSet<Pair<String, String>>();
      for (Fact f : germanFactsWithRelation) {
        if (!(englishFactCollection.getFactsWithSubject(germanSubject) == null || englishFactCollection.getFactsWithObject(germanObject) == null)) test
            .add(new Pair<String, String>(f.getArg(1), f.getArg(2)));
      }
      
      /*Keeps the YAGO relations which are already processed with this specific German attribute.*/
      Set<String> pairProcessed = new HashSet<String>();
      for (Fact f2 : englishFactCollection) {
        
        String englishRelation = f2.getRelation();
        
        if (pairProcessed.contains(englishRelation)) continue;
        
        List<Fact> englishFactswithRelation = englishFactCollection.get(englishRelation);
        
        /* The size of the intersection of Gold and test sets */
        int testNGold = 0;
        
        for (Fact f4 : englishFactswithRelation) {
          if (test.contains(new Pair<String, String>(f4.getArg(1), f4.getArg(2)))) testNGold++;
        }
        
        deduce(germanRelation, englishRelation, testNGold, test.size());
        pairProcessed.add(englishRelation);
      }
      germanProcessed.add(germanRelation);
    }
    
    for (Entry<String, Map<String, Pair<Integer, Integer>>> entry : statistics.entrySet()) {
      Map<String, Pair<Integer, Integer>> temp = entry.getValue();
      if (temp != null) {
        for (Entry<String, Pair<Integer, Integer>> subEntry : temp.entrySet()) {
          int total = subEntry.getValue().second;
          int correct = subEntry.getValue().first;
          double[] ws = FrequencyVector.wilson(total, correct);
          
          if (correct >= 0) {
            Fact scoreFact = new Fact(entry.getKey(), (double) correct / total + " <" + correct + "/" + total + ">" + "     " + ws[0] + "    " + ws[1],
                subEntry.getKey());
            writers.get(MATCHED_INFOBOXATTS_SCORES_MAP.get(language)).write(scoreFact);
            
            /** filtering out */
            if (ws[0] - ws[1] > WILSON_THRESHOLD && correct > SUPPORT_THRESHOLD) {
            	
              Fact fact = new Fact(entry.getKey(), "<_infoboxPattern>", subEntry.getKey());
              
              write(writers, MATCHED_INFOBOXATTS_MAP.get(language), fact, MATCHEDATTSOURCES_MAP.get(language),
                  FactComponent.wikipediaURL(entry.getKey()), "");
            }
          }
        }
      }
    }
    
  }
  
  private static synchronized ExtendedFactCollection getFactCollection(FactSource infoboxFacts) {
    if (yagoFactCollection != null) return (yagoFactCollection);
    yagoFactCollection = new ExtendedFactCollection();
    for (Fact f : infoboxFacts) {
      yagoFactCollection.add(f);
    }
    return (yagoFactCollection);
  }
  
  public void deduce(String germanRelation, String englishRelation, int intersectionSize, int testSize) throws IOException {
    
    if (!statistics.containsKey(germanRelation)) {
      statistics.put(germanRelation, new HashMap<String, Pair<Integer, Integer>>());
    }
    
    statistics.get(germanRelation).put(englishRelation, new Pair<Integer, Integer>(intersectionSize, testSize));
    
  }
  
  public static Set<String> getIntersection(Set<String> set1, Set<String> set2) {
    boolean set1IsLarger = set1.size() > set2.size();
    Set<String> cloneSet = new HashSet<String>(set1IsLarger ? set2 : set1);
    cloneSet.retainAll(set1IsLarger ? set1 : set2);
    return cloneSet;
  }
  
  public static Set<String> getUnion(Set<String> set1, Set<String> set2) {
    Set<String> cloneSet = new HashSet<String>(set1);
    cloneSet.addAll(set2);
    return cloneSet;
  }
  
  public void setWilsonThreshold(double d) {
    WILSON_THRESHOLD = d;
  }
  
  public void setSupportThreshold(double d) {
    SUPPORT_THRESHOLD = d;
  }
  
  public AttributeMatcher(String secondLang) {
    language = secondLang;
  }
  
  public static void main(String[] args) throws Exception {
    
    new AttributeMatcher("de").extract(new File("D:/data3/yago2s"), "mapping infobox attributes in different languages");
  }
  
}
