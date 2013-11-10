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
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;


public class CategoryMapperEN extends CategoryMapper{
  
 
  
  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language));
    return temp;
  }
  

  
  
  public CategoryMapperEN(){
    super("en");
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
   
    
      for (Fact f : input.get(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language))){
        for (Fact fact : categoryPatterns.extract(FactComponent.stripQuotes(f.getArg(2)),f.getArg(1))){
          write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(f.getArg(1)), "CategoryMapper");
        }
      }


  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    CategoryMapperEN extractor = new CategoryMapperEN();
    extractor.extract(new File("D:/data2/yago2s/"),
        "mapping infobox attributes into infobox facts");
  }
  
  

}
