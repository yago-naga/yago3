package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import deduplicators.ClassExtractor;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * Produces a simplified taxonomy of just 3 layers.
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

public class SimpleTypeExtractor extends Extractor {

  /** Branches of YAGO, order matters! */
  public static final List<String> yagoBranches = Arrays.asList(YAGO.person, YAGO.organization, YAGO.building, YAGO.location, YAGO.artifact,
      YAGO.abstraction, YAGO.physicalEntity);

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CoherentTypeExtractor.YAGOTYPES, ClassExtractor.YAGOTAXONOMY);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(ClassExtractor.YAGOTAXONOMY);
  }

  /** The theme of simple types */
  public static final Theme SIMPLETYPES = new Theme("yagoSimpleTypes",
      "A simplified rdf:type system. This theme contains all instances, and links them with rdf:type facts to the leaf level of WordNet (use with yagoSimpleTaxonomy)",
      Theme.ThemeGroup.SIMPLETAX);

  /** Simple taxonomy */
  public static final Theme SIMPLETAXONOMY = new Theme("yagoSimpleTaxonomy",
      "A simplified rdfs:subClassOf taxonomy. This taxonomy contains just WordNet leaves, the main YAGO branches, and " + YAGO.entity + " (use with "
          + SIMPLETYPES + ")",
      ThemeGroup.SIMPLETAX);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(SIMPLETYPES, SIMPLETAXONOMY);
  }

  @Override
  public void extract() throws Exception {
    FactCollection types = new FactCollection();
    FactCollection taxonomy = ClassExtractor.YAGOTAXONOMY.factCollection();
    Set<String> leafClasses = new HashSet<>();
    Announce.doing("Loading YAGO types");
    for (Fact f : CoherentTypeExtractor.YAGOTYPES) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      String clss = f.getArg(2);
      if (clss.startsWith("<wikicategory")) clss = taxonomy.getObject(clss, RDFS.subclassOf);
      leafClasses.add(clss);
      types.add(new Fact(f.getArg(1), RDFS.type, clss));
    }
    Announce.done();

    Announce.doing("Writing types");
    for (Fact f : types) {
      SIMPLETYPES.write(f);
    }
    Announce.done();
    types = null;

    Announce.doing("Writing classes");
    for (String branch : yagoBranches) {
      SIMPLETAXONOMY.write(new Fact(branch, RDFS.subclassOf, YAGO.entity));
      for (String branch2 : yagoBranches) {
        if (branch != branch2) {
          SIMPLETAXONOMY.write(new Fact(branch, RDFS.disjoint, branch2));
        }
      }
    }
    for (String clss : leafClasses) {
      String branch = yagoBranch(clss, taxonomy);
      if (branch == null) {
        // Announce.warning("No branch for", clss);
      } else {
        SIMPLETAXONOMY.write(new Fact(clss, RDFS.subclassOf, branch));
      }
    }
    Announce.done();

  }

  /** returns the super-branch that this class belongs to */
  public static String yagoBranch(String clss, FactCollection taxonomy) {
    Set<String> supr = taxonomy.superClasses(clss);
    for (String b : yagoBranches) {
      if (supr.contains(b)) return (b);
    }
    return (null);
  }

  public static void main(String[] args) throws Exception {
    new SimpleTypeExtractor().extract(new File("D:/data2/yago2s"), "test\n");
  }

}
