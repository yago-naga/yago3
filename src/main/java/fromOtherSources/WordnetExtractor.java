package fromOtherSources;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Name;
import main.ParallelCaller;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * Extracts facts form wordnet.
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/
public class WordnetExtractor extends DataExtractor {

  /** wordnet classes */
  public static final Theme WORDNETCLASSES = new Theme("wordnetClasses", "SubclassOf-Hierarchy from WordNet");

  /** wordnet labels/means */
  public static final Theme WORDNETWORDS = new Theme("wordnetWords", "Labels from Wordnet");

  /** ids of wordnet */
  public static final Theme WORDNETIDS = new Theme("yagoWordnetIds", "Mappings of the WordNet-based YAGO classes to the ids of WordNet",
      ThemeGroup.LINK);

  /** Preferred meanings */
  public static final Theme PREFMEANINGS = new Theme("yagoPreferredMeanings", "Preferred meanings of words in YAGO", ThemeGroup.LINK);

  /** wordnet glosses */
  public static final Theme WORDNETGLOSSES = new Theme("wordnetGlosses", "Glosses from Wordnet");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(HardExtractor.HARDWIREDFACTS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(WORDNETCLASSES, WORDNETWORDS, WORDNETIDS, WORDNETGLOSSES, PREFMEANINGS);
  }

  /** Pattern for synset definitions */
  // s(100001740,1,'entity',n,1,11).
  public static Pattern SYNSETPATTERN = Pattern.compile("s\\((\\d+),\\d*,'(.*)',(.),(\\d*),(\\d*)\\)\\.");

  /** Pattern for relation definitions */
  // hyp (00001740,00001740).
  public static Pattern RELATIONPATTERN = Pattern.compile("\\w*\\((\\d{9}),(.*)\\)\\.");

  @Override
  public void extract() throws Exception {
    Announce.doing("Extracting from Wordnet");
    Set<String> definedWords = new HashSet<>();
    for (Fact f : HardExtractor.HARDWIREDFACTS) {
      if (!f.getRelation().equals("<isPreferredMeaningOf>")) continue;
      PREFMEANINGS.write(f);
      definedWords.add(f.getArgJavaString(2));
    }

    Collection<String> instances = new HashSet<String>(8000);
    for (String line : new FileLines(new File(inputData, "wn_ins.pl"), "Loading instances")) {
      line = line.replace("''", "'");
      Matcher m = RELATIONPATTERN.matcher(line);
      if (!m.matches()) continue;
      instances.add(m.group(1));
    }
    Map<String, String> id2class = new HashMap<String, String>(80000);
    String lastId = "";
    String lastClass = "";

    for (String line : new FileLines(new File(inputData, "wn_s.pl"), "Loading synsets")) {
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
        WORDNETWORDS.write(new Fact(null, lastClass, "skos:prefLabel", FactComponent.forStringWithLanguage(word, "eng")));
        WORDNETIDS.write(new Fact(null, lastClass, "<hasSynsetId>", FactComponent.forString(id)));
      }
      String wordForm = FactComponent.forStringWithLanguage(word, "eng");
      // add additional fact if it is preferred meaning
      if (numMeaning.equals("1")) {
        // First check whether we do not already have such an element
        if (!definedWords.contains(word) && !definedWords.contains(Character.toUpperCase(word.charAt(0)) + word.substring(1))) {
          PREFMEANINGS.write(new Fact(null, lastClass, "<isPreferredMeaningOf>", wordForm));
        }
      }
      WORDNETWORDS.write(new Fact(null, lastClass, RDFS.label, wordForm));
    }
    instances = null;
    for (String line : new FileLines(new File(inputData, "wn_hyp.pl"), "Loading subclassOf")) {
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
      WORDNETCLASSES.write(new Fact(null, id2class.get(arg1), "rdfs:subClassOf", id2class.get(arg2)));
    }

    for (String line : new FileLines(new File(inputData, "wn_g.pl"), "Loading hasGloss")) {
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
      WORDNETGLOSSES.write(fact);
    }
    Announce.done();
  }

  public WordnetExtractor(File wordnetFolder) {
    super(wordnetFolder);
  }

  public WordnetExtractor() {
    this(new File("./data/wordnet"));
  }

  public static void main(String[] args) throws Exception {
    new WordnetExtractor().extract(new File("c:/fabian/data/yago3"), ParallelCaller.header);
  }
}
