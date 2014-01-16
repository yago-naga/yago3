package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import utils.PatternList;
import utils.TermExtractor;
import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.parsers.Char;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TypeChecker;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;


public class InfoboxMapperEN extends InfoboxMapper {

  
//  /** Infobox facts */
//  public static final Theme INFOBOXFACTS_TOREDIRECT = new Theme(
//      "infoboxFactsToBeRedirected_en",
//      "Facts extracted from the Wikipedia infoboxes, redirects to be resolved");
  
  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(new Redirector(
        INFOBOXFACTS_TOREDIRECT_MAP.get(language), INFOBOXFACTS_TOTYPECHECK_MAP.get(language), this, this.language),
        new TypeChecker( INFOBOXFACTS_TOTYPECHECK_MAP.get(language), INFOBOXFACTS_MAP.get(language), this)));
  }


  /** Extracts a relation from a string */
  protected void extract(String entity, String string, String relation,
      String attribute, Map<String, String> preferredMeanings,
      FactCollection factCollection, Map<Theme, FactWriter> writers,
      PatternList replacements) throws IOException {
    string = replacements.transform(Char.decodeAmpersand(string));
    string = string.replace("$0", FactComponent.stripBrackets(entity));
    string = string.trim();
    if (string.length() == 0)
      return;

    // Check inverse
    boolean inverse;
    String expectedDatatype;
    if (relation.endsWith("->")) {
      inverse = true;
      relation = Char.cutLast(Char.cutLast(relation)) + '>';
      expectedDatatype = factCollection.getArg2(relation, RDFS.domain);
    } else {
      inverse = false;
      expectedDatatype = factCollection.getArg2(relation, RDFS.range);
    }
    if (expectedDatatype == null) {
      Announce.warning("Unknown relation to extract:", relation);
      expectedDatatype = YAGO.entity;
    }

    // Get the term extractor
    TermExtractor extractor = expectedDatatype.equals(RDFS.clss) ? new TermExtractor.ForClass(
        preferredMeanings) : TermExtractor.forType(expectedDatatype);
    String syntaxChecker = FactComponent.asJavaString(factCollection
        .getArg2(expectedDatatype, "<_hasTypeCheckPattern>"));

    // Extract all terms
    List<String> objects = extractor.extractList(string);
    for (String object : objects) {
      // Check syntax
      if (syntaxChecker != null
          && FactComponent.asJavaString(object) != null
          && !FactComponent.asJavaString(object).matches(
              syntaxChecker)) {
        Announce.debug("Extraction", object, "for", entity, relation,
            "does not match syntax check", syntaxChecker);
        continue;
      }
      // Check data type
      if (FactComponent.isLiteral(object)) {
        String parsedDatatype = FactComponent.getDatatype(object);
        if (parsedDatatype == null)
          parsedDatatype = YAGO.string;
        if (syntaxChecker != null
            && factCollection.isSubClassOf(expectedDatatype,
                parsedDatatype)) {
          // If the syntax check went through, we are fine
          object = FactComponent
              .setDataType(object, expectedDatatype);
        } else {
          // For other stuff, we check if the datatype is OK
          if (!factCollection.isSubClassOf(parsedDatatype,
              expectedDatatype)) {
            Announce.debug("Extraction", object, "for", entity,
                relation, "does not match type check",
                expectedDatatype);
            continue;
          }
        }
      }
      if (inverse) {
        Fact fact = new Fact(object, relation, entity);
        write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language),
            FactComponent.wikipediaURL(entity),
            "InfoboxExtractor from " + attribute);
      } else {
        Fact fact = new Fact(entity, relation, object);
        write(writers, INFOBOXFACTS_TOREDIRECT_MAP.get(language), fact, INFOBOXSOURCES_MAP.get(language),
            FactComponent.wikipediaURL(entity),
            "InfoboxExtractor from " + attribute);
      }
      if (factCollection.contains(relation, RDFS.type, YAGO.function))
        break;
    }
  }
  
  @Override
  public void extract(Map<Theme, FactWriter> writers,
      Map<Theme, FactSource> input) throws Exception {

//     for(Fact f : input.get(InfoboxExtractor.INFOBOXATTS)) {
//     write(writers, INFOBOXFACTS, f, INFOBOXSOURCES,
//     FactComponent.wikipediaURL(f.getArg(1)), "nicename");
//     Announce.done();
//     }

    FactCollection infoboxFacts = new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS));
    FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
    Map<String, Set<String>> patterns = InfoboxExtractor.infoboxPatterns(infoboxFacts);
    PatternList replacements = new PatternList(infoboxFacts,"<_infoboxReplace>");
    Map<String, String> combinations = infoboxFacts.asStringMap("<_infoboxCombine>");
    Map<String, String> preferredMeanings = WordnetExtractor.preferredMeanings(input);

    
    /*final version*/ 
    Map<String, Set<String>> attributes = new TreeMap<String, Set<String>>();
    String prevEntity = "";
    for (Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(language))) {
      System.out.println( "START: " + f);
      String attribute = FactComponent.stripBrackets(FactComponent.stripPrefix(f.getRelation()));
      String value = f.getArgJavaString(2);
      if (value==null) {
        continue; 
      }

      if(!f.getArg(1).equals(prevEntity) ){
        processCombinations(prevEntity,attributes, combinations,
            patterns, preferredMeanings, hardWiredFacts, writers, replacements);
        prevEntity= f.getArg(1);
        attributes.clear();
        D.addKeyValue(attributes,attribute,value, TreeSet.class);
      }else{
        D.addKeyValue(attributes,attribute,value, TreeSet.class);
      }

    }
    
    processCombinations(prevEntity,attributes, combinations,
        patterns, preferredMeanings, hardWiredFacts, writers, replacements);
    /*version without apply combinations*/
//    for (Fact f : input.get(InfoboxExtractor.INFOBOXATTS)) {
//      String attribute = FactComponent.stripBrackets(FactComponent.stripPrefix(f.getRelation()));
//      String value = f.getArgJavaString(2);
//      Set<String> relations = patterns.get(attribute);
//      if (relations == null) continue;
//      for (String relation : relations) {
//        extract(f.getArg(1), value, relation,"<"+attribute+">", preferredMeaning,hardWiredFacts, writers, replacements);
//      }
//    }
    

  }
  
  public Map<String, Set<String>> applyCombination(Map<String, Set<String>> result,  Map<String, String> combinations) {
//  Map<String, Set<String>> result = new TreeMap<String, Set<String>>();

//  D.addKeyValue(result, originalAttribute, originalValue, TreeSet.class);   

   
  // for (Fact f : input){
  // Apply combinations
   next: for (String code : combinations.keySet()) {
     StringBuilder val = new StringBuilder();

     for (String attribute : code.split(">")) {
       int scanTo = attribute.indexOf('<');
       if (scanTo != -1) {
         val.append(attribute.substring(0, scanTo));
         String attr = attribute.substring(scanTo + 1);
         // Do we want to exclude the existence of an attribute?
         if (attr.startsWith("~")) {
           attr = attr.substring(1);
           if (result.get(InfoboxExtractor
               .normalizeAttribute(attr)) != null){
             continue next;
           }
           continue;
         }
         String newVal = D.pick(result.get(InfoboxExtractor
             .normalizeAttribute(attr)));
         if (newVal == null){
           continue next;
         }
         val.append(newVal);
       } else {
         val.append(attribute);
       }
     }

     D.addKeyValue(
         result,
         InfoboxExtractor.normalizeAttribute(combinations.get(code)),
         val.toString(), TreeSet.class);
   }

  // }

  return result;
}

  private void processCombinations(String entity, Map<String, Set<String>> attributes, Map<String, String> combinations,
      Map<String, Set<String>> patterns, Map<String, String> preferredMeanings, FactCollection hardWiredFacts, 
      Map<Theme, FactWriter> writers, PatternList replacements) throws IOException{
    if(!attributes.isEmpty()){
      attributes = applyCombination(attributes,combinations);
      
      for (String mappedattribute : attributes.keySet()) {

        Set<String> relations = patterns.get(mappedattribute);
     
        if (relations == null) continue;
        for (String mappedvalue : attributes.get(mappedattribute)) {
          for (String relation : relations) {
            extract(entity, mappedvalue, relation, mappedattribute, preferredMeanings,hardWiredFacts, writers, replacements);
          }
        }
      }
    }
  }
  public InfoboxMapperEN(){
    super("en");
  }
  
  
  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
//     C:\Users\Administrator\Dropbox\workspace\yago2s
//     File f1=new
//     File("c:/Users/Administrator/Dropbox/workspace/yago2s/data");
//     File f2=new File("c:/data/yago2s");
//     new PatternHardExtractor(f1).extract(f2, "test");
//     new HardExtractor(new
//     File("c:/Users/Administrator/Dropbox/workspace/basics2s/data")).extract(new
//     File("c:/data/yago2s"), "test");
//     new WordnetExtractor(new File("./data/wordnet")).extract(new
//     File("c:/data/yago2s"), "This time its gonna work!");
//     new InfoboxExtractor(new
//     File("C:/Users/Administrator/data2/wikipedia/testset/wikitest.xml"))
//     .extract(new File("C:/Users/Administrator/data2/yago2s/"+mylang),
//     "Test on 1 wikipedia article");
//    InfoboxMapperEN extractor = new InfoboxMapperEN();
//    extractor.extract(new File("D:/data2/yago2s/"),
//        "mapping infobox attributes into infobox facts");
//      InfoboxMapperEN extractor = new InfoboxMapperEN();
//      extractor.extract(new File("/home/jbiega/data/yago2s/"),
//          "mapping infobox attributes into infobox facts");
//    
//      for (Extractor e : extractor.followUp()) {
//    	  e.extract(new File("/home/jbiega/data/yago2s/"), "test");
//      }
    
  }
}
