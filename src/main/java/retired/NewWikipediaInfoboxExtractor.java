package retired;

import java.io.File;
import java.util.Set;

import extractors.DataExtractor;
import fromWikipedia.InfoboxExtractor;
import utils.Theme;

/**
 * Extracts from the new wikipedia. This has to be a data extractor, because we
 * want to call it without introducing a new language.
 
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

public class NewWikipediaInfoboxExtractor extends DataExtractor {

  /** We have a small infobx extractor that does the work */
  protected final InfoboxExtractor infex;

  /** Our output theme */
  public static Theme NEWWIKIPEDIAATTRIBUTES = InfoboxExtractor.INFOBOX_ATTRIBUTES.inLanguage("new");

  @Override
  public Set<Theme> input() {
    return infex.input();
  }

  @Override
  public Set<Theme> output() {
    return infex.output();
  }

  @Override
  public Set<Theme> inputCached() {
    return infex.inputCached();
  }

  public NewWikipediaInfoboxExtractor(File input) {
    super(input);
    infex = new InfoboxExtractor("new", input);
  }

  @Override
  public void extract() throws Exception {
    infex.extract();
  }

}
