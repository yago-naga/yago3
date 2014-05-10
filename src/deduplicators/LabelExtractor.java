package deduplicators;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.PersonNameExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.FlightIATAcodeExtractor;
import fromWikipedia.RedirectExtractor;

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
				WikidataLabelExtractor.WIKIPEDIALABELS,
				PersonNameExtractor.PERSONNAMES, WordnetExtractor.WORDNETWORDS,
				SchemaExtractor.YAGOSCHEMA, WordnetExtractor.WORDNETGLOSSES,
				FlightIATAcodeExtractor.AIRPORT_CODE,
				GeoNamesDataImporter.GEONAMESMAPPEDDATA));
		input.addAll(CategoryMapper.CATEGORYFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(InfoboxMapper.INFOBOXFACTS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(RedirectExtractor.REDIRECTLABELS
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
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
