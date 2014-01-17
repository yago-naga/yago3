package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import utils.PatternList;
import utils.TermExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.AttributeRedirector;

/**
 * YAGO2s - InfoboxExtractor
 * 
 * This version, extracts facts from infoboxes, no matter in which language.
 * 
 * @author Fabian M. Suchanek
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxExtractor extends Extractor {

  /** Input file */
  protected File wikipedia;

  private String language;

  public static final HashMap<String, Theme> INFOBOXATTS_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> INFOBOXATTS_REDIRECTED_MAP = new HashMap<String, Theme>();

  public static final HashMap<String, Theme> INFOBOXATTSOURCES_MAP = new HashMap<String, Theme>();
  
  public static final HashMap<String, Theme> INFOBOXTYPES_MAP = new HashMap<String, Theme>();
  

 
  static {
    for (String s : Extractor.languages) {
      INFOBOXATTS_MAP.put(s, new Theme("yagoInfoboxAttributes" +  Extractor.langPostfixes.get(s), "Facts of infobox", ThemeGroup.OTHER));
      INFOBOXATTS_REDIRECTED_MAP.put(s, new Theme("infoboxAttributesRedirected" +  Extractor.langPostfixes.get(s), "Redirected facts of infobox", ThemeGroup.OTHER));
      INFOBOXATTSOURCES_MAP.put(s, new Theme("yagoInfoboxAttSources" +  Extractor.langPostfixes.get(s), "Sources for facts of infobox", ThemeGroup.OTHER));
      INFOBOXTYPES_MAP.put(s, new Theme("infoboxTypes" +  Extractor.langPostfixes.get(s), "Types of infoboxes", ThemeGroup.OTHER));
    }

  }

  static String[] getAllLangs() {
    return languages;
  }

  public String getLang() {
    return language;
  }

  @Override
  public File inputDataFile() {
    return wikipedia;
  }

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.INFOBOXPATTERNS, PatternHardExtractor.TITLEPATTERNS, HardExtractor.HARDWIREDFACTS,
        WordnetExtractor.WORDNETWORDS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>( INFOBOXATTS_MAP.get(language), INFOBOXATTSOURCES_MAP.get(language), INFOBOXTYPES_MAP.get(language));
  }
  
  @Override
  public Set<Extractor> followUp() {
	if (this.language.equals("en")) {
	  return new HashSet<Extractor> (Arrays.asList(new InfoboxMapperEN()));
	} else {
		return new HashSet<Extractor> (Arrays.asList(
				new AttributeRedirector(INFOBOXATTS_MAP.get(this.language), INFOBOXATTS_REDIRECTED_MAP.get(this.language), this.language),
				new AttributeMatcher(this.language)));
	}
  }

  /** normalizes an attribute name */
  public static String normalizeAttribute(String a) {
    return (a.trim().toLowerCase().replace("_", "").replace(" ", "").replace("-", "").replaceAll("\\d", ""));
  }

  /** Extracts a relation from a string */
  protected void extract(String entity, String string, String relation, String attribute, Map<String, String> preferredMeanings,
      FactCollection factCollection, Map<Theme, FactWriter> writers, PatternList replacements) throws IOException {

    string = replacements.transform(Char.decodeAmpersand(string));
    string = string.replace("$0", FactComponent.stripBrackets(entity));

    string = string.trim();
    if (string.length() == 0) return;

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
    TermExtractor extractor = expectedDatatype.equals(RDFS.clss) ? new TermExtractor.ForClass(preferredMeanings) : TermExtractor
        .forType(expectedDatatype);
    String syntaxChecker = FactComponent.asJavaString(factCollection.getArg2(expectedDatatype, "<_hasTypeCheckPattern>"));

    // Extract all terms
    List<String> objects = extractor.extractList(string);
    for (String object : objects) {
      // Check syntax
      if (syntaxChecker != null && FactComponent.asJavaString(object) != null && !FactComponent.asJavaString(object).matches(syntaxChecker)) {
        Announce.debug("Extraction", object, "for", entity, relation, "does not match syntax check", syntaxChecker);
        continue;
      }
      // Check data type
      if (FactComponent.isLiteral(object)) {
        String parsedDatatype = FactComponent.getDatatype(object);
        if (parsedDatatype == null) parsedDatatype = YAGO.string;
        if (syntaxChecker != null && factCollection.isSubClassOf(expectedDatatype, parsedDatatype)) {
          // If the syntax check went through, we are fine
          object = FactComponent.setDataType(object, expectedDatatype);
        } else {
          // For other stuff, we check if the datatype is OK
          if (!factCollection.isSubClassOf(parsedDatatype, expectedDatatype)) {
            Announce.debug("Extraction", object, "for", entity, relation, "does not match type check", expectedDatatype);
            continue;
          }
        }
      }
      if (inverse) {
        Fact fact = new Fact(object, relation, entity);
        write(writers,  INFOBOXATTS_MAP.get(language), fact, INFOBOXATTSOURCES_MAP.get(language), FactComponent.wikipediaURL(entity), "InfoboxExtractor from " + attribute);
      } else {
        Fact fact = new Fact(entity, relation, object);
        write(writers, INFOBOXATTS_MAP.get(language), fact, INFOBOXATTSOURCES_MAP.get(language), FactComponent.wikipediaURL(entity), "InfoboxExtractor from " + attribute);
      }

      if (factCollection.contains(relation, RDFS.type, YAGO.function)) break;
    }
  }

  /** reads an environment, returns the char on which we finish */
  public static int readEnvironment(Reader in, StringBuilder b) throws IOException {
    final int MAX = 4000;
    while (true) {
      if (b.length() > MAX) return (-2);
      int c;
      switch (c = in.read()) {
        case -1:
          return (-1);
        case '}':
          return ('}');
        case '{':
          while (c != -1 && c != '}') {
            b.append((char) c);
            c = readEnvironment(in, b);
            if (c == -2) return (-2);
          }
          b.append("}");
          break;
        case '[':
          while (c != -1 && c != ']') {
            b.append((char) c);
            c = readEnvironment(in, b);
            if (c == -2) return (-2);
          }
          b.append("]");
          break;
        case ']':
          return (']');
        case '|':
          return ('|');
        default:
          b.append((char) c);
      }
    }
  }

  /** reads an infobox */
  public static Map<String, Set<String>> readInfobox(Reader in, Map<String, String> combinations) throws IOException {
    Map<String, Set<String>> result = new TreeMap<String, Set<String>>();

    while (true) {
      String attribute = normalizeAttribute(FileLines.readTo(in, '=', '}').toString());
      if (attribute.length() == 0) return (result);
      StringBuilder value = new StringBuilder();
      int c = readEnvironment(in, value);
      String valueStr = value.toString().trim();
      if (!valueStr.isEmpty()) {
        if(attribute.contains("|")){
          String[] parts= attribute.split("\\|");
          if(parts.length>0)
            attribute  = parts[parts.length-1];
        }

        D.addKeyValue(result, attribute, Char.decodeAmpersand(valueStr), TreeSet.class);
      }
      if (c == '}' || c == -1 || c == -2) break;
    }
    // Apply combinations

    //    next: for (String code : combinations.keySet()) {
    //      StringBuilder val = new StringBuilder();
    //      for (String attribute : code.split(">")) {
    //        int scanTo = attribute.indexOf('<');
    //        if (scanTo != -1) {
    //          val.append(attribute.substring(0, scanTo));
    //          String attr = attribute.substring(scanTo + 1);
    //          // Do we want to exclude the existence of an attribute?
    //          if (attr.startsWith("~")) {
    //            attr = attr.substring(1);
    //            if (result.get(normalizeAttribute(attr)) != null) continue next;
    //            continue;
    //          }
    //          String newVal = D.pick(result.get(normalizeAttribute(attr)));
    //          if (newVal == null) continue next;
    //          val.append(newVal);
    //        } else {
    //          val.append(attribute);
    //        }
    //      }
    //      
    //      D.addKeyValue(result, normalizeAttribute(combinations.get(code)), val.toString(), TreeSet.class);
    //    }
    return (result);
  }

  public String addPrefix(String relation) {
    // return relation;
    return "<infobox/" + this.language + "/" + FactComponent.stripBrackets(relation) + ">";
  }

  private boolean comesFirst(Reader temp, String start,String end, String...findMe) throws IOException{
    String between = FileLines.readBetween(temp, start, end);
    for(String f:findMe)
      if(between.contains(f))
        return true;
    
    return false;
  }
  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactCollection infoboxFacts = new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS));
    FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
    Map<String, Set<String>> patterns = infoboxPatterns(infoboxFacts);
    PatternList replacements = new PatternList(infoboxFacts, "<_infoboxReplace>");
    Map<String, String> combinations = infoboxFacts.asStringMap("<_infoboxCombine>");
    Map<String, String> preferredMeaning = WordnetExtractor.preferredMeanings(input);
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    Announce.progressStart("Extracting", 4_500_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
        /*nested comments not supported*/
      switch (FileLines.findIgnoreCase(in, "<title>", "{{Infobox", "{{ Infobox", "<comment>")) {
        case -1:
          Announce.progressDone();
          in.close();
          return;
        case 0:
          Announce.progressStep();
          if (this.language.equals("en")) titleEntity = titleExtractor.getTitleEntity(in);
          else titleEntity = titleExtractor.getTitleEntityWithoutWordnet(in);
          break;   
        case 3:
          String s = FileLines.readToBoundary(in, "</comment>");
          break;
        default:
           if (titleEntity == null) continue;
 String cls = FileLines.readTo(in, '}', '|').toString().trim().toLowerCase();
          
          if (Character.isDigit(Char.last(cls))) cls = Char.cutLast(cls);
        
//          if(writers==null)
     
//       write(writers, INFOBOXTYPES_MAP.get(language), new Fact(titleEntity, "<hasInfoboxType>", FactComponent.forYagoEntity(cls)),
//          
//           INFOBOXATTSOURCES_MAP.get(language), "", "");
       
       writers.get(INFOBOXTYPES_MAP.get(language)).write( new Fact(titleEntity, "rdf:type", FactComponent.forYagoEntity(cls)));
       
          
          Map<String, Set<String>> attributes = readInfobox(in, combinations);

          /*new version*/
          for (String attribute : attributes.keySet()) {
            for (String value : attributes.get(attribute)) {
              write(writers, INFOBOXATTS_MAP.get(language), new Fact(titleEntity, addPrefix(FactComponent.forYagoEntity(attribute)), FactComponent.forString(value)),
                  INFOBOXATTSOURCES_MAP.get(language), FactComponent.wikipediaURL(titleEntity), "Infobox Extractor");
            }
          }

          //<Milan>	<hasLatitude>	"45.45"^^<degrees> .
          /*previous version*/
          //    		for (String attribute : attributes.keySet()) {
          //    			Set<String> relations = patterns.get(attribute);
          //    			if (relations == null) continue;
          //    			for (String value : attributes.get(attribute)) {
          //    				for (String relation : relations) {
          //    					if(relation.equals("<hasLatitude>") && titleEntity.equals("<Milan>")){}
          //    					extract(titleEntity, value, relation, attribute, preferredMeaning, hardWiredFacts, writers, replacements);
          //    				}
          //    			}
          //    		}
      }
    }
  }

  /** returns the infobox patterns */
  public static Map<String, Set<String>> infoboxPatterns(FactCollection infoboxFacts) {
    Map<String, Set<String>> patterns = new HashMap<String, Set<String>>();
    Announce.doing("Compiling infobox patterns");
    //grabbing those with relation equal to "<_infoboxPattern>"
    for (Fact fact : infoboxFacts.get("<_infoboxPattern>")) {
      D.addKeyValue(patterns, normalizeAttribute(fact.getArgJavaString(1)), fact.getArg(2), TreeSet.class);
    }
    if (patterns.isEmpty()) {
      Announce.warning("No infobox patterns found");
    }
    Announce.done();
    return (patterns);
  }

  /** Constructor from source file */
  public InfoboxExtractor(File wikipedia, String lang) {
    this.wikipedia = wikipedia;
    this.language = lang;

  }

  public InfoboxExtractor(File wikipedia) {
    this(wikipedia, decodeLang(wikipedia.getName()));
  }

  public static void main(String[] args) throws Exception {
    //    Announce.setLevel(Announce.Level.DEBUG);
    //    new PatternHardExtractor(new File("C:/Users/Administrator/data")).extract(new File("C:/Users/Administrator/data2/yago2s/"), "test");
    //    new HardExtractor(new File("C:/Users/Administrator/Dropbox/workspace/basics2s/data/")).extract(new File("C:/Users/Administrator/data2/yago2s/"), "test");
    //    new WordnetExtractor(new File("C:/Users/Administrator/data/wordnet")).extract(new File("C:/Users/Administrator/data2/yago2s/"), "This time its gonna work!");
    //    
//    InfoboxExtractor ie1 = new InfoboxExtractor(new File("D:/en_wikitest.xml"));
//    ie1.extract(new File("D:/data2/yago2s/"), "Test on 1 wikipedia article");

//    InfoboxExtractor ie2 = new InfoboxExtractor(new File("D:/ar_wiki.xml"));
//    ie2.extract(new File("D:/data2/yago2s/"), "Test on 1 wikipedia article");
//    
//    InfoboxExtractor ie3 = new InfoboxExtractor(new File("D:/de_wikitest.xml"));
//    ie3.extract(new File("D:/data2/yago2s/"), "Test on 1 wikipedia article");
	  
//    InfoboxExtractor ie3 = new InfoboxExtractor(new File("/home/jbiega/Downloads/wiki.xml"));
//    ie3.extract(new File("/home/jbiega/data/yago2s/"), "Test on 1 wikipedia article");
    
//    InfoboxExtractor ie4 = new InfoboxExtractor(new File("D:/fa_wiki.xml"));
//    ie4.extract(new File("D:/data2/yago2s/"), "Test on 1 wikipedia article");

  }
}