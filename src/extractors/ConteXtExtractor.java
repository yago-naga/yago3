package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import extractorUtils.FactTemplateExtractor;
import extractorUtils.TitleExtractor;

/**
 * Extracts context keyphrases (the X in SPOTLX) facts from Wikipedia
 * 
 * @author Johannes Hoffart
 * 
 */
public class ConteXtExtractor extends Extractor {

	/** Input file */
	private File wikipedia;

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.CONTEXTPATTERNS,
				PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
	}

	/** Means facts from disambiguation pages */
	public static final Theme CONTEXTFACTS = new Theme("conteXtFacts",
			"Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(CONTEXTFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting disambiguation means");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor(input);

		FactCollection contextPatternCollection = new FactCollection(
				input.get(PatternHardExtractor.CONTEXTPATTERNS));
		FactTemplateExtractor contextPatterns = new FactTemplateExtractor(contextPatternCollection,
				"<_extendedContextWikiPattern>");

		FactWriter out = output.get(CONTEXTFACTS);

		String titleEntity = null;
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			default:
				if (titleEntity == null)
					continue;

				String page = FileLines.readBetween(in, "<text", "</text>");
				for (Fact fact : contextPatterns.extract(page, titleEntity)) {
				  if (fact != null)
				    out.write(fact);
				}
			}
		}
	}

	/**
	 * Needs Wikipedia as input
	 * 
	 * @param wikipedia
	 *            Wikipedia XML dump
	 */
	public ConteXtExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

}
