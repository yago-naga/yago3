package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromThemes.RuleExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all meta facts and puts them into the right themes

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

public class MetaFactExtractor extends SimpleDeduplicator {

  @Override
  public List<Theme> inputOrdered() {
    return Arrays.asList(SchemaExtractor.YAGOSCHEMA, HardExtractor.HARDWIREDFACTS, RuleExtractor.RULERESULTS, FlightExtractor.FLIGHTS,
        //				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }

  /** All meta facts of YAGO */
  public static final Theme YAGOMETAFACTS = new Theme("yagoMetaFacts", "All temporal and geospatial meta facts of YAGO, complementing the CORE facts",
      ThemeGroup.META);

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(YAGO.extractionSource, YAGO.extractionTechnique);

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new MetaFactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }

  @Override
  public Theme myOutput() {
    return YAGOMETAFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if (fact.getRelation().startsWith("<_")) return (false);
    if (relationsExcluded.contains(fact.getRelation())) return (false);
    return (FactComponent.isFactId(fact.getArg(1)));
  }
}
