package extractors;

import java.io.File;
import java.util.Map;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import basics.FactSource;
import basics.FactWriter;
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
	public static final Theme INFOBOXPATTERNS = new Theme("_infoboxPatterns");
	/** Patterns of titles */
	public static final Theme TITLEPATTERNS = new Theme("_titlePatterns");
	/** Patterns of categories */
	public static final Theme CATEGORYPATTERNS = new Theme("_categoryPatterns");
	/** Patterns of categories */
	public static final Theme RULES = new Theme("_rules");

	public Map<Theme, String> output() {
		return (new FinalMap<Theme, String>(INFOBOXPATTERNS, "These are the Wikipedia infobox patterns used in YAGO",
				TITLEPATTERNS, "These are the replacement patterns for Wikipedia titles used in YAGO",
				CATEGORYPATTERNS, "These are the Wikipedia category patterns used in YAGO", RULES,
				"These are the implication rules of YAGO"));
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> factCollections) throws Exception {
		Announce.doing("Copying patterns");
		Announce.message("Input folder is", inputFolder);
		for (Theme t : output().keySet()) {
			extract(t.file(inputFolder), writers.get(t));
		}
		Announce.done();
	}

	public PatternHardExtractor(File inputFolder) {
		super(inputFolder);
	}
}
