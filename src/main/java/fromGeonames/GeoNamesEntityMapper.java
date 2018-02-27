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

package fromGeonames;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.DataExtractor;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * The GeoNamesEntityMapper maps geonames entities to Wikipedia entities.
 * 
 * Needs the GeoNames alternateNames.txt as input.
 * 
*/
public class GeoNamesEntityMapper extends DataExtractor {

  public static final String ENWIKI_PREFIX = "http://en.wikipedia.org/wiki/";

  /**
   * geonames entity links (need type-checking to make sure all entities are
   * present).
   */
  public static final Theme DIRTYGEONAMESENTITYIDS = new Theme("geonamesEntityIdsDirty",
      "IDs from GeoNames entities (might contain links to non-YAGO entities)", ThemeGroup.GEONAMES);

  /** geonames entity links */
  public static final Theme GEONAMESENTITYIDS = new Theme("yagoGeonamesEntityIds", "IDs from GeoNames entities", ThemeGroup.LINK);

  public static final String GEONAMES_NAMESPACE = "http://sws.geonames.org/";

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>();
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(DIRTYGEONAMESENTITYIDS);
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    return new HashSet<FollowUpExtractor>(Arrays.asList(new TypeChecker(DIRTYGEONAMESENTITYIDS, GEONAMESENTITYIDS, this)));
  }

  @Override
  public void extract() throws Exception {
    for (String line : new FileLines(inputData, "UTF-8", "Reading GeoNames Wikipedia mappings")) {
      String[] data = line.split("\t");

      String lang = data[2];
      if (lang.equals("link")) {
        // Skip non-Wikipedia link alternate names.
        String alternateName = data[3];
        if (alternateName.startsWith(ENWIKI_PREFIX)) {
          String geoEntity = FactComponent.forWikipediaURL(alternateName);
          String geoId = data[1];
          // Links missing in YAGO will be dropped by the
          // type-checker.
          DIRTYGEONAMESENTITYIDS.write(new Fact(geoEntity, RDFS.sameas, FactComponent.forUri(GEONAMES_NAMESPACE + geoId)));
        }
      }
    }
  }

  public GeoNamesEntityMapper(File alternateNames) {
    super(alternateNames);
  }

  public GeoNamesEntityMapper() {
    this(new File(Parameters.get("geonames") + "/" + "alternateNames.txt"));
  }

  public static void main(String[] args) throws Exception {
    new GeoNamesEntityMapper().extract(new File("c:/fabian/data/yago3"), "");
  }
}
