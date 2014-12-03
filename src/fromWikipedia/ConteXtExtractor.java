package fromWikipedia;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.FactTemplateExtractor;
import utils.MultilingualTheme;
import utils.PatternList;
import utils.Theme;
import utils.TitleExtractor;
import basics.Fact;
import extractors.MultilingualWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * Extracts context keyphrases (the X in SPOTLX) facts from Wikipedia.
 * 
 * For now, the provenance generation (yagoConteXtFacts) is disabled.
 * 
 * @author Johannes Hoffart
 * 
 */
public class ConteXtExtractor extends MultilingualWikipediaExtractor {

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.CONTEXTPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				PatternHardExtractor.AIDACLEANINGPATTERNS,
				WordnetExtractor.PREFMEANINGS,PatternHardExtractor.LANGUAGECODEMAPPING));
	}

	@Override
	public Set<Theme> inputCached() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.CONTEXTPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
        PatternHardExtractor.AIDACLEANINGPATTERNS,
				WordnetExtractor.PREFMEANINGS,PatternHardExtractor.LANGUAGECODEMAPPING));
	}
	 
	/** Context for entities */
	public static final MultilingualTheme CONTEXTFACTS = new MultilingualTheme(
			"yagoConteXtFacts",
			"Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names");

	@Override
	public Set<Theme> output() {
		// return new FinalSet<Theme>(DIRTYCONTEXTFACTS, CONTEXTSOURCES);
		return new FinalSet<Theme>(CONTEXTFACTS.inLanguage(language));
	}

	@Override
	public void extract() throws Exception {
		// Extract the information
		Announce.doing("Extracting context facts");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor(language);

		FactCollection contextPatternCollection = PatternHardExtractor.CONTEXTPATTERNS
				.factCollection();
		FactTemplateExtractor contextPatterns = new FactTemplateExtractor(
				contextPatternCollection, "<_extendedContextWikiPattern>");
		PatternList replacements = new PatternList(
        PatternHardExtractor.AIDACLEANINGPATTERNS.factCollection(),
        "<_aidaCleaning>");

		// FactWriter outSources = output.get(CONTEXTSOURCES);

		String titleEntity = null;
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = titleExtractor.getTitleEntity(in);
				if (titleEntity == null)
					continue;

				String page = FileLines.readBetween(in, "<text", "</text>");
				String normalizedPage = Char17.decodeAmpersand(Char17
						.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+",
								" ")));
				String transformedPage = replacements.transform(normalizedPage);

				// for (Pair<Fact, String> fact :
				// contextPatterns.extractWithProvenance(normalizedPage,
				// titleEntity)) {
				// if (fact.first != null)
				// write(out, fact.first, outSources,
				// FactComponent.wikipediaURL(titleEntity),
				// "ConteXtExtractor from: " + fact.second);
				// }
				for (Fact fact : contextPatterns.extract(transformedPage,
						titleEntity, language)) {
					if (fact != null) {
					  CONTEXTFACTS.inLanguage(language).write(fact);
					}
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
	public ConteXtExtractor(String lang, File wikipedia) {
		super(lang, wikipedia);
	}

}
