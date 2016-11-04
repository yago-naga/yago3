package fromThemes;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.RDFS;
import deduplicators.ClassExtractor;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * Extracts all transitive rdf:type facts.
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
public class TransitiveTypeExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(ClassExtractor.YAGOTAXONOMY, CoherentTypeExtractor.YAGOTYPES);
  }

  /** All type facts */
  public static final Theme TRANSITIVETYPE = new Theme("yagoTransitiveType", "Transitive closure of all rdf:type/rdfs:subClassOf facts",
      ThemeGroup.TAXONOMY);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(TRANSITIVETYPE);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(ClassExtractor.YAGOTAXONOMY);
  }

  @Override
  public void extract() throws Exception {
    FactCollection classes = ClassExtractor.YAGOTAXONOMY.factCollection();
    Map<String, Set<String>> yagoTaxonomy = new HashMap<>();
    Announce.doing("Computing the transitive closure");
    for (Fact f : CoherentTypeExtractor.YAGOTYPES) {
      if (f.getRelation().equals(RDFS.type)) {
        D.addKeyValue(yagoTaxonomy, f.getArg(1), f.getArg(2), TreeSet.class);
        for (String c : classes.superClasses(f.getArg(2))) {
          D.addKeyValue(yagoTaxonomy, f.getArg(1), c, TreeSet.class);
        }
      }
    }
    Announce.done();
    Announce.doing("Writing data");
    for (Entry<String, Set<String>> type : yagoTaxonomy.entrySet()) {
      for (String c : type.getValue()) {
        Fact f = new Fact(type.getKey(), RDFS.type, c);
        f.makeId();
        TRANSITIVETYPE.write(f);
      }
    }
    Announce.done();
    Announce.done();
  }

  public static void main(String[] args) throws Exception {
    new TransitiveTypeExtractor().extract(new File("D:/data2/yago2s"), "test");
  }
}
