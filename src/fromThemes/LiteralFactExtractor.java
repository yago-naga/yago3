package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromWikipedia.CategoryMapper;
import fromWikipedia.CoordinateExtractor;
import fromWikipedia.FlightIATAcodeExtractor;
import fromWikipedia.InfoboxMapper;
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
	public Set<Theme> input() {
		Set<Theme> input = new HashSet<Theme>(Arrays.asList(
				HardExtractor.HARDWIREDFACTS, RuleExtractor.RULERESULTS,
				TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
				TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS,
				SchemaExtractor.YAGOSCHEMA,
				FlightIATAcodeExtractor.AIRPORT_CODE,
				SchemaExtractor.YAGOSCHEMA, CoordinateExtractor.COORDINATES,
				GeoNamesDataImporter.GEONAMESMAPPEDDATA));

		input.addAll(CategoryMapper.CATEGORYFACTS.inAllLanguages());
		input.addAll(InfoboxMapper.INFOBOXFACTS.inAllLanguages());
		return input;
	}

	/** All facts of YAGO */
	public static final Theme YAGOLITERALFACTS = new Theme("yagoLiteralFacts",
			"All facts of YAGO that contain literals (except labels)",
			ThemeGroup.CORE);

	/** relations that we exclude, because they are treated elsewhere */
	public static final Set<String> relationsExcluded = new FinalSet<>(
			RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range,
			RDFS.subpropertyOf, RDFS.label, "skos:prefLabel",
			"<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>",
			"<hasGloss>", "<hasConfidence>", "<redirectedFrom>");

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
