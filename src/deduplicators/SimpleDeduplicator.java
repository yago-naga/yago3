package deduplicators;

import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.RDFS;
import basics.Theme;
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

	@Override
	public final Set<Theme> output() {
		return new FinalSet<>(myOutput());
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(SchemaExtractor.YAGOSCHEMA);
	}

	/** TRUE if I want to write this relation */
	public abstract boolean isMyRelation(Fact fact);

	@Override
	public void extract() throws Exception {
		Announce.doing("Deduplicating", this.getClass().getSimpleName());
		Set<String> functions = null;
		if (!input().contains(SchemaExtractor.YAGOSCHEMA)) {
			Announce.warning("Deduplicators should have SchemaExtractor.YAGOSCHEMA, in their required input!");
		} else {
			functions = SchemaExtractor.YAGOSCHEMA.factCollection()
					.seekSubjects(RDFS.type, YAGO.function);
		}

		Announce.doing("Loading");
		FactCollection batch = new FactCollection();
		for (Theme theme : input()) {
			Announce.doing("Loading from", theme);
			for (Fact fact : theme.factSource()) {
				if (isMyRelation(fact))
					batch.add(fact, functions);
			}
			Announce.done();
		}
		Announce.done();

		Announce.doing("Writing");
		for (Fact f : batch)
			myOutput().write(f);
		Announce.done();
		myOutput().close();

		Announce.done();
	}

}
