package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import utils.Theme;
import utils.Theme.ThemeGroup;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.RuleExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.TemporalCategoryExtractor;
import fromWikipedia.TemporalInfoboxExtractor;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactExtractor extends SimpleDeduplicator {

	@Override
	public Set<Theme> input() {
		Set<Theme> input = new HashSet<Theme>(Arrays.asList(
				GenderExtractor.PERSONS_GENDER, HardExtractor.HARDWIREDFACTS,
				RuleExtractor.RULERESULTS, FlightExtractor.FLIGHTS,
				GeoNamesDataImporter.GEONAMESMAPPEDDATA,
				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
				TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS,
				SchemaExtractor.YAGOSCHEMA, GenderExtractor.PERSONS_GENDER));
		input.addAll(CategoryMapper.CATEGORYFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(InfoboxMapper.INFOBOXFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		return input;
	}

	/** All facts of YAGO */
	public static final Theme YAGOFACTS = new Theme("yagoFacts",
			"All facts of YAGO that hold between instances", ThemeGroup.CORE);

	/** All facts of YAGO */
	public static final Theme FACTCONFLICTS = new Theme("_factConflicts",
			"Facts that were not added because they conflicted with an existing fact");

	/** relations that we exclude, because they are treated elsewhere */
	public static final Set<String> relationsExcluded = new FinalSet<>(
			RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range,
			RDFS.subpropertyOf, RDFS.label, "skos:prefLabel",
			"<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>",
			"<hasGloss>", "<redirectedFrom>");

	@Override
	public Theme myOutput() {
		return YAGOFACTS;
	}

	@Override
	public Theme conflicts() {
		return FACTCONFLICTS;
	}
	
	@Override
	public boolean isMyRelation(Fact fact) {
		if (fact.getRelation().startsWith("<_"))
			return (false);
		if (relationsExcluded.contains(fact.getRelation()))
			return (false);
		if (FactComponent.isFactId(fact.getArg(1)))
			return (false);
		if (FactComponent.isLiteral(fact.getArg(2)))
			return (false);
		return (true);
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new FactExtractor().extract(new File("C:/fabian/data/yago3"), "test");
	}

}