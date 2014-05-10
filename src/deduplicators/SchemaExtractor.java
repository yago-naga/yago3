package deduplicators;

import java.io.File;
import java.util.Set;

import fromOtherSources.HardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import basics.YAGO;

/**
 * YAGO2s - SchemaExtractor
 * 
 * Deduplicates all schema facts (except for the multilingual ones). This
 * extractor is different from FactExtractor so that it can run in parallel.
 * 
 * @author Fabian M. Suchanek
 * 
 */

public class SchemaExtractor extends SimpleDeduplicator {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(HardExtractor.HARDWIREDFACTS);
	}

	/** All facts of YAGO */
	public static final Theme YAGOSCHEMA = new Theme("yagoSchema",
			"The domains, ranges and confidence values of YAGO relations",
			ThemeGroup.TAXONOMY);

	/** Relations that we care for */
	public static Set<String> relations = new FinalSet<>(RDFS.domain,
			RDFS.range, RDFS.subpropertyOf, YAGO.hasConfidence);

	@Override
	public Theme myOutput() {
		return YAGOSCHEMA;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		boolean isDesiredRelation = relations.contains(fact.getRelation());
		boolean isTypeRelation = fact.getRelation().equals(RDFS.type);
		boolean hasRightTypeArguments = fact.getArg(1).matches(
				".*Property.*|.*Relation.*")
				|| fact.getArg(2).matches(".*Property.*|.*Relation.*");

		return isDesiredRelation || (isTypeRelation && hasRightTypeArguments);
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new SchemaExtractor().extract(new File("/home/jbiega/data/yago2s"),
				"test");
	}

}
