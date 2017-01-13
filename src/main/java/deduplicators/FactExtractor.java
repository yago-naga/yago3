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
import fromThemes.GenderNameExtractor;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
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

public class FactExtractor extends SimpleDeduplicator {

  @Override
  @Fact.ImplementationNote("Authoritative facts go first. Hardwired facts go first. Infoboxes go before categories")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<Theme>();
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.add(HardExtractor.HARDWIREDFACTS);
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(GenderNameExtractor.GENDERSBYCATEGORY);
    input.add(GenderNameExtractor.GENDERSBYNAME);
    input.addAll(GenderExtractor.GENDERBYPRONOUN.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(Arrays.asList(RuleExtractor.RULERESULTS, FlightExtractor.FLIGHTS, GeoNamesDataImporter.GEONAMES_MAPPED_DATA,
        //				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS));
    return input;
  }

  /** All facts of YAGO */
  public static final Theme YAGOFACTS = new Theme("yagoFacts", "All facts of YAGO that hold between instances", ThemeGroup.CORE);

  /** All facts of YAGO */
  public static final Theme FACTCONFLICTS = new Theme("_factConflicts", "Facts that were not added because they conflicted with an existing fact");

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range, RDFS.subpropertyOf,
      RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>", "<hasGloss>", "<redirectedFrom>");

  @Override
  public Theme myOutput() {
    return YAGOFACTS;
  }

  @Override
  public Theme conflicts() {
    return FACTCONFLICTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if (fact.getRelation().startsWith("<_")) return (false);
    if (relationsExcluded.contains(fact.getRelation())) return (false);
    if (FactComponent.isFactId(fact.getArg(1))) return (false);
    if (FactComponent.isLiteral(fact.getArg(2))) return (false);
    return (true);
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new FactExtractor().extract(new File("C:/fabian/data/yago3"), "test");
  }

}
