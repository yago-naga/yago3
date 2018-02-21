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

package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import main.ParallelCaller;
import utils.Theme;
import utils.demonyms.LocationNames;

/**
 * Extracts the coherent types from previous types
 *
*/
public class LocationExtractor extends Extractor {

  public static final Theme CATEGORYLOCATIONS = new Theme("categoryLocations", "Locations extracted from categories", Theme.ThemeGroup.INTERNAL);

  public static final Theme CATEGORYLOCATIONSSOURCES = new Theme("categoryLocationsSources", "Sources for the locations extracted from categories");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<>();
    result.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    result.addAll(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    result.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    return result;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(CATEGORYLOCATIONS, CATEGORYLOCATIONSSOURCES);
  }

  @Override
  public void extract() throws Exception {
    Map<String, List<String>> catToLocations = new HashMap<>();
    Map<String, Map<String, Integer>> entityToLocToCount = new HashMap<>();

    LocationNames ln = new LocationNames();
    ln.populate();

    // count locations for entities 
    for (Theme theme : input()) {
      for (Fact f : theme) {
        Map<String, Integer> locToCount = null;
        List<String> locs = catToLocations.computeIfAbsent(f.getObject(), cat -> {
          if (cat == null || cat.contains(" descent")) return Arrays.asList();
          return ln.locations(cat);
        });

        for (String loc : locs) {
          if (locToCount == null) {
            locToCount = entityToLocToCount.computeIfAbsent(f.getSubject(), k -> new HashMap<>(1));
          }
          locToCount.merge(loc, 1, (a, b) -> a + b);
        }
      }
    }

    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (f.getRelation().equals(RDFS.type) && f.getObject().equals(YAGO.person)) {
        Map<String, Integer> locToCount = entityToLocToCount.get(f.getSubject());
        if (locToCount == null) continue;

        // output most frequent location
        int max = Collections.max(locToCount.values());
        for (String loc : locToCount.keySet()) {
          if (locToCount.get(loc) == max) {
            Fact nf = new Fact(f.getSubject(), "<livedIn>", loc);
            write(CATEGORYLOCATIONS, nf, CATEGORYLOCATIONSSOURCES, FactComponent.wikipediaURL(f.getSubject()), "LocationExtractor");
          }
        }
      }
    }

  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) args = new String[] { "../yago.ini" };
    Parameters.init(args[0]);
    File yago = Parameters.getFile("yagoFolder");
    TransitiveTypeExtractor.TRANSITIVETYPE.assignToFolder(yago);
    ParallelCaller.createWikipediaList(Parameters.getList("languages"), Parameters.getList("wikipedias"));
    new LocationExtractor().extract(yago, "test");
  }

}
