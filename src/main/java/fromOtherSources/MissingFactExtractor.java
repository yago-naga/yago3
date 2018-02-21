/*
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

package fromOtherSources;

import java.io.File;
import java.util.Set;

import basics.Fact;
import basics.FactSource;
import deduplicators.DateExtractor;
import deduplicators.FactExtractor;
import deduplicators.LabelExtractor;
import deduplicators.LiteralFactExtractor;
import deduplicators.MetaFactExtractor;
import extractors.DataExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * Produces a theme that contains missing facts
 * 
*/

public class MissingFactExtractor extends DataExtractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(FactExtractor.YAGOFACTS, LiteralFactExtractor.YAGOLITERALFACTS, DateExtractor.YAGODATEFACTS, LabelExtractor.YAGOLABELS,
        MetaFactExtractor.YAGOMETAFACTS);
  }

  /** Comparison theme */
  public static final Theme MISSING_FACTS = new Theme("missingFacts",
      "Facts that were there in the previous version of YAGO and that are no longer there.");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(MISSING_FACTS);
  }

  public MissingFactExtractor(File oldYagoFolder) {
    super(oldYagoFolder);
  }

  @Override
  public void extract() throws Exception {
    for (Theme checkMe : input()) {
      Announce.doing("Checking", checkMe);
      Announce.doing("Loading old facts");
      FactCollection old = new FactCollection();
      int numFacts = 10000;
      for (Fact f : FactSource.from(checkMe.findFileInFolder(inputData))) {
        if (f.getArg(1).startsWith("<wordnet_")) continue;
        if (f.getArg(1).endsWith("_language>")) continue;
        old.add(f);
        if (numFacts-- == 0) break;
      }
      Announce.done(old.size() + " facts");
      Announce.doing("Going through new facts");
      for (Fact f : checkMe) {
        old.remove(f);
      }
      Announce.done(old.size() + " facts missing");
      MISSING_FACTS.write(new Fact(checkMe.asYagoEntity(), "rdf:type", "<follows>"));
      for (Fact f : old) {
        MISSING_FACTS.write(f);
      }
      Announce.done();
    }
  }

  public static void main(String[] args) throws Exception {
    new MissingFactExtractor(new File("c:/Fabian/temp/oldYago")).extract(new File("c:/Fabian/data/yago2s"), "test");
  }
}
