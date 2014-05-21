package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import utils.FactCollection;
import utils.Theme;
import deduplicators.ClassExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * YAGO2s - RuleExtractor
 * 
 * Generates the results of rules.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class RuleExtractor extends BaseRuleExtractor {

	@Override
	public Set<Theme> input() {
		Set<Theme> input = new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.RULES,
				TransitiveTypeExtractor.TRANSITIVETYPE,
				ClassExtractor.YAGOTAXONOMY, HardExtractor.HARDWIREDFACTS,
				WordnetExtractor.WORDNETCLASSES));
		input.addAll(CategoryMapper.CATEGORYFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(InfoboxMapper.INFOBOXFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		return input;
	}

	/** Theme of deductions */
	public static final Theme RULERESULTS = new Theme("ruleResults",
			"Results of rule applications");

	/** Theme of sources deductions */
	public static final Theme RULESOURCES = new Theme("ruleSources",
			"Source information for results of rule applications");

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

	/** Extract rule collection from fact sources */
	@Override
	public FactCollection getInputRuleCollection() throws Exception {
		FactCollection collection = new FactCollection(
				PatternHardExtractor.RULES);
		return collection;
	}

	public static void main(String[] args) throws Exception {
		new PatternHardExtractor(new File("./data")).extract(new File(
				"c:/fabian/data/yago2s"), "test");
		Announce.setLevel(Announce.Level.DEBUG);
		new RuleExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
