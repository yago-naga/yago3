package fromWikipedia;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import utils.PatternList;
import utils.TermExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;


public class InfoboxMapper_multi extends InfoboxMapper{
  
  @Override
  public Set<Theme> input() {
    Set<Theme> temp = super.input();
    temp.add(InterLanguageLinks.INTERLANGUAGELINKS);
    temp.add(AttributeMatcher.MATCHED_INFOBOXATTS_MAP.get(language));
    return temp;
  }
  
  public static Map<String, Set<String>> infoboxMatchings(FactCollection facts) {
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();
    Announce.doing("Compiling infobox patterns");
    for (Fact f : facts) {
      D.addKeyValue(map, f.getArg(1), f.getArg(2), TreeSet.class);
    }
    if (map.isEmpty()) {
      Announce.warning("No infobox patterns found");
    }
    
    Announce.done();
    return (map);
  }
  
  public void extract(Map<Theme, FactWriter> writers,
      Map<Theme, FactSource> input) throws Exception {

// FactCollection infoboxFacts = new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS));
 FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
// Map<String, Set<String>> patterns = InfoboxExtractor.infoboxPatterns(infoboxFacts);
// PatternList replacements = new PatternList(infoboxFacts,"<_infoboxReplace>");
// Map<String, String> combinations = infoboxFacts.asStringMap("<_infoboxCombine>");
 Map<String, String> preferredMeanings = WordnetExtractor.preferredMeanings(input);
 
   Map<String, Set<String>> matchings = 
       infoboxMatchings(new FactCollection(input.get(AttributeMatcher.MATCHED_INFOBOXATTS_MAP.get(language))));

  Map<String, Set<String>> rdictionary = new HashMap<String, Set<String>>();
 
 for (Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(language))) {
   rdictionary = Dictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
   Set<String> subjects = rdictionary.get(FactComponent.stripBrackets(f.getArg(1)));
   Set<String> yagoRelations = matchings.get(f.getRelation());

   if(subjects==null) continue;
   if(yagoRelations ==null) continue;
 
   
   for (String relation : yagoRelations) {
//     if(!relation.equals( "<hasOfficialLanguage>")) continue;
     boolean inverse = f.getRelation().endsWith("->");
     String expectedDatatype = hardWiredFacts.getArg2(relation, RDFS.range);
     TermExtractor termExtractor = expectedDatatype.equals(RDFS.clss) ? new TermExtractor.ForClass(
         preferredMeanings) : TermExtractor.forType(expectedDatatype);//NIVID
     List<String> secondLangObjects = termExtractor.extractList(AttributeMatcher.preprocess(f.getArg(2))); //NIVID
//     if(subjects.size()>1){
//       System.out.println("_________________");
//       System.out.println(f.getArg(1));
//       System.out.println(subjects);
//     }
     
//     System.out.println("_________________");
//     System.out.println(f);
//     System.out.println("objects: " + secondLangObjects);
//     System.out.println("subjects: " + subjects);
//     System.out.println("yago relations: " + yagoRelations);
//     System.out.println("_________________");
     
     for(String o:secondLangObjects){
       Set<String> objects = rdictionary.get(FactComponent.stripBrackets(o));
       if(objects == null) continue;
       for(String obj :objects){
         if (inverse) {
           Fact fact = new Fact(obj, relation, subjects.toArray()[0].toString());
           write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language),
               FactComponent.wikipediaURL(subjects.toArray()[0].toString()),
               "InfoboxMapper_multi ");
         } else {
           Fact fact = new Fact(subjects.toArray()[0].toString(), relation, obj);
           write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language),
               FactComponent.wikipediaURL(subjects.toArray()[0].toString()),
               "InfoboxMapper_multi ");
         }
       }
     }
     
   }
}


}
  
  public InfoboxMapper_multi(String lang){
    super(lang);
  }

  public static void main(String[] args) throws Exception {
    InfoboxMapper_multi extractor = new InfoboxMapper_multi("de");
    extractor.extract(new File("D:/data2/yago2s/"),
        "mapping infobox attributes into infobox facts");

  }



}
