package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import fromOtherSources.HardExtractor;

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
	public List<Theme> inputOrdered() {
		return Arrays.asList(HardExtractor.HARDWIREDFACTS);
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
		new SchemaExtractor().extract(new File("c:/fabian/data/yago3"), "test");
	}

}
