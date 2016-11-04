package retired;

import java.util.Set;

import extractors.Extractor;
import fromThemes.InfoboxTermExtractor;
import utils.Theme;

/** Extracts the terms from the new Wikipedia
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Thomas Rebele.

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
public class NewWikipediaTermExtractor extends Extractor {

  /** we have a small term extractor who does the work */
  protected InfoboxTermExtractor it;

  public static Theme NEWWIKITERMS = InfoboxTermExtractor.INFOBOXTERMS_TOREDIRECT.inLanguage("new");

  @Override
  public Set<Theme> input() {
    return it.input();
  }

  @Override
  public Set<Theme> output() {
    return it.output();
  }

  @Override
  public void extract() throws Exception {
    it.extract();
  }

  public NewWikipediaTermExtractor() {
    super();
    it = new InfoboxTermExtractor("new");
  }
}
