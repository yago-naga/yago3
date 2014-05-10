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
import utils.FactTemplateExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.Theme;
import extractors.EnglishWikipediaExtractor;
import extractors.Extractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;

/**
 * Extracts means facts from Wikipedia disambiguation pages
 * 
 * @author Johannes Hoffart
 * 
 */
public class DisambiguationPageExtractor extends EnglishWikipediaExtractor {

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(
				Arrays.asList(PatternHardExtractor.DISAMBIGUATIONTEMPLATES));
	}

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList((Extractor) new Redirector(
				DIRTYDISAMBIGUATIONMEANSFACTS,
				REDIRECTEDDISAMBIGUATIONMEANSFACTS, this,
				decodeLang(this.inputData.getName())),
				(Extractor) new TypeChecker(REDIRECTEDDISAMBIGUATIONMEANSFACTS,
						DISAMBIGUATIONMEANSFACTS, this)));
	}

	/** Means facts from disambiguation pages */
	public static final Theme DIRTYDISAMBIGUATIONMEANSFACTS = new Theme(
			"disambiguationMeansFactsDirty",
			"Means facts from disambiguation pages - needs redirecting and typechecking");

	/** Means facts from disambiguation pages */
	public static final Theme REDIRECTEDDISAMBIGUATIONMEANSFACTS = new Theme(
			"disambiguationMeansFactsRedirected",
			"Means facts from disambiguation pages - needs typechecking");

	/** Means facts from disambiguation pages */
	public static final Theme DISAMBIGUATIONMEANSFACTS = new Theme(
			"disambiguationMeansFacts", "Means facts from disambiguation pages");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(DIRTYDISAMBIGUATIONMEANSFACTS);
	}

	@Override
	public void extract() throws Exception {
		// Extract the information
		Announce.doing("Extracting disambiguation means");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(inputData);

		FactCollection disambiguationPatternCollection = PatternHardExtractor.DISAMBIGUATIONTEMPLATES
				.factCollection();
		FactTemplateExtractor disambiguationPatterns = new FactTemplateExtractor(
				disambiguationPatternCollection, "<_disambiguationPattern>");
		Set<String> templates = disambiguationTemplates(disambiguationPatternCollection);

		String titleEntity = null;
		String page = null;

		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = FileLines.readToBoundary(in, "</title>");
				titleEntity = cleanDisambiguationEntity(titleEntity);
				page = FileLines.readBetween(in, "<text", "</text>");

				if (titleEntity == null || page == null)
					continue;

				if (isDisambiguationPage(page, templates)) {
					for (Fact fact : disambiguationPatterns.extract(page,
							titleEntity)) {
						if (fact != null)
							DIRTYDISAMBIGUATIONMEANSFACTS.write(fact);
					}
				}
			}
		}
	}

	protected static String cleanDisambiguationEntity(String titleEntity) {
		if (titleEntity.indexOf("(disambiguation)") > -1) {
			titleEntity = titleEntity.substring(0,
					titleEntity.indexOf("(disambiguation)")).trim();
		}
		return titleEntity;
	}

	/** Returns the set of disambiguation templates */
	public static Set<String> disambiguationTemplates(
			FactCollection disambiguationTemplates) {
		return (disambiguationTemplates
				.seekStringsOfType("<_yagoDisambiguationTemplate>"));
	}

	private boolean isDisambiguationPage(String page, Set<String> templates) {
		for (String templName : templates) {
			if (page.contains(templName)
					|| page.contains(templName.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Needs Wikipedia as input
	 * 
	 * @param wikipedia
	 *            Wikipedia XML dump
	 */
	public DisambiguationPageExtractor(File wikipedia) {
		super(wikipedia);
	}

	public static void main(String[] args) throws Exception {
		String s = "Regular Title";
		String correct = "Regular Title";
		s = DisambiguationPageExtractor.cleanDisambiguationEntity(s);
		if (!s.equals(correct)) {
			System.out.println("Expected: " + correct + ". Value: " + s);
		}

		s = "Regular Title (disambiguation)";
		s = DisambiguationPageExtractor.cleanDisambiguationEntity(s);
		if (!s.equals(correct)) {
			System.out.println("Expected: " + correct + ". Value: " + s);
		}

		s = "Regular Title (disambiguation). ";
		s = DisambiguationPageExtractor.cleanDisambiguationEntity(s);
		if (!s.equals(correct)) {
			System.out.println("Expected: " + correct + ". Value: " + s);
		}

		System.out.println("Done.");
	}

}
