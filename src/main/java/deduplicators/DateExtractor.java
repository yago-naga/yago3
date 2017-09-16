package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import basics.Fact;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - LiteralFactExtractor
 * 
 * Deduplicates all facts with dates and puts them into the right themes
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

public class DateExtractor extends SimpleDeduplicator {

  @Override
  @Fact.ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<Theme>();
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.add(HardExtractor.HARDWIREDFACTS);
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(RuleExtractor.RULERESULTS);
    //		input.add(TemporalCategoryExtractor.TEMPORALCATEGORYFACTS);
    input.add(TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
    return input;
  }

  /** All facts of YAGO */
  public static final Theme YAGODATEFACTS = new Theme("yagoDateFacts", "All facts of YAGO that contain dates", ThemeGroup.CORE);

  /** All facts of YAGO */
  public static final Theme DATEFACTCONFLICTS = new Theme("_dateFactConflicts",
      "Date facts that were not added because they conflicted with an existing fact");

  @Override
  public Theme conflicts() {
    return DATEFACTCONFLICTS;
  }

  /** relations that we treat */
  public static final Set<String> relationsIncluded = new FinalSet<>("<wasBornOnDate>", "<diedOnDate>", "<wasCreatedOnDate>", "<wasDestroyedOnDate>",
      "<happenedOnDate>", "<startedOnDate>", "<endedOnDate>");

  @Override
  public Theme myOutput() {
    return YAGODATEFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    return (relationsIncluded.contains(fact.getRelation()));
  }

  public static void main(String[] args) throws Exception {
    //Announce.setLevel(Announce.Level.DEBUG);
    new DateExtractor().extract(new File("/san/suchanek/yago3-2017-02-20"), "test");
  }

}
