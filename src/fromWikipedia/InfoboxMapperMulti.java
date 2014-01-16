package fromWikipedia;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javatools.administrative.Announce;
import javatools.administrative.D;
import utils.TermExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.WordnetExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;

/**
 * Class InfoboxMapper - YAGO2S
 * 
 * Maps the facts in the output of InfoboxExtractor 
 * for English.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxMapperMulti extends InfoboxMapper{
  
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

    FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
    Map<String, String> preferredMeanings = WordnetExtractor.preferredMeanings(input);

    Map<String, Set<String>> matchings = 
        infoboxMatchings(new FactCollection(input.get(AttributeMatcher.MATCHED_INFOBOXATTS_MAP.get(language))));

    Map<String, String> rdictionary = new HashMap<String, String>();

    for (Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(language))) {
      rdictionary = InterLanguageLinksDictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
      String subjects = rdictionary.get(FactComponent.stripBrackets(f.getArg(1)));
      Set<String> yagoRelations = matchings.get(f.getRelation());

      if(subjects==null) continue;
      if(yagoRelations ==null) continue;


      for (String relation : yagoRelations) {
        boolean inverse = f.getRelation().endsWith("->");
        String expectedDatatype = hardWiredFacts.getArg2(relation, RDFS.range);
        TermExtractor termExtractor = expectedDatatype.equals(RDFS.clss) ? new TermExtractor.ForClass(
            preferredMeanings) : TermExtractor.forType(expectedDatatype);//NIVID
            List<String> secondLangObjects = termExtractor.extractList(AttributeMatcher.preprocess(f.getArg(2))); //NIVID

            for(String o:secondLangObjects){
              String object = rdictionary.get(FactComponent.stripBrackets(o));
              if(object == null) continue;
              if (inverse) {
                Fact fact = new Fact(object, relation, subjects.toString());
                write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language),
                    FactComponent.wikipediaURL(subjects.toString()),
                    "InfoboxMapperMulti");
              } else {
                Fact fact = new Fact(subjects.toString(), relation, object);
                write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language),
                    FactComponent.wikipediaURL(subjects.toString()),
                    "InfoboxMapperMulti");
              }
            }
      }
    }

}
  
  public InfoboxMapperMulti(String lang){
    super(lang);
  }

  public static void main(String[] args) throws Exception {
    InfoboxMapperMulti extractor = new InfoboxMapperMulti("de");
    extractor.extract(new File("D:/data2/yago2s/"),
        "mapping infobox attributes into infobox facts");
  }

}
