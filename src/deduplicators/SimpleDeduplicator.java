package deduplicators;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.datatypes.IntHashMap;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.FactCollection.Add;
import utils.Theme;
import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;

/**
 * YAGO2s - SimpleDeduplicator
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
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
		if (conflicts() == null)
			return new FinalSet<>(myOutput());
		else
			return (new FinalSet<>(myOutput(), conflicts()));
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(SchemaExtractor.YAGOSCHEMA);
	}

	/** TRUE if I want to write this relation */
	public abstract boolean isMyRelation(Fact fact);

	@Override
	public void extract() throws Exception {
		Announce.doing("Running", this.getClass().getSimpleName());
		Set<String> functions = null;
		if (!input().contains(SchemaExtractor.YAGOSCHEMA)) {
			Announce.warning("Deduplicators should have SchemaExtractor.YAGOSCHEMA, in their required input so that they can check functional relations!");
		} else {
			functions = SchemaExtractor.YAGOSCHEMA.factCollection()
					.seekSubjects(RDFS.type, YAGO.function);
		}

		Writer tsv = FileUtils.getBufferedUTF8Writer(new File(
				SchemaExtractor.YAGOSCHEMA.file().getParent(),
				"_factStatistics_" + this.getClass().getSimpleName() + ".tsv"));
		Announce.doing("Loading");
		FactCollection batch = new FactCollection();
		List<Theme> inputs = new ArrayList<>();
		// Make sure that the English languages go first
		for (Theme t : input()) {
			if (t.isEnglishOrDefault())
				inputs.add(0, t);
			else
				inputs.add(t);
		}
		for (Theme theme : inputs) {
			if (!theme.isAvailableForReading())
				continue;
			Announce.doing("Loading from", theme);
			IntHashMap<FactCollection.Add> added = new IntHashMap<>();
			for (Fact fact : theme) {
				if (isMyRelation(fact)) {
					Add whatHappened = batch.add(fact, functions);
					added.increase(whatHappened);
					if (whatHappened == Add.FUNCLASH && conflicts() != null) {
						fact.makeId();
						conflicts().write(fact);
						conflicts().write(
								new Fact(fact.getId(), YAGO.extractionSource,
										theme.asYagoEntity()));
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

		Announce.doing("Writing");
		for (Fact f : batch)
			myOutput().write(f);
		Announce.done();

		Announce.done();
	}

}
