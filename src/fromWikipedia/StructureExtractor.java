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
import followUp.EntityTranslator;
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
        PatternHardExtractor.INFOBOXPATTERNS,
				WordnetExtractor.PREFMEANINGS, PatternHardExtractor.LANGUAGECODEMAPPING));
	}

	@Override
	public Set<Theme> inputCached() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.STRUCTUREPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				PatternHardExtractor.INFOBOXPATTERNS,
				WordnetExtractor.PREFMEANINGS, PatternHardExtractor.LANGUAGECODEMAPPING));
	}
	
	@Override
	public Set<FollowUpExtractor> followUp() {
	  
	  Set<FollowUpExtractor> result = new HashSet<FollowUpExtractor>();

        
	  result.add(new Redirector(
        DIRTYSTRUCTUREFACTS.inLanguage(language), REDIRECTEDSTRUCTUREFACTS.inLanguage(language), this));

    if (!isEnglish()) {
      result.add(new EntityTranslator(REDIRECTEDSTRUCTUREFACTS.inLanguage(language), TRANSLATEDREDIRECTEDSTRUCTUREFACTS.inLanguage(this.language), this));
      result.add(new TypeChecker(TRANSLATEDREDIRECTEDSTRUCTUREFACTS.inLanguage(language), STRUCTUREFACTS.inLanguage(language), this));
    } else {
      result.add(new TypeChecker(REDIRECTEDSTRUCTUREFACTS.inLanguage(language), STRUCTUREFACTS.inLanguage(language), this));
    }
    return result;
	}

	/** Facts representing the Wikipedia structure (e.g. links) */
	public static final MultilingualTheme DIRTYSTRUCTUREFACTS = new MultilingualTheme(
			"structureFactsNeedTranslationTypeCheckingRedirecting",
			"Regular structure from Wikipedia, e.g. links - needs redirecting, translation and typechecking");
	
	/** Facts representing the Wikipedia structure (e.g. links) */
	public static final MultilingualTheme REDIRECTEDSTRUCTUREFACTS = new MultilingualTheme(
	    "structureFactsNeedTranslationTypeChecking",
	    "Regular structure from Wikipedia, e.g. links - needs translation and typechecking");

	 /** Facts representing the Wikipedia structure (e.g. links) */
  public static final MultilingualTheme TRANSLATEDREDIRECTEDSTRUCTUREFACTS = new MultilingualTheme(
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
		TitleExtractor titleExtractor = new TitleExtractor(language);

		FactCollection structurePatternCollection = PatternHardExtractor.STRUCTUREPATTERNS
				.factCollection();
		FactTemplateExtractor structurePatterns = new FactTemplateExtractor(
				structurePatternCollection, "<_extendedStructureWikiPattern>");
    PatternList replacements = new PatternList(
        PatternHardExtractor.INFOBOXPATTERNS.factCollection(),
        "<_infoboxReplace>");
    
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
				String normalizedPage = 
				    Char17.decodeAmpersand(
				        page.replaceAll("[\\s\\x00-\\x1F]+"," "));
				String transformedPage = replacements.transform(normalizedPage);

				for (Fact fact : structurePatterns.extract(transformedPage,
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
