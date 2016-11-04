package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import deduplicators.ClassExtractor;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.Theme;

/**
 * Generates the results of rules.
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
public class RuleExtractor extends BaseRuleExtractor {

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<Theme>(Arrays.asList(PatternHardExtractor.RULES, TransitiveTypeExtractor.TRANSITIVETYPE,
        ClassExtractor.YAGOTAXONOMY, HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETCLASSES));
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return input;
  }

  /** Theme of deductions */
  public static final Theme RULERESULTS = new Theme("ruleResults", "Results of rule applications");

  /** Theme of sources deductions */
  public static final Theme RULESOURCES = new Theme("ruleSources", "Source information for results of rule applications");

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

  /** Extract rule collection from fact sources */
  @Override
  public FactCollection getInputRuleCollection() throws Exception {
    FactCollection collection = new FactCollection(PatternHardExtractor.RULES);
    return collection;
  }

  public static void main(String[] args) throws Exception {
    new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "test");
    Announce.setLevel(Announce.Level.DEBUG);
    new RuleExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
