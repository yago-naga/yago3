package fromWikipedia;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * CategoryExtractorTester - YAGO2s
 * 
 * Tests AttributeMatcher for different languages
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class AttributeMatcherTester extends Extractor {

  
  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(); 
  }

  @Override
  public Set<Theme> output() {
    Set<Theme> result = new TreeSet<Theme>();
    for(String s:Extractor.languages){
      result.add(AttributeMatcher.MATCHED_INFOBOXATTS_MAP.get(s));
      result.add(AttributeMatcher.MATCHEDATTSOURCES_MAP.get(s));
    }
    return result;
  }
  

  public AttributeMatcherTester() {
  }

  @Override
  public void extract(File inputFolder, File outputFolder, String header) throws Exception{
    for(String s: Extractor.languages){
      AttributeMatcher am = new AttributeMatcher(s);
      am.setSupportThreshold(1);
      am.setWilsonThreshold(0);
      am.extract(inputFolder, "Test on wikipedia article");
    }
    
  }
  
  public static void main(String[] args) throws Exception {
//  Extractor extractor = Extractor.forName("fromWikipedia.MultiInfoboxExtractorTester", null);
//  extractor.extract(new File("C:/Users/Administrator/data2/yago2s/"),
//      "blah blah");
  File f =  new File("D:/data2/yago2s");
  new AttributeMatcherTester().extract( f, null);
}

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
  }

}
