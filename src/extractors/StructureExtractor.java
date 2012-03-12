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
 * Extracts Wikipedia links
 * 
 * @author Johannes Hoffart
 * 
 */
public class StructureExtractor extends Extractor {

	/** Input file */
	private File wikipedia;

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.STRUCTUREPATTERNS, 
				PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
	}

	/** Means facts from disambiguation pages */
	public static final Theme STRUCTUREFACTS = new Theme("structureFacts",
			"Regular structure from Wikipedia, e.g. links");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(STRUCTUREFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting disambiguation means");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor(input);

		FactCollection structurePatternCollection = new FactCollection(
				input.get(PatternHardExtractor.STRUCTUREPATTERNS));
		FactTemplateExtractor structurePatterns = new FactTemplateExtractor(structurePatternCollection,
				"<_extendedStructureWikiPattern>");

		FactWriter out = output.get(STRUCTUREFACTS);

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
				for (Fact fact : structurePatterns.extract(page, titleEntity)) {
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
	public StructureExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

}
