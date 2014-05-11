package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.MultilingualTheme;
import basics.Theme;
import extractors.MultilingualWikipediaExtractor;
import followUp.CategoryTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.DictionaryExtractor;
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

public class CategoryExtractor extends MultilingualWikipediaExtractor {

	public static final MultilingualTheme CATEGORYMEMBERS = new MultilingualTheme(
			"categoryMembers",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

	public static final MultilingualTheme CATEGORYMEMBERS_SOURCES = new MultilingualTheme(
			"categoryMemberSources",
			"Sources for the facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

	public static final MultilingualTheme CATEGORYMEMBERS_TRANSLATED = new MultilingualTheme(
			"categoryMembersTranslated",
			"Category Members facts with translated subjects and objects");

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.PREFMEANINGS,
				DictionaryExtractor.CATEGORYWORDS));
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(WordnetExtractor.PREFMEANINGS,
				DictionaryExtractor.CATEGORYWORDS);
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				CATEGORYMEMBERS_SOURCES.inLanguage(language),
				CATEGORYMEMBERS.inLanguage(language));
	}

	@Override
	public Set<FollowUpExtractor> followUp() {
		if (language.equals("en"))
			return (Collections.emptySet());
		return (new FinalSet<FollowUpExtractor>(new CategoryTranslator(
				CATEGORYMEMBERS.inLanguage(this.language),
				CATEGORYMEMBERS_TRANSLATED.inLanguage(this.language), this)));
	}

	@Override
	public void extract() throws Exception {
		TitleExtractor titleExtractor = new TitleExtractor(language);

		// Extract the information
		// Announce.progressStart("Extracting", 3_900_000);
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		/**
		 * categoryWord holds the synonym of the word "Category" in different
		 * languages. It is needed to distinguish the category part in Wiki
		 * pages.
		 */
		String categoryWord = DictionaryExtractor.CATEGORYWORDS
				.factCollection().getObject(FactComponent.forString(language),
						"<_hasCategoryWord>");
		if (categoryWord == null)
			throw new Exception("Category word undefined in language "
					+ language);
		categoryWord = FactComponent.asJavaString(categoryWord);
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
				write(CATEGORYMEMBERS.inLanguage(language),
						new Fact(titleEntity, "<hasWikiCategory>",
								FactComponent.forForeignWikiCategory(category,
										language)),
						CATEGORYMEMBERS_SOURCES.inLanguage(language),
						FactComponent.wikipediaURL(titleEntity),
						"CategoryExtractor");
				break;
			}
		}
	}

	public CategoryExtractor(String lang, File wikipedia) {
		super(lang, wikipedia);
	}

	public static void main(String[] args) throws Exception {

		new CategoryExtractor(
				"de",
				new File(
						"C:/Fabian/eclipseProjects/yago2s/testCases/fromWikipedia.CategoryExtractor/de/wikipedia/de_wikitest.xml"))
				.extract(new File("c:/fabian/data/yago3"),
						"Test on 1 wikipedia article");

	}

}
