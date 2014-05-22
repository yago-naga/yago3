package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.CoordinateExtractor;
import fromWikipedia.TemporalCategoryExtractor;
import fromWikipedia.TemporalInfoboxExtractor;

/**
 * YAGO2s - LiteralFactExtractor
 * 
 * Deduplicates all facts with literals and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class LiteralFactExtractor extends SimpleDeduplicator {

	@Override
	@ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
	public List<Theme> inputOrdered() {
		List<Theme> input = new ArrayList<>();
		input.add(SchemaExtractor.YAGOSCHEMA);
		input.add(HardExtractor.HARDWIREDFACTS);
		input.add(CoordinateExtractor.COORDINATES);
		input.addAll(InfoboxMapper.INFOBOXFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(CategoryMapper.CATEGORYFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(Arrays.asList(RuleExtractor.RULERESULTS,
				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
				TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS,
				GeoNamesDataImporter.GEONAMESMAPPEDDATA));
		return input;
	}

	/** All facts of YAGO */
	public static final Theme YAGOLITERALFACTS = new Theme("yagoLiteralFacts",
			"All facts of YAGO that contain literals (except labels)",
			ThemeGroup.CORE);

	/** All facts of YAGO */
	public static final Theme LITERALFACTCONFLICTS = new Theme(
			"_literalFactConflicts",
			"Literal facts that were not added because they conflicted with an existing fact");

	@Override
	public Theme conflicts() {
		return LITERALFACTCONFLICTS;
	}

	/** relations that we exclude, because they are treated elsewhere */
	public static final Set<String> relationsExcluded = new FinalSet<>(
			RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range,
			RDFS.subpropertyOf, RDFS.label, "skos:prefLabel",
			"<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>",
			"<hasGloss>", "<hasConfidence>", "<redirectedFrom>",
			"<wasBornOnDate>", "<diedOnDate>", "<wasCreatedOnDate>",
			"<wasDestroyedOnDate>", "<happenedOnDate>", "<startedOnDate>",
			"<endedOnDate>");

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new LiteralFactExtractor().extract(new File("C:/fabian/data/yago2s"),
				"test");
	}

	@Override
	public Theme myOutput() {
		return YAGOLITERALFACTS;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		if (fact.getRelation().startsWith("<_"))
			return (false);
		if (relationsExcluded.contains(fact.getRelation()))
			return (false);
		return (!FactComponent.isFactId(fact.getArg(1)) && FactComponent
				.isLiteral(fact.getArg(2)));
	}
}
