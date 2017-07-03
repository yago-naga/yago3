package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.TemporalInfoboxExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.FactCollection;
import utils.FactCollection.Add;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - LiteralFactExtractor
 * 
 * Deduplicates all facts with dates and puts them into the right themes
 * Dates from categories have a higher precision in general, but only contain the year.
 * So this class only takes dates from the infobox, if they agree with those from the categories.
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

public class DateExtractor extends Extractor {

  @Fact.ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
  public List<Theme> inputOrdered() {
    List<Theme> input = new ArrayList<Theme>();
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.add(HardExtractor.HARDWIREDFACTS);
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(Arrays.asList("en")));
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(Arrays.asList("en")));
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

  /** relations that we treat */
  public static final Set<String> relationsIncluded = new FinalSet<>("<wasBornOnDate>", "<diedOnDate>", "<wasCreatedOnDate>", "<wasDestroyedOnDate>",
      "<happenedOnDate>", "<startedOnDate>", "<endedOnDate>");

  public boolean isMyRelation(Fact fact) {
    return (relationsIncluded.contains(fact.getRelation()));
  }

  @Override
  public final Set<Theme> output() {
    return (new FinalSet<>(YAGODATEFACTS, DATEFACTCONFLICTS));
  }

  /**
   * Returns just the inputOrdered() to satisfy Extractor.input(). Do not
   * implement this, implement rather inputOrdered.
   */
  @Override
  public final Set<Theme> input() {
    Set<Theme> result = new HashSet<Theme>(inputOrdered());
    result.add(PatternHardExtractor.FALSEFACTS);
    return (result);
  };

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(SchemaExtractor.YAGOSCHEMA);
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Running", this.getClass().getSimpleName());
    Set<String> functions = null;
    functions = SchemaExtractor.YAGOSCHEMA.factCollection().seekSubjects(RDFS.type, YAGO.function);
    functions.addAll(SchemaExtractor.YAGOSCHEMA.factCollection().seekSubjects(RDFS.type, YAGO.functionInTime));

    Announce.doing("Loading");
    FactCollection categoryFacts = new FactCollection(), infoboxFacts = new FactCollection();

    // add category facts first
    for (Theme theme : CategoryMapper.CATEGORYFACTS.inLanguages(Arrays.asList("en"))) {
      if (!theme.isAvailableForReading()) continue;
      Announce.doing("Loading from", theme);
      for (Fact fact : theme) {
        if (isMyRelation(fact)) {
          if (categoryFacts.add(fact, functions) == Add.FUNCLASH) {
            fact.makeId();
            DATEFACTCONFLICTS.write(fact);
            DATEFACTCONFLICTS.write(new Fact(fact.getId(), YAGO.extractionSource, theme.asYagoEntity()));
          }
        }
      }
      Announce.done();
    }

    // add infobox facts
    // add only those, which agree with category facts
    List<Theme> infoboxThemes = new ArrayList<>();
    infoboxThemes.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(Arrays.asList("en")));
    infoboxThemes.add(TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
    for (Theme theme : inputOrdered()) {
      if (!theme.isAvailableForReading()) continue;
      Announce.doing("Loading from", theme);
      for (Fact fact : theme) {
        if (isMyRelation(fact)) {
          // check
          Set<Fact> facts = categoryFacts.getFactsWithSubjectAndRelation(fact.getSubject(), fact.getRelation());
          boolean add = true;
          for (Fact other : facts) {
            if (other.getObject().equals(fact.getObject())) continue;
            if (!FactComponent.isMoreSpecific(fact.getObject(), other.getObject())) {
              add = false;
            }
          }

          if (add && infoboxFacts.add(fact, functions) == Add.FUNCLASH) {
            fact.makeId();
            DATEFACTCONFLICTS.write(fact);
            DATEFACTCONFLICTS.write(new Fact(fact.getId(), YAGO.extractionSource, theme.asYagoEntity()));
          }
        }
      }
      Announce.done();
    }
    Announce.done();

    Announce.doing("Removing false facts");
    for (Fact f : PatternHardExtractor.FALSEFACTS) {
      f.makeId();
      infoboxFacts.remove(f);
    }
    Announce.done();

    Announce.doing("Writing");
    for (Fact f : infoboxFacts) {
      f.makeId();
      YAGODATEFACTS.write(f);
    }
    Announce.done();

    Announce.done();
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new DateExtractor().extract(new File("/home/tr/tmp/yago3-debug"), "test");
  }

}
