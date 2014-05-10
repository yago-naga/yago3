package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.TitleExtractor;
import basics.BaseTheme;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import extractors.Extractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.Translator;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CategoryMapper;
import fromThemes.CategoryTypeExtractor;

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

	public static final BaseTheme CATEGORYMEMBERS = new BaseTheme(
			"categoryMembers",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

	public static final BaseTheme CATEGORYMEMBERS_SOURCES = new BaseTheme(
			"categoryMemberSources",
			"Sources for the facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

	public static final BaseTheme CATEGORYMEMBERS_TRANSLATED = new BaseTheme(
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
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(new Translator(
				CATEGORYMEMBERS.inLanguage(this.language),
				CATEGORYMEMBERS_TRANSLATED.inLanguage(this.language),
				this.language, Translator.ObjectType.Category),
				new CategoryMapper(this.language), new CategoryTypeExtractor(
						this.language)));
	}

	@Override
	public void extract() throws Exception {
		TitleExtractor titleExtractor = new TitleExtractor(language);

		// Extract the information
		// Announce.progressStart("Extracting", 3_900_000);
		Reader in = FileUtils.getBufferedUTF8Reader(inputData);
		String titleEntity = null;
		/**
		 * categoryWord holds the synonym of the word "Category" in different
		 * languages. It is needed to distinguish the category part in Wiki
		 * pages.
		 */
		String categoryWord = DictionaryExtractor.CATEGORYWORDS
				.factCollection().getObject(FactComponent.forString(language),
						"<hasCategoryWord>");
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
				write(CATEGORYMEMBERS.inLanguage(language), new Fact(
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

	public CategoryExtractor(String lang, File wikipedia) {
		super(lang, wikipedia);
	}

	public static void main(String[] args) throws Exception {

		new CategoryExtractor("en", new File("D:/en_wikitest.xml")).extract(
				new File("D:/data3/yago2s"), "Test on 1 wikipedia article");

	}

}
