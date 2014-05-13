package fromOtherSources;

import java.io.File;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.Theme;

/**
 * PatternHardExtractor - YAGO2s
 * 
 * Produces the hard-coded facts that are YAGO-internal.
 * 
 * @author Fabian
 * 
 */
public class PatternHardExtractor extends HardExtractor {

	/** Patterns of infoboxes */
	public static final Theme INFOBOXPATTERNS = new Theme("_infoboxPatterns",
			"The Wikipedia infobox patterns used in YAGO");
	/** Language codes */
	public static final Theme LANGUAGECODEMAPPING = new Theme(
			"_languageCodeMappings",
			"Mappings from ISO 639-1 codes to ISO 639-2/T codes.");
	public static final Theme INFOBOXTEMPORALPATTERNS = new Theme(
			"_infoboxTemporalPatterns",
			"The Wikipedia infobox patterns used in YAGO");
	/** Patterns of titles */
	public static final Theme TITLEPATTERNS = new Theme("_titlePatterns",
			"The replacement patterns for Wikipedia titles used in YAGO");
	/** Patterns of categories */
	public static final Theme CATEGORYPATTERNS = new Theme("_categoryPatterns",
			"The Wikipedia category patterns used in YAGO");
	public static final Theme TEMPORALCATEGORYPATTERNS = new Theme(
			"_categoryTemporalPatterns",
			"The Wikipedia category patterns used in YAGO");
	/** Patterns of disambiguation pages */
	public static final Theme DISAMBIGUATIONTEMPLATES = new Theme(
			"_disambiguationPatterns",
			"Patterns for the disambiguation pages of Wikipedia");
	/** Patterns of entity keyphrases */
	public static final Theme CONTEXTPATTERNS = new Theme(
			"_extendedContextWikiPatterns",
			"Patterns for extracting Keyphrases");
	/** Patterns of entity keyphrases */
	public static final Theme STRUCTUREPATTERNS = new Theme(
			"_extendedStructureWikiPatterns",
			"Patterns for extracting regular structure from Wikipedia (e.g. links)");
	/** Implication rules of YAGO */
	public static final Theme RULES = new Theme("_rules",
			"These are the implication rules of YAGO");
	/** Implication rules of YAGO SPOTLX representation */
	public static final Theme SPOTLX_ENTITY_RULES = new Theme(
			"_spotlxEntityRules",
			"Implication rules for YAGO SPOTLX representation");
	public static final Theme SPOTLX_FACT_RULES = new Theme("_spotlxFactRules",
			"Implication rules for YAGO SPOTLX representation");

	public Set<Theme> output() {
		return (new FinalSet<Theme>(INFOBOXPATTERNS, INFOBOXTEMPORALPATTERNS,
				TITLEPATTERNS, CATEGORYPATTERNS, TEMPORALCATEGORYPATTERNS,
				RULES, DISAMBIGUATIONTEMPLATES, CONTEXTPATTERNS,
				STRUCTUREPATTERNS, LANGUAGECODEMAPPING, SPOTLX_ENTITY_RULES,
				SPOTLX_FACT_RULES));
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Copying patterns");
		Announce.message("Input folder is", inputData);
		for (Theme t : output()) {
			File f = t.findFileInFolder(inputData);
			Announce.doing("Copying hard wired facts from", f.getName());
			for (Fact fact : FactSource.from(f)) {
				t.write(fact);
			}
			Announce.done();
		}
		Announce.done();
	}

	public PatternHardExtractor(File inputFolder) {
		super(inputFolder);
	}

	public PatternHardExtractor() {
		this(new File("./data"));
	}

	public static void main(String[] args) throws Exception {
		new PatternHardExtractor(new File("./data")).extract(new File(
				"c:/fabian/data/yago3"), "test");
	}
}