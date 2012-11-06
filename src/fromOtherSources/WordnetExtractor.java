package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Name;
import main.ParallelCaller;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;
import fromWikipedia.Extractor;

/**
 * YAGO2s - WordNetExtractor
 * 
 * Extracts facts form wordnet. 
 * 
 * @author Fabian M. Suchanek
 *
 */
public class WordnetExtractor extends Extractor {

  /** Holds the preferred meanings, as these are frequently used. Call freeMemory() to abandon them. */
  protected static Map<String, String> preferredMeaning;

  /** Folder where wordnet lives */
  protected File wordnetFolder;

  /** wordnet classes */
  public static final Theme WORDNETCLASSES = new Theme("wordnetClasses", "SubclassOf-Hierarchy from WordNet");

  /** wordnet labels/means */
  public static final Theme WORDNETWORDS = new Theme("wordnetWords", "Labels and preferred meanings form Wordnet");

  /** ids of wordnet */
  public static final Theme WORDNETIDS = new Theme("yagoWordnetIds", "Mappings of the WordNet-based YAGO classes to the ids of WordNet",
      ThemeGroup.LINK);

  /** wordnet glosses */
  public static final Theme WORDNETGLOSSES = new Theme("wordnetGlosses", "Glosses from Wordnet");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(HardExtractor.HARDWIREDFACTS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(WORDNETCLASSES, WORDNETWORDS, WORDNETIDS, WORDNETGLOSSES);
  }

  /** Pattern for synset definitions */
  // s(100001740,1,'entity',n,1,11).
  public static Pattern SYNSETPATTERN = Pattern.compile("s\\((\\d+),\\d*,'(.*)',(.),(\\d*),(\\d*)\\)\\.");

  /** Pattern for relation definitions */
  // hyp (00001740,00001740).
  public static Pattern RELATIONPATTERN = Pattern.compile("\\w*\\((\\d{9}),(.*)\\)\\.");

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    Announce.doing("Extracting from Wordnet");
    Map<String, String> hardwiredMeanings = new HashMap<>();
    addPreferredMeanings(hardwiredMeanings,new FactCollection(input.get(HardExtractor.HARDWIREDFACTS)));
    // Reset the preferred meanings cache, because they only contain the hard-wired facts 
    preferredMeaning = null;
    Collection<String> instances = new HashSet<String>(8000);
    for (String line : new FileLines(new File(wordnetFolder, "wn_ins.pl"), "Loading instances")) {
      line = line.replace("''", "'");
      Matcher m = RELATIONPATTERN.matcher(line);
      if (!m.matches()) continue;
      instances.add(m.group(1));
    }
    Map<String, String> id2class = new HashMap<String, String>(80000);
    String lastId = "";
    String lastClass = "";

    for (String line : new FileLines(new File(wordnetFolder, "wn_s.pl"), "Loading synsets")) {
      line = line.replace("''", "'"); // TODO: Does this work for
      // wordnet_child's_game_100483935 ?
      Matcher m = SYNSETPATTERN.matcher(line);
      if (!m.matches()) continue;
      String id = m.group(1);
      String word = m.group(2);
      String type = m.group(3);
      String numMeaning = m.group(4);
      if (instances.contains(id)) continue;
      // The instance list does not contain all instances...
      if (Name.couldBeName(word)) continue;
      if (!type.equals("n")) continue;
      if (!id.equals(lastId)) {
        if (id.equals("100001740")) lastClass = YAGO.entity;
        else lastClass = FactComponent.forWordnetEntity(word, id);
        id2class.put(lastId = id, lastClass);
        writers.get(WORDNETWORDS).write(new Fact(null, lastClass, "skos:prefLabel", FactComponent.forStringWithLanguage(word, "eng")));
        writers.get(WORDNETIDS).write(new Fact(null, lastClass, "<hasSynsetId>", FactComponent.forString(id)));
      }
      String wordForm = FactComponent.forStringWithLanguage(word, "eng");
      // add additional fact if it is preferred meaning
      if (numMeaning.equals("1")) {
        // First check whether we do not already have such an element
        if (hardwiredMeanings.get(word) == null && hardwiredMeanings.get(Character.toUpperCase(wordForm.charAt(0)) + wordForm.substring(1)) == null) {
          writers.get(WORDNETWORDS).write(new Fact(null, lastClass, "<isPreferredMeaningOf>", wordForm));
        }
      }
      writers.get(WORDNETWORDS).write(new Fact(null, lastClass, RDFS.label, wordForm));
    }
    instances = null;
    for (String line : new FileLines(new File(wordnetFolder, "wn_hyp.pl"), "Loading subclassOf")) {
      line = line.replace("''", "'"); // TODO: Does this work for
      // wordnet_child's_game_100483935 ?
      Matcher m = RELATIONPATTERN.matcher(line);
      if (!m.matches()) {
        continue;
      }
      String arg1 = m.group(1);
      String arg2 = m.group(2);
      if (!id2class.containsKey(arg1)) {
        continue;
      }
      if (!id2class.containsKey(arg2)) continue;
      writers.get(WORDNETCLASSES).write(new Fact(null, id2class.get(arg1), "rdfs:subClassOf", id2class.get(arg2)));
    }

    for (String line : new FileLines(new File(wordnetFolder, "wn_g.pl"), "Loading hasGloss")) {
      line = line.replace("''", "'");
      Matcher m = RELATIONPATTERN.matcher(line);
      if (!m.matches()) {
        continue;
      }
      String arg1 = m.group(1);
      String arg2 = m.group(2);
      if (!id2class.containsKey(arg1)) {
        continue;
      }

      arg2 = FactComponent.forStringWithLanguage(arg2.substring(1, arg2.length() - 1).replace('"', '\''), "eng");
      Fact fact = new Fact(null, id2class.get(arg1), "<hasGloss>", arg2);
      writers.get(WORDNETGLOSSES).write(fact);
    }
    Announce.done();
  }

  public WordnetExtractor(File wordnetFolder) {
    this.wordnetFolder = wordnetFolder;
  }

  /** Frees the memory used by the cached preferred menaings*/
  public static synchronized void freeMemory() {
    preferredMeaning = null;
  }

  /** Returns a map of Java strings to preferred YAGO entities. Requires HARDWIREDFACTS and WORDNETWORDS */
  public static Map<String, String> preferredMeanings(Map<Theme, FactSource> input) throws IOException {
    if (preferredMeaning != null) return (preferredMeaning);
    preferredMeaning = new HashMap<String, String>();
    addPreferredMeanings(preferredMeaning, new FactCollection(input.get(HardExtractor.HARDWIREDFACTS)));
    addPreferredMeanings(preferredMeaning, new FactCollection(input.get(WordnetExtractor.WORDNETWORDS)));
    if (preferredMeaning.isEmpty()) Announce.warning("No preferred meanings found");
    return (preferredMeaning);
  }

  /** Adds the preferred meanings of a file to a map*/
  public static void addPreferredMeanings(Map<String, String> preferredMeanings, FactCollection fc) {
    for (Fact fact : fc.get("<isPreferredMeaningOf>")) {
      preferredMeanings.put(fact.getArgJavaString(2), fact.getArg(1));
    }
  }
  
  public static void main(String[] args) throws Exception {
    new WordnetExtractor(new File("c:/fabian/data/wordnet")).extract(new File("c:/fabian/data/yago2s"),ParallelCaller.header);
  }
}
