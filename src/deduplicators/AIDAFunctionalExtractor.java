package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import extractors.MultilingualExtractor;
import javatools.administrative.Announce;
import utils.Theme;
import utils.Theme.ThemeGroup;
import basics.Fact;
import fromOtherSources.HardExtractor;
import fromOtherSources.WikidataLabelExtractor;
import fromThemes.PersonNameExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.WikiInfoExtractor;

/**
 * AIDA Fact Extractor
 * 
 * Extracts all facts necessary for AIDA and puts them in a single file.
 */
public class AIDAFunctionalExtractor extends SimpleDeduplicator {

	@Override
	@ImplementationNote("Hardwired facts go first. Infoboxes should go before categories")
	public List<Theme> inputOrdered() {
		List<Theme> input = new ArrayList<Theme>();
		
		// For YAGO compliance.
		input.add(SchemaExtractor.YAGOSCHEMA);
		
		// Dictionary.
		input.add(PersonNameExtractor.PERSONNAMES);
		input.add(PersonNameExtractor.PERSONNAMEHEURISTICS);
		input.add(GenderExtractor.PERSONS_GENDER);
		input.add(WikidataLabelExtractor.WIKIPEDIALABELS);
		input.add(WikidataLabelExtractor.WIKIDATAMULTILABELS);
		input.add(HardExtractor.HARDWIREDFACTS);
		
		// Metadata.
		input.addAll(WikiInfoExtractor.WIKIINFO.inLanguages(MultilingualExtractor.wikipediaLanguages));
		
		return input;
	}

	/** All facts of YAGO */
	public static final Theme AIDAFUNCTIONALFACTS = new Theme("aidaFunctionalFacts",
			"All functional facts necessary for AIDA", ThemeGroup.OTHER);

	/** All facts of YAGO */
	public static final Theme AIDAFUNCTIONALCONFLICTS = new Theme("_aidaFunctionalFactConflicts",
			"Facts that were not added because they conflicted with an existing fact");

	@Override
	public Theme myOutput() {
		return AIDAFUNCTIONALFACTS;
	}

	@Override
	public Theme conflicts() {
		return AIDAFUNCTIONALCONFLICTS;
	}

	@Override
	public boolean isMyRelation(Fact fact) {
		if (AIDAExtractorMerger.relations.contains(fact.getRelation())) {
			return true;
		} else {
			return false;
		}
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new AIDAFunctionalExtractor().extract(new File("C:/fabian/data/yago3"), "test");
	}

}
