package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import extractors.MultilingualExtractor;
import fromOtherSources.HardExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.TemporalCategoryExtractor;
import fromWikipedia.TemporalInfoboxExtractor;

/**
 * YAGO2s - LiteralFactExtractor
 * 
 * Deduplicates all facts with dates and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class DateExtractor extends SimpleDeduplicator {

	@Override
	@ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
	public List<Theme> inputOrdered() {
		List<Theme> input = new ArrayList<Theme>();
		input.add(SchemaExtractor.YAGOSCHEMA);
		input.add(HardExtractor.HARDWIREDFACTS);
		input.addAll(InfoboxMapper.INFOBOXFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(CategoryMapper.CATEGORYFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.add(RuleExtractor.RULERESULTS);
		input.add(TemporalCategoryExtractor.TEMPORALCATEGORYFACTS);
		input.add(TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
		return input;
	}

	/** All facts of YAGO */
	public static final Theme YAGODATEFACTS = new Theme("yagoDateFacts",
			"All facts of YAGO that contain dates", ThemeGroup.CORE);

	/** All facts of YAGO */
	public static final Theme DATEFACTCONFLICTS = new Theme(
			"_dateFactConflicts",
			"Date facts that were not added because they conflicted with an existing fact");

	@Override
	public Theme conflicts() {
		return DATEFACTCONFLICTS;
	}

	/** relations that we treat */
	public static final Set<String> relationsIncluded = new FinalSet<>(
			"<wasBornOnDate>", "<diedOnDate>", "<wasCreatedOnDate>",
			"<wasDestroyedOnDate>", "<happenedOnDate>", "<startedOnDate>",
			"<endedOnDate>");

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new DateExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
	}

	@Override
	public Theme myOutput() {
		return YAGODATEFACTS;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		return (relationsIncluded.contains(fact.getRelation()));
	}
}
