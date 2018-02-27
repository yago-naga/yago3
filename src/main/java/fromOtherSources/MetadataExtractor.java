/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Johannes Hoffart.

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * Extracts some metadata about YAGO itself (sources, creation time, ...)
 *
*/
public class MetadataExtractor extends Extractor {

  /** Our output */
  public static final Theme METADATAFACTS = new Theme("yagoMetadataFacts", "The metadata facts of YAGO");

  @Override
  public Set<Theme> input() {
    return new HashSet<>();
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<>(METADATAFACTS));
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Storing metadata");

    List<String> wikipedias = Parameters.getList("wikipedias");
    int wikiId = 0;
    for (String wikipedia : wikipedias) {
      File wikipediaFile = new File(wikipedia);
      String dumpName = wikipediaFile.getName();
      METADATAFACTS.write(new Fact(FactComponent.forString("WikipediaSource_" + wikiId++), "<_yagoMetadata>", FactComponent.forString(dumpName)));
    }

    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    df.setTimeZone(tz);
    String dateString = df.format(new Date());
    METADATAFACTS.write(new Fact(FactComponent.forString("CreationDate"), "<_yagoMetadata>", FactComponent.forString(dateString)));
  }
}
