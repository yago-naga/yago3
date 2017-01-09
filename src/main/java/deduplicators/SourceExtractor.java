package deduplicators;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesClassMapper;
import fromOtherSources.WikidataLabelExtractor;
import fromOtherSources.WordnetDomainExtractor;
import fromThemes.CategoryMapper;
import fromThemes.CategoryTypeExtractor;
import fromThemes.GenderNameExtractor;
import fromThemes.InfoboxMapper;
import fromThemes.InfoboxTypeExtractor;
import fromThemes.PersonNameExtractor;
import fromThemes.RuleExtractor;
import fromWikipedia.CoordinateExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - SourceExtractor
 * 
 * Deduplicates all source facts. This extractor is different from FactExtractor
 * so that it can run in parallel.
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

public class SourceExtractor extends Extractor {

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<Theme>(
        Arrays.asList(PersonNameExtractor.PERSONNAMESOURCES, RuleExtractor.RULESOURCES, WikidataLabelExtractor.WIKIPEDIALABELSOURCES,
            WikidataLabelExtractor.WIKIDATAMULTILABELSOURCES, GeoNamesClassMapper.GEONAMESSOURCES, FlightExtractor.FLIGHTSOURCE,
            GenderNameExtractor.GENDERSOURCES, WordnetDomainExtractor.WORDNETDOMAINSOURCES, TemporalInfoboxExtractor.TEMPORALINFOBOXSOURCES));
    input.add(CoordinateExtractor.COORDINATE_SOURCES);
    input.addAll(GenderExtractor.GENDERBYPRONOUNSOURCES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(InfoboxMapper.INFOBOXSOURCES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(InfoboxTypeExtractor.INFOBOXTYPESOURCES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryMapper.CATEGORYSOURCES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryTypeExtractor.CATEGORYTYPESOURCES.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return input;
  }

  /** All source facts of YAGO */
  public static final Theme YAGOSOURCES = new Theme("yagoSources", "All sources of YAGO facts", ThemeGroup.META);

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(YAGOSOURCES);
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Extracting sources");
    for (Theme theme : input()) {
      Announce.doing("Extracting sources from", theme);
      for (Fact fact : theme) {
        if (fact.getRelation().equals(YAGO.extractionSource) || fact.getRelation().equals(YAGO.extractionTechnique)) {
          YAGOSOURCES.write(fact);
        }
      }
      Announce.done();
    }
    Announce.done();
  }
}
