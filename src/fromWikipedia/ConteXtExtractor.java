package fromWikipedia;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.util.FileUtils;
import utils.FactTemplateExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.Redirector;
import fromThemes.TypeChecker;

/**
 * Extracts context keyphrases (the X in SPOTLX) facts from Wikipedia.
 * 
 * For now, the provenance generation (yagoConteXtFacts) is disabled. 
 * 
 * @author Johannes Hoffart
 * 
 */
public class ConteXtExtractor extends Extractor {

	/** Input file */
	private File wikipedia;

  @Override
  public File inputDataFile() {   
    return wikipedia;
  }

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.CONTEXTPATTERNS,
				PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
	}
	
	 /** Context for entities */
  public static final Theme DIRTYCONTEXTFACTS = new Theme("conteXtFactsDirty",
      "Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names - needs redirecting and typechecking");

  public static final Theme REDIRECTEDCONTEXTFACTS = new Theme("conteXtFactsRedirected",
      "Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names - needs typechecking");
  
	/** Context for entities */
	public static final Theme CONTEXTFACTS = new Theme("yagoConteXtFacts",
			"Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names");

	 /** Context for entities provenance */
//  public static final Theme CONTEXTSOURCES = new Theme("yagoConteXtSources",
//      "Source information for the extracted keyphrases");
	
	@Override
	public Set<Theme> output() {
//		return new FinalSet<Theme>(DIRTYCONTEXTFACTS, CONTEXTSOURCES);
	  return new FinalSet<Theme>(DIRTYCONTEXTFACTS);
	}
	
  @Override
 public Set<Extractor> followUp() {
   return new HashSet<Extractor>(
       Arrays.asList(
           (Extractor) new Redirector(DIRTYCONTEXTFACTS, REDIRECTEDCONTEXTFACTS, this, decodeLang(this.wikipedia.getName())), 
           (Extractor) new TypeChecker(REDIRECTEDCONTEXTFACTS, CONTEXTFACTS, this)));
 }

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting context facts");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor(input);

		FactCollection contextPatternCollection = new FactCollection(
				input.get(PatternHardExtractor.CONTEXTPATTERNS));
		FactTemplateExtractor contextPatterns = new FactTemplateExtractor(contextPatternCollection,
				"<_extendedContextWikiPattern>");

		FactWriter out = output.get(DIRTYCONTEXTFACTS);
//    FactWriter outSources = output.get(CONTEXTSOURCES);

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
				String normalizedPage = Char.decodeAmpersand(Char.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+", " ")));

//				for (Pair<Fact, String> fact : contextPatterns.extractWithProvenance(normalizedPage, titleEntity)) {
//				  if (fact.first != null)
//				    write(out, fact.first, outSources, FactComponent.wikipediaURL(titleEntity), "ConteXtExtractor from: " + fact.second);
//				}
        for (Fact fact : contextPatterns.extract(normalizedPage, titleEntity)) {
          if (fact != null) {
            out.write(fact);
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
	public ConteXtExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

}
