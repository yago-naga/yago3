package fromThemes;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.Theme;

/**
 * Extracts the coherent types from previous types
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
public class CoherentTypeExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<Theme>();
    result.add(WordnetExtractor.WORDNETCLASSES);
    result.add(HardExtractor.HARDWIREDFACTS);
    result.add(CategoryClassExtractor.CATEGORYCLASSES);
    result.addAll(InfoboxTypeExtractor.INFOBOXTYPES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    result.addAll(CategoryTypeExtractor.CATEGORYTYPES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return result;
  }

  /** All types of YAGO */
  public static final Theme YAGOTYPES = new Theme("yagoTypes", "The coherent types extracted from different wikipedias", Theme.ThemeGroup.TAXONOMY);

  public static final Theme YAGOTYPESSOURCES = new Theme("yagoTypesSources", "Sources for the coherent types extracted from different wikipedias");

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(YAGOTYPES, YAGOTYPESSOURCES);
  }

  /** Caches the YAGO branches */
  protected Map<String, String> yagoBranches;

  /** Holds the entire class taxonomy */
  protected FactCollection subclassFacts;

  /** Holds the entire type facts */
  protected FactCollection typeFacts;

  /** Maps a type fact id to the theme where it's from */
  protected Map<String, Theme> sources;

  /** Maps a Theme to its number of type facts */
  protected IntHashMap<Theme> numTypeFacts;

  @Override
  public void extract() throws Exception {

    yagoBranches = new HashMap<String, String>();
    subclassFacts = new FactCollection();
    typeFacts = new FactCollection();
    sources = new HashMap<>();
    for (Theme theme : input()) {
      for (Fact f : theme) {
        if (f.getRelation().equals(RDFS.type)) {
          f.makeId();
          // Add only the first source
          if (!sources.containsKey(f.getId())) sources.put(f.getId(), theme);
          typeFacts.addFast(f);
        }
        if (f.getRelation().equals(RDFS.subclassOf)) subclassFacts.addFast(f);
      }
    }
    numTypeFacts = new IntHashMap<>();
    for (String currentEntity : typeFacts.getSubjects()) {
      flush(currentEntity, typeFacts.collectObjects(currentEntity, RDFS.type));
    }
    try (Writer w = FileUtils.getBufferedUTF8Writer(new File(YAGOTYPES.file().getParent(), "_typeStatistics.tsv"))) {
      for (Theme theme : numTypeFacts) {
        w.write(theme.name + "\t" + numTypeFacts.get(theme) + "\n");
      }
    }
    yagoBranches = null;
    typeFacts = null;
    subclassFacts = null;
    sources = null;
    Announce.done();
  }

  /** Returns the YAGO branch for a class */
  public String yagoBranchForClass(String arg) {
    if (yagoBranches.containsKey(arg)) return (yagoBranches.get(arg));
    String yagoBranch = SimpleTypeExtractor.yagoBranch(arg, subclassFacts);
    if (yagoBranch != null) {
      yagoBranches.put(arg, yagoBranch);
      return (yagoBranch);
    }
    return null;
  }

  /** Returns the YAGO branch for a an entity */
  public String yagoBranchForEntity(String entity, Set<String> types) {
    IntHashMap<String> branches = new IntHashMap<>();

    for (String type : types) {
      String yagoBranch = yagoBranchForClass(type);
      if (yagoBranch != null) {
        Announce.debug(entity, type, yagoBranch);
        branches.increase(yagoBranch);
        // Give higher priority to the stuff extracted from infoboxes
        if (type.startsWith("<wordnet")) branches.increase(yagoBranch);
      }
    }
    String bestSoFar = null;
    for (String candidate : branches.keys()) {
      if (bestSoFar == null || branches.get(candidate) > branches.get(bestSoFar) || branches.get(candidate) == branches.get(bestSoFar)
          && SimpleTypeExtractor.yagoBranches.indexOf(candidate) < SimpleTypeExtractor.yagoBranches.indexOf(bestSoFar))
        bestSoFar = candidate;
    }
    return (bestSoFar);
  }

  public void flush(String entity, Set<String> types) throws IOException {
    String yagoBranch = yagoBranchForEntity(entity, types);
    // Announce.debug("Branch of", entity, "is", yagoBranch);
    if (yagoBranch == null) {
      return;
    }
    for (String type : types) {
      String branch = yagoBranchForClass(type);
      if (branch == null || !branch.equals(yagoBranch)) {
        Announce.debug("Wrong branch:", type, branch, ". Expected branch:", yagoBranch);
      } else {
        Fact f = new Fact(entity, RDFS.type, type);
        f.makeId();
        Theme source = sources.get(f.getId());
        if (source == null) continue;
        numTypeFacts.increase(source);
        write(YAGOTYPES, f, YAGOTYPESSOURCES, FactComponent.wikipediaURL(entity), source.name);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    MultilingualExtractor.wikipediaLanguages = Arrays.asList("en");
    new CoherentTypeExtractor().extract(new File("c:/fabian/data/yago3"), "test");
  }
}
