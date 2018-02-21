/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Joanna Asia Biega.

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
import java.util.Set;

import deduplicators.DateExtractor;
import deduplicators.FactExtractor;
import deduplicators.LiteralFactExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * The base representative for SPOTLX rule deduction. It first uses time&space
 * 'entity' rules for infering the more general creation and destruction facts
 * (i.e. placedIn, startsExistingOnDate and endsExistingOnDate) and then calls
 * the extractor which continues the deduction process.
 * 
*/
public class SPOTLXRuleExtractor extends BaseRuleExtractor {

  /*
   * Non follow-up extractors should not be declared as follow-up
   * 
   * public Set<FollowUpExtractor> followUp() { return (new
   * HashSet<FollowUpExtractor>( Arrays.asList(new
   * SPOTLXDeductiveExtractor(1)))); }
   */

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.SPOTLX_ENTITY_RULES, PatternHardExtractor.HARDWIREDFACTS, TransitiveTypeExtractor.TRANSITIVETYPE,
        DateExtractor.YAGODATEFACTS, FactExtractor.YAGOFACTS, LiteralFactExtractor.YAGOLITERALFACTS);
  }

  /** Themes of spotlx deductions */
  public static final Theme RULERESULTS = new Theme("spotlxEntityFacts", "SPOTLX deduced facts");

  public static final Theme RULESOURCES = new Theme("spotlxEntitySources", "SPOTLX deduced facts");

  @Override
  public Theme getRULERESULTS() {
    return RULERESULTS;
  }

  @Override
  public Theme getRULESOURCES() {
    return RULESOURCES;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(RULERESULTS, RULESOURCES);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(PatternHardExtractor.SPOTLX_ENTITY_RULES);
  }

  @Override
  public FactCollection getInputRuleCollection() throws Exception {
    // FactSource spotlxRelationRules =
    // input.get(PatternHardExtractor.SPOTLX_ENTITY_RULES);
    // FactCollection collection = new FactCollection(spotlxRelationRules);
    return PatternHardExtractor.SPOTLX_ENTITY_RULES.factCollection();
  }

  public static void main(String[] args) throws Exception {
    new PatternHardExtractor(new File("./data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    new HardExtractor(new File("../basics2s/data")).extract(new File("/home/jbiega/data/yago2s"), "test");
    Announce.setLevel(Announce.Level.DEBUG);
    new SPOTLXRuleExtractor().extract(new File("/home/jbiega/data/yago2s"), "test");
  }

}
