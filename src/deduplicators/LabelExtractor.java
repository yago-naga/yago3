package deduplicators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromGeonames.GeoNamesDataImporter;
import fromOtherSources.HardExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CategoryMapper;
import fromThemes.InfoboxMapper;
import fromThemes.PersonNameExtractor;
import fromWikipedia.DisambiguationPageExtractor;
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
	@ImplementationNote("We don't have conflicts here, so let's just take any order")
	public List<Theme> inputOrdered() {
		List<Theme> input = new ArrayList<>();
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.addAll(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.add(HardExtractor.HARDWIREDFACTS);
    input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
    input.add(PersonNameExtractor.PERSONNAMES);
    input.add(WordnetExtractor.WORDNETWORDS);
    input.add(SchemaExtractor.YAGOSCHEMA);
    input.add(WordnetExtractor.WORDNETGLOSSES);
    input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
    input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
     input.add(GeoNamesDataImporter.GEONAMES_MAPPED_DATA);
    input.addAll(CategoryMapper.CATEGORYFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(InfoboxMapper.INFOBOXFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
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
