package fromWikipedia;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import basics.ExtendedFactCollection;
import basics.FactSource;
import basics.Theme;

/**
 * WikipediaTypeExtractor - YAGO2s
 * 
 * Extracts types from categories and infoboxes
 * for non-English Languages
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class WikipediaTypeExtractorMulti extends WikipediaTypeExtractor {

  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(CategoryTranslator.CATEGORYTRANSLATEDFACTS_MAP.get(language));
    return temp;
  }

  public WikipediaTypeExtractorMulti(String lang) {
    super(lang);
  }

  @Override
  protected ExtendedFactCollection getCategoryFactCollection( Map<Theme, FactSource> input) {
    ExtendedFactCollection result = new ExtendedFactCollection();
    loadFacts(input.get(CategoryTranslator.CATEGORYTRANSLATEDFACTS_MAP.get(language)), result) ;
    return result;
    
  }
  
  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.MUTE);
    WikipediaTypeExtractorMulti extractor = new WikipediaTypeExtractorMulti("de");
    extractor.extract(new File("D:/data2/yago2s/"),
        "");
  }

}
