package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import fromOtherSources.InterLanguageLinks;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * EntityTranslator - YAGO2s
 * 
 * Translates the subjects (left hand side) of the input themes to the most English language.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class EntityTranslator extends Extractor {

  
  public static final Theme TRANSLATEDFACTS = new Theme("translatedFacts", "");
  
  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<Theme>(Arrays.asList(
        InterLanguageLinks.INTERLANGUAGELINKS,
        InfoboxExtractor.INFOBOXTYPES_MAP.get("en")
        ));
    for (String s : Extractor.languages) {
      result.add(WikipediaTypeExtractor.YAGOTYPES_MAP.get(s));
      result.add(InfoboxTypeTranslator.INFOBOXTYPETRANSLATEDFACTS_MAP.get(s));
    }
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(TRANSLATEDFACTS);
    
  }
  
  protected  FactCollection loadFacts(FactSource factSource, FactCollection temp) {
    for(Fact f: factSource){
      temp.add(f);
    }
    return(temp);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    FactCollection translatedFacts = new FactCollection();
    for(Fact f: new FactCollection(input.get(WikipediaTypeExtractor.YAGOTYPES_MAP.get("en")))){
      translatedFacts.add(f);
    }
    for(Fact f: new FactCollection(input.get(InfoboxExtractor.INFOBOXTYPES_MAP.get("en")))){
      translatedFacts.add(f);
    }
    for(String s:Extractor.languages){
      if(s.equals("en")) continue;
      Map<String, String> tempDictionary= InterLanguageLinksDictionary.get(s, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
      FactCollection temp = new FactCollection();
      loadFacts(input.get(WikipediaTypeExtractor.YAGOTYPES_MAP.get(s)), temp);
      loadFacts(input.get(InfoboxTypeTranslator.INFOBOXTYPETRANSLATEDFACTS_MAP.get(s)), temp);
      for(Fact f: temp){

        String translatedSubject  =  FactComponent.stripBrackets(f.getArg(1));
//        String translatedObject =  FactComponent.stripBrackets(f.getArg(2));


        if(tempDictionary.containsKey( FactComponent.stripBrackets(f.getArg(1)))){
          translatedSubject = tempDictionary.get(FactComponent.stripBrackets(f.getArg(1)));
        }
//        if(tempDictionary.containsKey( FactComponent.stripBrackets(f.getArg(2)))){
//          translatedObject = tempDictionary.get(FactComponent.stripBrackets(f.getArg(2)));
//        }
        translatedFacts.add(new Fact (FactComponent.forYagoEntity(translatedSubject),  f.getRelation(), FactComponent.forYagoEntity((f.getArg(2)))));
      }
    }
    
    
    for(Fact fact:translatedFacts){
      output.get(TRANSLATEDFACTS).write(fact);
    }
    
  }
  
  public static void main(String[] args) throws Exception {
    new EntityTranslator().extract(new File("D:/data2/yago2s"), null); 
  }

}
