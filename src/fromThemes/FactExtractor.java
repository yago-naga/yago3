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
import fromWikipedia.CategoryExtractor;
import fromWikipedia.CategoryMapper;
import fromWikipedia.CategoryTypeExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.InfoboxExtractor;
import fromWikipedia.InfoboxMapper;
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
				SchemaExtractor.YAGOSCHEMA));

		for (String lang : Extractor.languages) {
			input.add(CategoryMapper.CATEGORYFACTS_MAP.get(lang));
			input.add(InfoboxMapper.INFOBOXFACTS_MAP.get(lang));
		}
		return input;
	}

	/** All facts of YAGO */
	public static final Theme YAGOFACTS = new Theme("yagoFacts",
			"All facts of YAGO that hold between instances", ThemeGroup.CORE);

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
		new FactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
	}

}
