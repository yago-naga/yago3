package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * CategoryExtractor - YAGO2s
 * 
 * Extracts facts from Wikipedia categories.
 * 
 * @author Fabian
 * @author Farzaneh Mahdisoltani
 * 
 */

public class CategoryExtractor extends Extractor {

	protected File wikipedia;

	public static final Theme CATEGORYMEMBERS = new Theme(
			"categoryMembers",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

	public static final Theme CATEGORYMEMBERS_SOURCES = new Theme(
			"categoryMemberSources",
			"Sources for the facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

	public static final Theme CATEGORYMEMBERS_TRANSLATED = new Theme(
			"categoryMembersTranslated",
			"Category Members facts with translated subjects and objects");

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(
				PatternHardExtractor.CATEGORYPATTERNS,
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS,
				InterLanguageLinks.INTERLANGUAGELINKS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				CATEGORYMEMBERS_SOURCES.inLanguage(language),
				CATEGORYMEMBERS.inLanguage(language));
	}

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(new Translator(
				CATEGORYMEMBERS.inLanguage(this.language),
				CATEGORYMEMBERS_TRANSLATED.inLanguage(this.language),
				this.language, "Category"), new CategoryMapper(this.language),
				new CategoryTypeExtractor(this.language)));
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers,
			Map<Theme, FactSource> input) throws Exception {
		TitleExtractor titleExtractor = new TitleExtractor(input);

		// Extract the information
		// Announce.progressStart("Extracting", 3_900_000);
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		/**
		 * categoryWord holds the synonym of the word "Category" in
		 * different languages. It is needed to distinguish the category
		 * part in Wiki pages.
		 */
		String categoryWord = InterLanguageLinksDictionary.getCatDictionary(
				input.get(InterLanguageLinks.INTERLANGUAGELINKS)).get(language);
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:", "[["
					+ categoryWord + ":")) {
			case -1:
				// Announce.progressDone();
				in.close();
				return;
			case 0:
				// Announce.progressStep();
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 1:
			case 2:

				if (titleEntity == null) {
					continue;
				}
				String category = FileLines.readTo(in, ']', '|').toString();
				category = category.trim();
				write(writers, CATEGORYMEMBERS.inLanguage(language), new Fact(
						titleEntity, "<hasWikiCategory/" + this.language + ">",
						FactComponent.forString(category)),
						CATEGORYMEMBERS_SOURCES.inLanguage(language),
						FactComponent.wikipediaURL(titleEntity),
						"CategoryExtractor");
				break;
			case 3:
				titleEntity = null;
				break;
			}
		}
	}

	/**
	 * Finds the language from the name of the input file, assuming that the
	 * first part of the name before the underline is equal to the language
	 */
	public static String decodeLang(String fileName) {
		if (!fileName.contains("_"))
			return "en";
		return fileName.split("_")[0];
	}

	/** Constructor from source file */
	public CategoryExtractor(File wikipedia) {
		this(wikipedia, decodeLang(wikipedia.getName()));
	}

	public CategoryExtractor(File wikipedia, String lang) {
		this.wikipedia = wikipedia;
		this.language = lang;
	}

	public static void main(String[] args) throws Exception {

		new CategoryExtractor(new File("D:/en_wikitest.xml")).extract(new File(
				"D:/data3/yago2s"), "Test on 1 wikipedia article");

	}

}
