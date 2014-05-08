package fromThemes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.CategoryMapper;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.FlightIATAcodeExtractor;
import fromWikipedia.InfoboxExtractor;
import fromWikipedia.InfoboxMapper;
import fromWikipedia.PersonNameExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.WikipediaLabelExtractor;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;

/**
 * YAGO2s - LabelExtractor
 * 
 * Deduplicates all label facts (except for the multilingual ones). This
 * extractor is different from FactExtractor so that it can run in parallel.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class LabelExtractor extends SimpleDeduplicator {

	@Override
	public Set<Theme> input() {
		Set<Theme> input = new HashSet<Theme>(Arrays.asList(
				DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS,
				HardExtractor.HARDWIREDFACTS,
				WikipediaLabelExtractor.WIKIPEDIALABELS,
				PersonNameExtractor.PERSONNAMES, WordnetExtractor.WORDNETWORDS,
				SchemaExtractor.YAGOSCHEMA, WordnetExtractor.WORDNETGLOSSES,
				FlightIATAcodeExtractor.AIRPORT_CODE,
				GeoNamesDataImporter.GEONAMESMAPPEDDATA));
		input.addAll(CategoryMapper.CATEGORYFACTS.inAllLanguages());
		input.addAll(InfoboxMapper.INFOBOXFACTS.inAllLanguages());
		input.addAll(RedirectExtractor.REDIRECTLABELS.inAllLanguages());
		return input;
	}

	/** Relations that we care for */
	public static Set<String> relations = new FinalSet<>(RDFS.label,
			"skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>",
			"<hasFamilyName>", "<hasGloss>", "<redirectedFrom>");

	/** All facts of YAGO */
	public static final Theme YAGOLABELS = new Theme(
			"yagoLabels",
			"All facts of YAGO that contain labels (rdfs:label, skos:prefLabel, isPreferredMeaningOf, hasGivenName, hasFamilyName, hasGloss, redirectedFrom)",
			ThemeGroup.CORE);

	@Override
	public Theme myOutput() {
		return YAGOLABELS;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		return relations.contains(fact.getRelation());
	}

}
