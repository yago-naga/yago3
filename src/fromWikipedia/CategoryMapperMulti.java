package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactTemplateExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;


public class CategoryMapperMulti extends CategoryMapper{
 

 
  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(CategoryTranslator.PARTLY_TRANSLATEDFACTS_MAP.get(language));
    temp.add(CategoryTranslator.COMPLETELY_TRANSLATEDFACTS_MAP.get(language));
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
   
    
//      for (Fact f : input.get(CategoryExtractor.CATEGORYATTS_MAP.get(language))){
//        System.out.println("PPPPPPPPPPPPPPPPPPPPPPPP");
//        System.out.println(FactComponent.stripQuotes(f.getArg(2)));
//        System.out.println(f.getArg(1));
//        //Staat in Afrika
////        <Republik_Kongo>
//        System.out.println("PPPPPPPPPPPPPPPPPPPPPPPP");
//        for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(f.getArg(2)),f.getArg(1))){
//          write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(f.getArg(1)), "CategoryMapper");
//        }
//      } 
    
    for (Fact f : input.get(CategoryTranslator.PARTLY_TRANSLATEDFACTS_MAP.get(language))){
        String temp= f.getArg(2);
        if(f.getArg(2).contains("_"))
          temp =  f.getArg(2).replace("_", " ");
        for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(temp),f.getArg(1))){
          if(fact!=null)
          write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(f.getArg(1)), "CategoryMapper");
        }
        
      }
      for (Fact f : input.get(CategoryTranslator.COMPLETELY_TRANSLATEDFACTS_MAP.get(language))){
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

  public CategoryMapperMulti(String lang) {
    super(lang);
  }
  
  public static void main(String[] args) throws Exception {
    new CategoryMapperMulti("de").extract(new File("D:/data2/yago2s/"),
        "mapping infobox attributes into infobox facts");

  }

}
