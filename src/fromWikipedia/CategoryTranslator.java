package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;

/**
 * CategoryTranslator - YAGO2s
 * 
 * Translates the categories (right hand side) for category membership facts in different languages.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class CategoryTranslator extends Extractor{

  private String language;
  public static final HashMap<String, Theme> CATEGORYTRANSLATEDFACTS_MAP = new HashMap<String, Theme>(); 
  
  static {
    for (String s : Extractor.languages) {
      CATEGORYTRANSLATEDFACTS_MAP.put(s, new Theme("categoryTranslatedFatcs" + Extractor.langPostfixes.get(s), "Facts of categoryExtractor with one component translateds", ThemeGroup.OTHER));
    }
  }
  
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(
        InterLanguageLinks.INTERLANGUAGELINKS,
        PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS, 
        CategoryExtractor.CATEGORYMEMBERSHIPTRANSLATED_MAP.get(language)));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYTRANSLATEDFACTS_MAP.get(language));
  }


  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
//    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS)),   "<_categoryPattern>");

    Announce.progressStart("Extracting", 3_900_000);
    Map<String, String> rdictionary = InterLanguageLinksDictionary.get(language, input.get( InterLanguageLinks.INTERLANGUAGELINKS));
    String categoryWord  = InterLanguageLinksDictionary.getCatDictionary(input.get( InterLanguageLinks.INTERLANGUAGELINKS)).get(language); //= "Kategorie"; 
    for (Fact f : input.get(CategoryExtractor.CATEGORYMEMBERSHIPTRANSLATED_MAP.get(language))){
      System.out.println(f);
      String entity = f.getArg(1); 
      System.out.println("OOOOOOOOOOOOOOOOOOOOOOOOOOOOO " + categoryWord+":"+FactComponent.stripBrackets(f.getArg(2).replace(" ", "_")));
      String category = rdictionary.get(categoryWord+":"+FactComponent.stripBrackets(f.getArg(2).replace(" ", "_")));
     
      if(category == null) 
        continue; 
    
      
//      if(entity == null) {
////        for(String category: categories){
//          String temp = category; 
//          if(temp.contains(":")){
//            temp = temp.substring(temp.lastIndexOf(":") + 1);
//          }
//          Fact fact = new Fact (FactComponent.forYagoEntity(f.getArg(1)), "<hasWikiCategory/en>", FactComponent.forString(temp)); 
//          writers.get(PARTLY_TRANSLATEDFACTS_MAP.get(language)).write(fact);
////        }
//continue; 
//      }
      
     
       
     
//      for(String entity:entities){
//        for(String category: categories){

//          for (Fact fact : categoryPatterns.extract(category.replace('_', ' ').replace("Category:", ""),FactComponent.forYagoEntity(entity))){
//            if( fact ==null) continue;
          String temp = category; 
          if(temp.contains(":")){
            temp = temp.substring(temp.lastIndexOf(":") + 1);
          }
          
          Fact fact = new Fact (FactComponent.forYagoEntity(entity), "<hasWikiCategory/en>", FactComponent.forString(temp)); 
            writers.get(CATEGORYTRANSLATEDFACTS_MAP.get(language)).write(fact);
//            write(writers, CATEGORYFACTS_TOREDIRECT_MAP.get(language), fact, CATEGORYSOURCES_MAP.get(language), FactComponent.wikipediaURL(FactComponent.forYagoEntity(entity)), "CategoryExtractor");
//          }
//        }
//      }
      
    }
    

  }
  
  
  public CategoryTranslator(String lang) {
    language = lang;
  }
  
  public static void main(String[] args) throws Exception {
    new CategoryTranslator("de").extract(new File("D:/data3/yago2s/"),
        "translating category-memebership facts");

  }

}
