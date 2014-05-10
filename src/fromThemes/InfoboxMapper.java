package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import basics.BaseTheme;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.Theme;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;

/**
 * Class InfoboxMapper - YAGO2S
 * 
 * Maps the facts in the output of InfoboxExtractor for different languages.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxMapper extends MultilingualExtractor {

	public static final BaseTheme INFOBOXFACTS = new BaseTheme("infoboxFacts",
			"Facts of infobox, redirected and type-checked");
	public static final BaseTheme INFOBOXFACTS_TOREDIRECT = new BaseTheme(
			"infoboxFactsToRedirect",
			"Facts of infobox to be redirected and type-checked");
	public static final BaseTheme INFOBOXFACTS_TOTYPECHECK = new BaseTheme(
			"infoboxFactsToCheck", "Facts of infobox to be type-checked");

	public static final BaseTheme INFOBOXSOURCES = new BaseTheme(
			"infoboxSources", "Sources of infobox facts");

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(new Redirector(
				INFOBOXFACTS_TOREDIRECT.inLanguage(language),
				INFOBOXFACTS_TOTYPECHECK.inLanguage(language), this,
				this.language),
				new TypeChecker(INFOBOXFACTS_TOTYPECHECK.inLanguage(language),
						INFOBOXFACTS.inLanguage(language), this)));
	}

	@Override
	public Set<Theme> input() {
		if (language.equals("en")) {
			return (new FinalSet<>(PatternHardExtractor.INFOBOXPATTERNS,
					InfoboxTermExtractor.INFOBOXATTSTRANSLATED
							.inLanguage(language)));
		} else {
			return (new FinalSet<>(
					AttributeMatcher.MATCHED_INFOBOXATTS.inLanguage(language),
					InfoboxTermExtractor.INFOBOXATTSTRANSLATED
							.inLanguage(language)));
		}
	}

	@Override
	public Set<Theme> output() {
		return new HashSet<>(Arrays.asList(
				INFOBOXFACTS_TOREDIRECT.inLanguage(language),
				INFOBOXSOURCES.inLanguage(language)));
	}

	@Override
	public void extract() throws Exception {

		FactCollection infoboxAttributeMappings;

		// Get the infobox patterns depending on the language
		if (this.language.equals("en")) {
			infoboxAttributeMappings = PatternHardExtractor.INFOBOXPATTERNS
					.factCollection();
		} else {
			infoboxAttributeMappings = AttributeMatcher.MATCHED_INFOBOXATTS
					.inLanguage(language).factCollection();
		}

		for (Fact f : InfoboxTermExtractor.INFOBOXATTSTRANSLATED.inLanguage(
				language).factSource()) {

			Set<String> relations = infoboxAttributeMappings.collectObjects(
					f.getRelation(), "<_infoboxPattern>");
			if (relations.isEmpty())
				continue;

			for (String relation : relations) {
				Fact fact;
				if (relation.endsWith("->")) {
					relation = Char.cutLast(Char.cutLast(relation)) + '>';
					fact = new Fact(f.getObject(), relation, f.getSubject());
				} else {
					fact = new Fact(f.getSubject(), relation, f.getObject());
				}
				write(INFOBOXFACTS_TOREDIRECT.inLanguage(language), fact,
						INFOBOXSOURCES.inLanguage(language),
						FactComponent.wikipediaURL(f.getSubject()),
						"InfoboxExtractor from " + f.getRelation());
			}
		}

	}

	public InfoboxMapper(String lang) {
		super(lang);
	}

	public static void main(String[] args) throws Exception {
		InfoboxMapper extractor = new InfoboxMapper("en");
		extractor.extract(new File("/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");
	}

}