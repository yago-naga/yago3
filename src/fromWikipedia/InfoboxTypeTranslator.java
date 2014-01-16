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
 * InfoboxTypeTranslator - YAGO2s
 * 
 * Translates the infobox types (right hand side) in different languages.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class InfoboxTypeTranslator extends Extractor{

  private String language;
  public static final HashMap<String, Theme> INFOBOXTYPETRANSLATEDFACTS_MAP = new HashMap<String, Theme>(); 
  
  static {
    for (String s : Extractor.languages) {
      INFOBOXTYPETRANSLATEDFACTS_MAP.put(s, new Theme("infoboxTypeTranslatedFatcs" + Extractor.langPostfixes.get(s), "Facts of infobox types with one component translateds", ThemeGroup.OTHER));
    }
  }
  
  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(
        InterLanguageLinks.INTERLANGUAGELINKS,
        PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS, 
        InfoboxExtractor.INFOBOXTYPES_MAP.get(language)
        ));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(INFOBOXTYPETRANSLATEDFACTS_MAP.get(language));
  }


  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {

    Announce.progressStart("Extracting", 3_900_000);

    Map<String, String> rdictionary = InterLanguageLinksDictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
    String infoboxWord = InterLanguageLinksDictionary.getInfDictionary( input.get(InterLanguageLinks.INTERLANGUAGELINKS)).get(language);

    for (Fact f : input.get(InfoboxExtractor.INFOBOXTYPES_MAP.get(language))){
      
      String entity = f.getArg(1); 
//          rdictionary.get(FactComponent.stripBrackets(f.getArg(1)));  
      String word = FactComponent.stripBrackets(f.getArg(2).replace(" ", "_"));
      if(word.length()>0)
      word=word.substring(0, 1).toUpperCase() + word.substring(1);
     
      String category = rdictionary.get(infoboxWord+"_"+word);
      if(category == null) 
        continue; 
    
      

          String temp = category; 
          if(temp.contains("_")){
            temp = temp.substring(temp.lastIndexOf("_") + 1);
          }
          
          Fact fact = new Fact (FactComponent.forYagoEntity(entity), "<hasInfoboxType/en>", FactComponent.forString(temp)); 
            writers.get(INFOBOXTYPETRANSLATEDFACTS_MAP.get(language)).write(fact);
 
      
    }
    

  }
  
  
  public InfoboxTypeTranslator(String lang) {
    language = lang;
  }
  
  public static void main(String[] args) throws Exception {
    new InfoboxTypeTranslator("de").extract(new File("D:/data2/yago2s/"),
        "translating category-memebership facts");

  }

}
