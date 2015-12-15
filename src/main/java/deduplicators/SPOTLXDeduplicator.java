package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javatools.administrative.Announce;
import utils.Theme;
import basics.Fact;
import fromThemes.SPOTLXDeductiveExtractor;

/**
 * YAGO2s - SPOTLXDeduplicator
 * 
 * An clean-up extractor for the SPOTLX deduction process. It produces the final
 * results of SPOTLX deduction, filtering only occurs[In, Since, Until]
 * relations and removing the duplicates.
 * 
 * @author Joanna Biega
 */
public class SPOTLXDeduplicator extends SimpleDeduplicator {
	@Override
	public List<Theme> inputOrdered() {
		return Arrays.asList(SchemaExtractor.YAGOSCHEMA,
				SPOTLXDeductiveExtractor.RULERESULTS);
	}

	public static final Theme SPOTLXFACTS = new Theme("spotlxFacts",
			"SPOTLX deduced facts");

	public static final List<String> SPOTLX_FINAL_RELATIONS = new ArrayList<String>(
			Arrays.asList("<occursIn>", "<occursSince>", "<occursUntil>"));

	@Override
	public Theme myOutput() {
		return SPOTLXFACTS;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		return SPOTLX_FINAL_RELATIONS.contains(fact.getRelation());
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new SPOTLXDeduplicator().extract(new File("/home/jbiega/data/yago2s"),
				"test");
		// new SPOTLXDeduplicator().extract(new File("/local/jbiega/yagofacts"),
		// "test");
	}
}
