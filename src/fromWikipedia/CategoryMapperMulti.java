package fromWikipedia;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import utils.FactTemplateExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;


public class CategoryMapperMulti extends CategoryMapper{
 

  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(InterLanguageLinks.INTERLANGUAGELINKS);
    return temp;
  }
  
  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),   "<_categoryPattern>");

    Announce.progressStart("Extracting", 3_900_000);

    /*previouis version*/
    //    for (Fact fact : categoryPatterns.extract(category, titleEntity)) {
    //      if (fact != null) {
    //        write(writers, CATEGORYFACTS_TOREDIRECT, fact, CATEGORYSOURCES, FactComponent.wikipediaURL(titleEntity), "CategoryExtractor");
    //      }
    //    }

    Map<String, Set<String>> rdictionary = Dictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));

String categoryWord = Dictionary.getCatDictionary(input.get(InterLanguageLinks.INTERLANGUAGELINKS)).get(language);
int count1 =0;
int count2 =0;
int total =0;
    for (Fact f : input.get(CategoryExtractor.CATEGORYATTS_MAP.get(language))){
      
      Set<String> entities = rdictionary.get(FactComponent.stripBrackets(f.getArg(1)));
      
      Set<String> categories = rdictionary.get(categoryWord+":"+FactComponent.stripQuotes(f.getArg(2)));
      total++;
      if(entities == null) {
        System.out.println(FactComponent.stripBrackets(f.getArg(1)));
        count2++;
        continue; 
      
      }
      
      if(categories == null) { 
        count1++;
        continue; 
        
      } 
       
     
      for(String entity:entities){
        for(String category: categories){

          for (Fact fact : categoryPatterns.extract(category.replace('_', ' ').replace("Category:", ""),FactComponent.forYagoEntity(entity))){
            if( fact ==null) continue;
            write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(FactComponent.forYagoEntity(entity)), "CategoryExtractor");
          }
        }
      }
      
    }
    

  }
  
  public CategoryMapperMulti(String lang) {
    super(lang);
  }
  
  public static void main(String[] args) throws Exception {
    new CategoryMapperMulti("de").extract(new File("D:/data2/yago2s/"),
        "mapping infobox attributes into infobox facts");

  }

}
