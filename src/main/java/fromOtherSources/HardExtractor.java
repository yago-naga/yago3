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
import java.util.TreeSet;

import basics.Fact;
import basics.FactSource;
import extractors.DataExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * Produces the hard-coded facts.
 * 
*/
public class HardExtractor extends DataExtractor {

  /** Our output */
  public static final Theme HARDWIREDFACTS = new Theme("hardWiredFacts", "The manually created facts of YAGO");

  @Override
  public Set<Theme> output() {
    return (new FinalSet<Theme>(HARDWIREDFACTS));
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Copying hard wired facts");
    Announce.message("Input folder is", inputData);
    for (File f : inputData.listFiles()) {
      if (f.isDirectory() || f.getName().startsWith(".")) continue;
      Announce.doing("Copying hard wired facts from", f.getName());
      for (Fact fact : FactSource.from(f)) {
        HARDWIREDFACTS.write(fact);
      }
      Announce.done();
    }
    Announce.done();
  }

  public HardExtractor(File inputFolder) {
    super(inputFolder);
    if (!inputFolder.exists()) throw new RuntimeException("Folder not found " + inputFolder);
    if (!inputFolder.isDirectory()) throw new RuntimeException("Not a folder: " + inputFolder);
  }

  public HardExtractor() {
    this(new File("schema"));
  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>();
  }

  public static void main(String[] args) throws Exception {
    new HardExtractor().extract(new File("c:/fabian/data/yago3"), "test");
  }
}
