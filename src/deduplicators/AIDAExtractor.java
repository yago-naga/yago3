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
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.PersonNameExtractor;
import fromThemes.TransitiveTypeExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.ConteXtExtractor;
import fromWikipedia.DisambiguationPageExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.StructureExtractor;
import fromWikipedia.WikiInfoExtractor;

/**
 * AIDA Fact Extractor
 * 
 * Extracts all facts necessary for AIDA and puts them in a single file.
 */
public class AIDAExtractor extends SimpleDeduplicator {

	@Override
	@ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
	public List<Theme> inputOrdered() {
		List<Theme> input = new ArrayList<Theme>();
		
		// For YAGO compliance.
		input.add(SchemaExtractor.YAGOSCHEMA);
		
		// Dictionary.
		input.addAll(StructureExtractor.STRUCTUREFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages)); // also gives links and anchor texts.
		input.addAll(DisambiguationPageExtractor.DISAMBIGUATIONMEANSFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.add(PersonNameExtractor.PERSONNAMES);
		input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
		input.addAll(RedirectExtractor.REDIRECTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.add(GenderExtractor.PERSONS_GENDER);
		input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
		//input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
		
		// Metadata.
		input.addAll(WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages));
		
		// Types and Taxonomy.
		input.add(TransitiveTypeExtractor.TRANSITIVETYPE);
		input.add(ClassExtractor.YAGOTAXONOMY);
		
		// Keyphrases.
		input.addAll(ConteXtExtractor.CONTEXTFACTS.inLanguages(MultilingualExtractor.wikipediaLanguages));
		input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
		return input;
	}

	/** All facts of YAGO */
	public static final Theme AIDAFACTS = new Theme("aidaFacts",
			"All facts necessary for AIDA", ThemeGroup.OTHER);

	/** All facts of YAGO */
	public static final Theme AIDACONFLICTS = new Theme("_aidaFactConflicts",
			"Facts that were not added because they conflicted with an existing fact");

	/** Relations that AIDA needs. */
	public static final Set<String> relations = new FinalSet<>(
			RDFS.type, RDFS.subclassOf, RDFS.label, "skos:prefLabel",
			"<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>",
			"<hasGender>", "<hasAnchorText>", "<hasInternalWikipediaLinkTo>",
			"<redirectedFrom>", "<hasWikipediaUrl>", "<hasCitationTitle>",
			"<hasWikipediaCategory>", "<hasWikipediaAnchorText>");

	@Override
	public Theme myOutput() {
		return AIDAFACTS;
	}

	@Override
	public Theme conflicts() {
		return AIDACONFLICTS;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		if (relations.contains(fact.getRelation())) {
			return true;
		} else {
			return false;
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new AIDAExtractor().extract(new File("C:/fabian/data/yago3"), "test");
	}

}
