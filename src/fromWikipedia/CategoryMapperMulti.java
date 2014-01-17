package fromWikipedia;

import java.io.File;
import java.util.Map;
import java.util.Set;
import javatools.administrative.Announce;
import utils.FactTemplateExtractor;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.PatternHardExtractor;

/**
 * CategoryMapper - YAGO2s
 * 
 * Maps the facts in the output of CategoryTranslator
 * for non-English languages
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class CategoryMapperMulti extends CategoryMapper{

  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(CategoryTranslator.CATEGORYTRANSLATEDFACTS_MAP.get(language));
    return temp;
  }
  

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),   "<_categoryPattern>");

    Announce.progressStart("Extracting", 3_900_000);
      
      ExtendedFactCollection result = getCategoryFactCollection(input);
      for (Fact f: result){
        String temp= f.getArg(2);
        if(f.getArg(2).contains("_"))
          temp =  f.getArg(2).replace("_", " ");
        for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(temp),f.getArg(1))){
          if(fact!=null){
          write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(f.getArg(1)), "CategoryMapper");
          }
        }
      }
     
  }
  
  protected ExtendedFactCollection getCategoryFactCollection( Map<Theme, FactSource> input) {
    ExtendedFactCollection result = new ExtendedFactCollection();
    loadFacts(input.get(CategoryTranslator.CATEGORYTRANSLATEDFACTS_MAP.get(language)), result) ;
    return result;
    
  }
  
  public CategoryMapperMulti(String lang) {
    super(lang);
  }
  
  public static void main(String[] args) throws Exception {
    new CategoryMapperMulti("de").extract(new File("D:/data2/yago2s/"),
        "mapping infobox attributes into infobox facts");

  }

}
