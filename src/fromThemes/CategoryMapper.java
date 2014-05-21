package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.datatypes.FinalSet;
import utils.FactTemplateExtractor;
import utils.MultilingualTheme;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import extractors.MultilingualExtractor;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.CategoryExtractor;

/**
 * CategoryMapper - YAGO2s
 * 
 * Maps the facts obtained from CategoryExtractor (Previously translated for
 * other languages) to YAGO facts.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */
public class CategoryMapper extends MultilingualExtractor {

	public static final MultilingualTheme CATEGORYFACTS_TOREDIRECT = new MultilingualTheme(
			"categoryFactsToBeRedirected",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected");

	public static final MultilingualTheme CATEGORYFACTS_TOTYPECHECK = new MultilingualTheme(
			"categoryFactsToBeTypechecked",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");

	public static final MultilingualTheme CATEGORYFACTS = new MultilingualTheme(
			"categoryFacts",
			"Facts about Wikipedia instances, derived from the Wikipedia categories");

	public static final MultilingualTheme CATEGORYSOURCES = new MultilingualTheme(
			"categorySources",
			"Sources for the facts about Wikipedia instances, derived from the Wikipedia categories");

	@Override
	public Set<Theme> inputCached() {
		return (new FinalSet<>(PatternHardExtractor.CATEGORYPATTERNS));
	}

	@Override
	public Set<Theme> input() {
		Set<Theme> result = new HashSet<Theme>(
				Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS));
		if (isEnglish())
			result.add(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language));
		else
			result.add(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED
					.inLanguage(language));
		return (result);
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				CATEGORYFACTS_TOREDIRECT.inLanguage(language),
				CATEGORYSOURCES.inLanguage(language));
	}

	@Override
	public Set<FollowUpExtractor> followUp() {
		return new FinalSet<FollowUpExtractor>(new Redirector(
				CATEGORYFACTS_TOREDIRECT.inLanguage(language),
				CATEGORYFACTS_TOTYPECHECK.inLanguage(language), this),
				new TypeChecker(CATEGORYFACTS_TOTYPECHECK.inLanguage(language),
						CATEGORYFACTS.inLanguage(language), this));
	}

	@Override
	public void extract() throws Exception {
		FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(
				PatternHardExtractor.CATEGORYPATTERNS.factCollection(),
				"<_categoryPattern>");

		FactSource factSource;
		if (isEnglish())
			factSource = CategoryExtractor.CATEGORYMEMBERS.inLanguage(language);
		else
			factSource = CategoryExtractor.CATEGORYMEMBERS_TRANSLATED
					.inLanguage(language);

		for (Fact f : factSource) {
			for (Fact fact : categoryPatterns.extract(
					FactComponent.stripCat(f.getObject()), f.getSubject())) {
				if (fact != null) {
					write(CATEGORYFACTS_TOREDIRECT.inLanguage(language), fact,
							CATEGORYSOURCES.inLanguage(language),
							FactComponent.wikipediaURL(f.getArg(1)),
							"CategoryMapper");
				}
			}
		}

	}

	/** Constructor from source file */
	public CategoryMapper(String lang) {
		super(lang);
	}

	public static void main(String[] args) throws Exception {
		new CategoryMapper("en").extract(new File("D:/data3/yago2s/"),
				"mapping categories into facts");
	}

}
