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
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;


public class CategoryTranslator extends Extractor{

  private String language;
  public static final HashMap<String, Theme> COMPLETELY_TRANSLATEDFACTS_MAP = new HashMap<String, Theme>(); 
  public static final HashMap<String, Theme> PARTLY_TRANSLATEDFACTS_MAP = new HashMap<String, Theme>(); 
  
  static {
    for (String s : Extractor.languages) {
      COMPLETELY_TRANSLATEDFACTS_MAP.put(s, new Theme("completelyTranslatedFacts_" + s, 
          "Facts of categoryExtractor with both components translated", ThemeGroup.OTHER));
      PARTLY_TRANSLATEDFACTS_MAP.put(s, new Theme("partlyTranslatedFatcs_" + s, "Facts of categoryExtractor with one component translateds", ThemeGroup.OTHER));
    }
  }
  
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS, InterLanguageLinks.INTERLANGUAGELINKS, CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language)));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(COMPLETELY_TRANSLATEDFACTS_MAP.get(language), PARTLY_TRANSLATEDFACTS_MAP.get(language));
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

    for (Fact f : input.get(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(language))){
      
      Set<String> entities = rdictionary.get(FactComponent.stripBrackets(f.getArg(1)));  
      Set<String> categories = rdictionary.get(categoryWord+":"+FactComponent.stripQuotes(f.getArg(2).replace(" ", "_")));
   
      if(categories == null) { 
        //TODO: not sure if these guys are also usefull 
        continue; 
        
      } 
      
      if(entities == null) {
        for(String category: categories){
          String temp = category; 
          if(temp.contains(":")){
            temp = temp.substring(temp.lastIndexOf(":") + 1);
          }
          Fact fact = new Fact (FactComponent.forYagoEntity(f.getArg(1)), "<hasWikiCategory/en>", FactComponent.forString(temp)); 
          writers.get(PARTLY_TRANSLATEDFACTS_MAP.get(language)).write(fact);
        }
continue; 
      }
      
     
       
     
      for(String entity:entities){
        for(String category: categories){

//          for (Fact fact : categoryPatterns.extract(category.replace('_', ' ').replace("Category:", ""),FactComponent.forYagoEntity(entity))){
//            if( fact ==null) continue;
          String temp = category; 
          if(temp.contains(":")){
            temp = temp.substring(temp.lastIndexOf(":") + 1);
          }
          Fact fact = new Fact (FactComponent.forYagoEntity(entity), "<hasWikiCategory/en>", FactComponent.forString(temp)); 
            writers.get(COMPLETELY_TRANSLATEDFACTS_MAP.get(language)).write(fact);
//            write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(FactComponent.forYagoEntity(entity)), "CategoryExtractor");
//          }
        }
      }
      
    }
    

  }
  public CategoryTranslator(String lang) {
    language = lang;
  }
  
  public static void main(String[] args) throws Exception {
    new CategoryTranslator("de").extract(new File("D:/data2/yago2s/"),
        "translating category-memebership facts");

  }

}
