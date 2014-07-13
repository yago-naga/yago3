package fromWikipedia;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.FactTemplateExtractor;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;
import basics.Fact;
import extractors.MultilingualWikipediaExtractor;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * Extracts Wikipedia links
 * 
 * @author Johannes Hoffart
 * 
 */
public class StructureExtractor extends MultilingualWikipediaExtractor {

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.STRUCTUREPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.PREFMEANINGS));
	}

	@Override
	public Set<FollowUpExtractor> followUp() {
		return new FinalSet<FollowUpExtractor>(new Redirector(
				DIRTYSTRUCTUREFACTS.inLanguage(language), REDIRECTEDSTRUCTUREFACTS.inLanguage(language), this),
				new TypeChecker(REDIRECTEDSTRUCTUREFACTS.inLanguage(language), STRUCTUREFACTS.inLanguage(language), this));
	}

	/** Facts representing the Wikipedia structure (e.g. links) */
	public static final MultilingualTheme DIRTYSTRUCTUREFACTS = new MultilingualTheme(
			"structureFactsNeedTypeCheckingRedirecting",
			"Regular structure from Wikipedia, e.g. links - needs redirecting and typechecking");

	/** Facts representing the Wikipedia structure (e.g. links) */
	public static final MultilingualTheme REDIRECTEDSTRUCTUREFACTS = new MultilingualTheme(
			"structureFactsNeedTypeChecking",
			"Regular structure from Wikipedia, e.g. links - needs typechecking");

	/** Facts representing the Wikipedia structure (e.g. links) */
	public static final MultilingualTheme STRUCTUREFACTS = new MultilingualTheme("structureFacts",
			"Regular structure from Wikipedia, e.g. links");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(DIRTYSTRUCTUREFACTS.inLanguage(language));
	}

	@Override
	public void extract() throws Exception {
		// Extract the information
		Announce.doing("Extracting structure facts");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor("en");

		FactCollection structurePatternCollection = PatternHardExtractor.STRUCTUREPATTERNS
				.factCollection();
		FactTemplateExtractor structurePatterns = new FactTemplateExtractor(
				structurePatternCollection, "<_extendedStructureWikiPattern>");

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
				String normalizedPage = page.replaceAll("[\\s\\x00-\\x1F]+",
						" ");

				for (Fact fact : structurePatterns.extract(normalizedPage,
						titleEntity, language)) {
					if (fact != null)
						DIRTYSTRUCTUREFACTS.inLanguage(language).write(fact);
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
	public StructureExtractor(String lang, File wikipedia) {
	  super(lang, wikipedia);
	}

}
