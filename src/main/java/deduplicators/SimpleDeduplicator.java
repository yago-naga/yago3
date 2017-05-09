package deduplicators;

import java.io.File;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.PatternHardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.filehandlers.FileUtils;
import utils.FactCollection;
import utils.FactCollection.Add;
import utils.Theme;

/**
 * YAGO2s - SimpleDeduplicator
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

public abstract class SimpleDeduplicator extends Extractor {

  /** Theme that I want to output */
  public abstract Theme myOutput();

  /** Theme where I store conflicts (or NULL) */
  public Theme conflicts() {
    return (null);
  }

  @Override
  public final Set<Theme> output() {
    if (conflicts() == null) return new FinalSet<>(myOutput());
    else return (new FinalSet<>(myOutput(), conflicts()));
  }

  /** The list of input themes, ordered by authority. */
  @Fact.ImplementationNote("If two facts contradict, the *earlier* one will prevail")
  public abstract List<Theme> inputOrdered();

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

  /** TRUE if I want to write this relation */
  public abstract boolean isMyRelation(Fact fact);

  @Override
  public void extract() throws Exception {
    Announce.doing("Running", this.getClass().getSimpleName());
    @Fact.ImplementationNote("We also count functions in time as functions, because many functions ini time have bogus integer values after the real value in the infoboxes.")
    Set<String> functions = null;
    if (!input().contains(SchemaExtractor.YAGOSCHEMA)) {
      Announce.warning("Deduplicators should have SchemaExtractor.YAGOSCHEMA, in their required input so that they can check functional relations!");
    } else {
      functions = SchemaExtractor.YAGOSCHEMA.factCollection().seekSubjects(RDFS.type, YAGO.function);
      functions.addAll(SchemaExtractor.YAGOSCHEMA.factCollection().seekSubjects(RDFS.type, YAGO.functionInTime));
    }

    Writer tsv = FileUtils.getBufferedUTF8Writer(
        new File(SchemaExtractor.YAGOSCHEMA.file().getParent(), "_factStatistics_" + this.getClass().getSimpleName() + ".tsv"));
    Announce.doing("Loading");
    FactCollection batch = new FactCollection();
    for (Theme theme : inputOrdered()) {
      if (!theme.isAvailableForReading()) continue;
      Announce.doing("Loading from", theme);
      IntHashMap<FactCollection.Add> added = new IntHashMap<>();
      for (Fact fact : theme) {
        if (isMyRelation(fact)) {
          Add whatHappened = batch.add(fact, functions);
          added.increase(whatHappened);
          if (whatHappened == Add.FUNCLASH && conflicts() != null) {
            fact.makeId();
            conflicts().write(fact);
            conflicts().write(new Fact(fact.getId(), YAGO.extractionSource, theme.asYagoEntity()));
          }
        }
      }
      Announce.message(added);
      tsv.write(theme.toString());
      for (Add a : Add.values()) {
        tsv.write("\t" + a + "\t" + added.get(a));
      }
      tsv.write("\n");
      tsv.flush();
      Announce.done();
    }
    Announce.done();
    tsv.close();

    Announce.doing("Removing false facts");
    for (Fact f : PatternHardExtractor.FALSEFACTS) {
      f.makeId();
      batch.remove(f);
    }
    Announce.done();

    Announce.doing("Writing");
    for (Fact f : batch) {
      f.makeId();
      myOutput().write(f);
    }
    Announce.done();

    Announce.done();
  }

}
