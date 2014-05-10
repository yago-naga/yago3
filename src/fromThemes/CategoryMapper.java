package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.datatypes.FinalSet;
import utils.FactTemplateExtractor;
import basics.BaseTheme;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.Theme;
import extractors.Extractor;
import extractors.MultilingualExtractor;
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

	public static final BaseTheme CATEGORYFACTS_TOREDIRECT = new BaseTheme(
			"categoryFactsToBeRedirected",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be redirected");

	public static final BaseTheme CATEGORYFACTS_TOTYPECHECK = new BaseTheme(
			"categoryFactsToBeTypechecked",
			"Facts about Wikipedia instances, derived from the Wikipedia categories, still to be typechecked");

	public static final BaseTheme CATEGORYFACTS = new BaseTheme(
			"categoryFacts",
			"Facts about Wikipedia instances, derived from the Wikipedia categories");

	public static final BaseTheme CATEGORYSOURCES = new BaseTheme(
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
		if (language.equals("en"))
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
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(new Redirector(
				CATEGORYFACTS_TOREDIRECT.inLanguage(language),
				CATEGORYFACTS_TOTYPECHECK.inLanguage(language), this,
				this.language),
				new TypeChecker(CATEGORYFACTS_TOTYPECHECK.inLanguage(language),
						CATEGORYFACTS.inLanguage(language), this)));
	}

	@Override
	public void extract() throws Exception {
		FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(
				PatternHardExtractor.CATEGORYPATTERNS.factCollection(),
				"<_categoryPattern>");

		FactSource factSource;
		if (language.equals("en"))
			factSource = CategoryExtractor.CATEGORYMEMBERS.inLanguage(language)
					.factSource();
		else
			factSource = CategoryExtractor.CATEGORYMEMBERS_TRANSLATED
					.inLanguage(language).factSource();

		for (Fact f : factSource) {
			for (Fact fact : categoryPatterns.extract(
					FactComponent.stripQuotes(f.getArg(2).replace('_', ' ')),
					f.getArg(1))) {
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
