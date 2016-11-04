package fromThemes;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.HardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * Checks whether every relation has domain, range, gloss, and type
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
public class RelationChecker extends Extractor {

  /** relations that indicate a relation */
  public static final Set<String> relationsAboutRelations = new FinalSet<>(RDFS.domain, RDFS.range, RDFS.subpropertyOf);

  public static void check(FactCollection hardFacts) {
    Announce.doing("Checking relations");
    Set<String> relations = new HashSet<>();
    for (Fact f : hardFacts) {
      relations.add(f.getRelation());
      if (relationsAboutRelations.contains(f.getRelation())) relations.add(f.getArg(1));
      if (f.getRelation().equals(YAGO.hasGloss) && f.getArg(2).contains("$")) relations.add(f.getArg(1));
    }
    Announce.message(relations.size(), "relations");
    for (String relation : relations) {
      for (String req : new String[] { RDFS.domain, RDFS.range, RDFS.type, YAGO.hasGloss }) {
        if (hardFacts.getFactsWithSubjectAndRelation(relation, req).isEmpty()) Announce.warning(relation, "does not have", req);
      }
    }
    Announce.done();
  }

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
  }

  @Override
  public Set<Theme> output() {
    return new HashSet<>();
  }

  @Override
  public void extract() throws Exception {
    check(HardExtractor.HARDWIREDFACTS.factCollection());
  }

  public static void main(String[] args) throws Exception {
    new HardExtractor(new File("../basics2s/data")).extract(new File("c:/fabian/data/yago2s"), "check");
    new RelationChecker().extract(new File("c:/fabian/data/yago2s"), "check");
  }
}
