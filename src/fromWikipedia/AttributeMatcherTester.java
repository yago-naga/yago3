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

  private File inputFolder;
  private File outputFolder;
  
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
  
  public AttributeMatcherTester(File inputFolder, File outputFolder) {
    this.outputFolder = outputFolder; 
    this.inputFolder = inputFolder; 
  }
  public AttributeMatcherTester(File folder) {
    this.outputFolder = folder; 
    this.inputFolder = folder;
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    for(String s: Extractor.languages){
      AttributeMatcher am = new AttributeMatcher(s);
      am.extract(outputFolder, "Test on wikipedia article");
    }
    
  }
  
  public static void main(String[] args) throws Exception {
//  Extractor extractor = Extractor.forName("fromWikipedia.MultiInfoboxExtractorTester", null);
//  extractor.extract(new File("C:/Users/Administrator/data2/yago2s/"),
//      "blah blah");
  File f =  new File("/san/fmahdiso/Data2/yago2s");
  new AttributeMatcherTester(f).extract( f, null);
}

}
