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
import javatools.util.FileUtils;
import utils.FactTemplateExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.PatternHardExtractor;
import fromThemes.Redirector;
import fromThemes.TypeChecker;

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
  public File inputDataFile() {   
    return wikipedia;
  }

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.STRUCTUREPATTERNS, 
				PatternHardExtractor.TITLEPATTERNS));
	}
	
	@Override
  public Set<Extractor> followUp() {
    return new HashSet<Extractor>(Arrays.asList(
        new Redirector(DIRTYSTRUCTUREFACTS, REDIRECTEDSTRUCTUREFACTS, this, decodeLang(this.wikipedia.getName())),
        new TypeChecker(REDIRECTEDSTRUCTUREFACTS, STRUCTUREFACTS, this)));
  }

  /** Facts representing the Wikipedia structure (e.g. links) */
  public static final Theme DIRTYSTRUCTUREFACTS = new Theme("structureFactsNeedTypeCheckingRedirecting", "Regular structure from Wikipedia, e.g. links - needs redirecting and typechecking");

  /** Facts representing the Wikipedia structure (e.g. links) */
  public static final Theme REDIRECTEDSTRUCTUREFACTS = new Theme("structureFactsNeedTypeChecking", "Regular structure from Wikipedia, e.g. links - needs typechecking");

  /** Facts representing the Wikipedia structure (e.g. links) */
  public static final Theme STRUCTUREFACTS = new Theme("structureFacts", "Regular structure from Wikipedia, e.g. links");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(DIRTYSTRUCTUREFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting structure facts");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor(input);

		FactCollection structurePatternCollection = new FactCollection(
				input.get(PatternHardExtractor.STRUCTUREPATTERNS));
		FactTemplateExtractor structurePatterns = new FactTemplateExtractor(structurePatternCollection,
				"<_extendedStructureWikiPattern>");

		FactWriter out = output.get(DIRTYSTRUCTUREFACTS);

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
        String normalizedPage = page.replaceAll("[\\s\\x00-\\x1F]+", " ");

				for (Fact fact : structurePatterns.extract(normalizedPage, titleEntity)) {
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
