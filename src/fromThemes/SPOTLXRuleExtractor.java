package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.FactCollection;
import basics.Theme;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.Extractor;

/**
 * YAGO2s - SPOTLXRuleExtractor
 * 
 * The base representative for SPOTLX rule deduction. It first uses time&space
 * 'entity' rules for infering the more general creation and destruction facts
 * (i.e. placedIn, startsExistingOnDate and endsExistingOnDate) and then calls
 * the extractor which continues the deduction process.
 * 
 * @author Joanna Biega
 */
public class SPOTLXRuleExtractor extends BaseRuleExtractor {

	@Override
	public Set<Extractor> followUp() {
		return (new HashSet<Extractor>(
				Arrays.asList(new SPOTLXDeductiveExtractor(1))));
	}

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(PatternHardExtractor.SPOTLX_ENTITY_RULES,
				PatternHardExtractor.HARDWIREDFACTS,
				TransitiveTypeExtractor.TRANSITIVETYPE,
				FactExtractor.YAGOFACTS, LiteralFactExtractor.YAGOLITERALFACTS);
	}

	/** Themes of spotlx deductions */
	public static final Theme RULERESULTS = new Theme("spotlxEntityFacts",
			"SPOTLX deduced facts");
	public static final Theme RULESOURCES = new Theme("spotlxEntitySources",
			"SPOTLX deduced facts");

	public Theme getRULERESULTS() {
		return RULERESULTS;
	}

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
		new PatternHardExtractor(new File("./data")).extract(new File(
				"/home/jbiega/data/yago2s"), "test");
		new HardExtractor(new File("../basics2s/data")).extract(new File(
				"/home/jbiega/data/yago2s"), "test");
		Announce.setLevel(Announce.Level.DEBUG);
		new SPOTLXRuleExtractor().extract(new File("/home/jbiega/data/yago2s"),
				"test");
	}

}