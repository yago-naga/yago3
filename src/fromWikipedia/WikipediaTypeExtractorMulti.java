package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactSource;
import basics.Theme;


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
    Announce.setLevel(Announce.Level.DEBUG);
    WikipediaTypeExtractorMulti extractor = new WikipediaTypeExtractorMulti("de");
    extractor.extract(new File("D:/data2/yago2s/"),
        "");
  }

}
