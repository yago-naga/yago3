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
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * Copies the wordnet domain to the output folder
 * 
*/
public class WordnetDomainHierarchyExtractor extends HardExtractor {

  /** Patterns of infoboxes */
  public static final Theme WORDNETDOMAINHIERARCHY = new Theme("yagoWordnetDomainHierarchy",
      "The hierarchy of WordNet Domains from http://wndomains.fbk.eu", ThemeGroup.LINK);

  @Override
  public Set<Theme> output() {
    return (new FinalSet<Theme>(WORDNETDOMAINHIERARCHY));
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Copying wordnet domain hierarchy");
    for (Fact f : FactSource.from(new File(inputData, "_wordnetDomainHierarchy.ttl"))) {
      WORDNETDOMAINHIERARCHY.write(f);
    }
    Announce.done();
  }

  public WordnetDomainHierarchyExtractor(File inputFolder) {
    super(inputFolder);
  }

  public WordnetDomainHierarchyExtractor() {
    this(new File("./data/wordnetDomains"));
  }

  public static void main(String[] args) throws Exception {
    new WordnetDomainHierarchyExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
