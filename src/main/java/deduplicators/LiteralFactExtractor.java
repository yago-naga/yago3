package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.CoordinateExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - LiteralFactExtractor
 * 
 * Deduplicates all facts with literals and puts them into the right themes
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

public class LiteralFactExtractor extends SimpleDeduplicator {

  @Override
  @Fact.ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<>();
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.add(HardExtractor.HARDWIREDFACTS);
    input.add(CoordinateExtractor.COORDINATES);
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(Arrays.asList(RuleExtractor.RULERESULTS,
        //				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS, GeoNamesDataImporter.GEONAMES_MAPPED_DATA));
    return input;
  }

  /** All facts of YAGO */
  public static final Theme YAGOLITERALFACTS = new Theme("yagoLiteralFacts", "All facts of YAGO that contain literals (except labels)",
      ThemeGroup.CORE);

  /** All facts of YAGO */
  public static final Theme LITERALFACTCONFLICTS = new Theme("_literalFactConflicts",
      "Literal facts that were not added because they conflicted with an existing fact");

  @Override
  public Theme conflicts() {
    return LITERALFACTCONFLICTS;
  }

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range, RDFS.subpropertyOf,
      RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>", "<hasGloss>", "<hasConfidence>",
      "<redirectedFrom>", "<wasBornOnDate>", "<diedOnDate>", "<wasCreatedOnDate>", "<wasDestroyedOnDate>", "<happenedOnDate>", "<startedOnDate>",
      "<endedOnDate>");

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new LiteralFactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }

  @Override
  public Theme myOutput() {
    return YAGOLITERALFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if (fact.getRelation().startsWith("<_")) return (false);
    if (relationsExcluded.contains(fact.getRelation())) return (false);
    return (!FactComponent.isFactId(fact.getArg(1)) && FactComponent.isLiteral(fact.getArg(2)));
  }
}
