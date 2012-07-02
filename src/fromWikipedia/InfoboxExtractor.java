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


import utils.PatternList;
import utils.TermExtractor;
import utils.TitleExtractor;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TypeChecker;

public class InfoboxExtractor extends Extractor {

  /** Input file */
  protected File wikipedia;

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.INFOBOXPATTERNS, PatternHardExtractor.TITLEPATTERNS, 
        HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETWORDS));
  }

  @Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(new Redirector(INFOBOXFACTS_TOREDIRECT, INFOBOXFACTS_TOTYPECHECK), new TypeChecker(
        INFOBOXFACTS_TOTYPECHECK, INFOBOXFACTS)));
  }

  /** Infobox facts */
  public static final Theme INFOBOXFACTS = new Theme("infoboxFacts",
      "Facts extracted from the Wikipedia infoboxes, type-checked and with redirects resolved");

  /** Infobox facts */
  public static final Theme INFOBOXFACTS_TOREDIRECT = new Theme("infoboxFactsToBeRedirected",
      "Facts extracted from the Wikipedia infoboxes, redirects to be resolved");

  /** Infobox facts */
  public static final Theme INFOBOXFACTS_TOTYPECHECK = new Theme("infoboxFactsToBeTypeChecked",
      "Facts extracted from the Wikipedia infoboxes, redirects resolved, to be type checked");

  /** Infobox sources */
  public static final Theme INFOBOXSOURCES = new Theme("infoboxSources", "Source information for the facts extracted from the Wikipedia infoboxes");

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(INFOBOXFACTS_TOREDIRECT, INFOBOXSOURCES);
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
    String cls;
    if (relation.endsWith("->")) {
      inverse = true;
      relation = Char.cutLast(Char.cutLast(relation)) + '>';
      cls = factCollection.getArg2(relation, RDFS.domain);
    } else {
      inverse = false;
      cls = factCollection.getArg2(relation, RDFS.range);
    }
    if (cls == null) {
      Announce.warning("Unknown relation to extract:", relation);
      cls = YAGO.entity;
    }

    // Get the term extractor
    TermExtractor extractor = cls.equals(RDFS.clss) ? new TermExtractor.ForClass(preferredMeanings) : TermExtractor.forType(cls);
    String syntaxChecker = FactComponent.asJavaString(factCollection.getArg2(cls, "<_hasTypeCheckPattern>"));

    // Extract all terms
    List<String> objects = extractor.extractList(string);
    for (String object : objects) {
      // Check syntax
      if (syntaxChecker != null && !FactComponent.asJavaString(object).matches(syntaxChecker)) {
        Announce.debug("Extraction", object, "for", entity, relation, "does not match syntax check", syntaxChecker);
        continue;
      }
      // Check data type
      if (FactComponent.isLiteral(object)) {
        String datatype = FactComponent.getDatatype(object);
        if (datatype == null) datatype = YAGO.string;
        if (!factCollection.isSubClassOf(datatype, cls)) {
          if (!cls.equals(YAGO.languageString)) object = FactComponent.setDataType(object, cls);
        }
      }
      if (inverse) {
        Fact fact = new Fact(object, relation, entity);
        write(writers, INFOBOXFACTS_TOREDIRECT, fact, INFOBOXSOURCES, FactComponent.wikipediaURL(entity), "InfoboxExtractor from " + attribute);
      } else {
        Fact fact = new Fact(entity, relation, object);
        write(writers, INFOBOXFACTS_TOREDIRECT, fact, INFOBOXSOURCES, FactComponent.wikipediaURL(entity), "InfoboxExtractor from " + attribute);
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
      if (!valueStr.isEmpty()) D.addKeyValue(result, attribute, Char.decodeAmpersand(valueStr), TreeSet.class);
      if (c == '}' || c == -1 || c == -2) break;
    }
    // Apply combinations
    next: for (String code : combinations.keySet()) {
      StringBuilder val = new StringBuilder();
      for (String attribute : code.split(">")) {
        int scanTo = attribute.indexOf('<');
        if (scanTo != -1) {
          val.append(attribute.substring(0, scanTo));
          String newVal = D.pick(result.get(normalizeAttribute(attribute.substring(scanTo + 1))));
          if (newVal == null) continue next;
          val.append(newVal);
        } else {
          val.append(attribute);
        }
      }
      D.addKeyValue(result, normalizeAttribute(combinations.get(code)), val.toString(), TreeSet.class);
    }
    return (result);
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    FactCollection infoboxFacts = new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS));
    FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
    Map<String, Set<String>> patterns = infoboxPatterns(infoboxFacts);
    PatternList replacements = new PatternList(infoboxFacts, "<_infoboxReplace>");
    Map<String, String> combinations = infoboxFacts.asStringMap("<_infoboxCombine>");
    Map<String, String> preferredMeaning = WordnetExtractor.preferredMeanings(hardWiredFacts,
        new FactCollection(input.get(WordnetExtractor.WORDNETWORDS)));
    TitleExtractor titleExtractor = new TitleExtractor(input);

    // Extract the information
    Announce.progressStart("Extracting", 4_500_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "{{Infobox", "{{ Infobox")) {
        case -1:
          Announce.progressDone();
          in.close();
          return;
        case 0:
          Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        default:
          if (titleEntity == null) continue;
          FileLines.readTo(in, '}', '|');
          Map<String, Set<String>> attributes = readInfobox(in, combinations);
          for (String attribute : attributes.keySet()) {
            Set<String> relations = patterns.get(attribute);
            if (relations == null) continue;
            for (String relation : relations) {
              for (String value : attributes.get(attribute)) {
                extract(titleEntity, value, relation, attribute, preferredMeaning, hardWiredFacts, writers, replacements);
              }
            }
          }
      }
    }
  }

  /** returns the infobox patterns */
  public static Map<String, Set<String>> infoboxPatterns(FactCollection infoboxFacts) {
    Map<String, Set<String>> patterns = new HashMap<String, Set<String>>();
    Announce.doing("Compiling infobox patterns");
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
  public InfoboxExtractor(File wikipedia) {
    this.wikipedia = wikipedia;
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "test");
    new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "test");
    new InfoboxExtractor(new File("c:/fabian/temp/economy.xml")).extract(new File("c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
    // new InfoboxExtractor(new
    // File("./testCases/wikitest.xml")).extract(new
    // File("/Users/Fabian/Fabian/work/yago2/newfacts"), "test");
  }
}
