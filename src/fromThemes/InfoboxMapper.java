package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.FactCollection;
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

/**
 * Class InfoboxMapper - YAGO2S
 * 
 * Maps the facts in the output of InfoboxExtractor for different languages.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class InfoboxMapper extends MultilingualExtractor {

	public static final MultilingualTheme INFOBOXFACTS = new MultilingualTheme(
			"infoboxFacts", "Facts of infobox, redirected and type-checked");
	public static final MultilingualTheme INFOBOXFACTS_TOREDIRECT = new MultilingualTheme(
			"infoboxFactsToRedirect",
			"Facts of infobox to be redirected and type-checked");
	public static final MultilingualTheme INFOBOXFACTS_TOTYPECHECK = new MultilingualTheme(
			"infoboxFactsToCheck", "Facts of infobox to be type-checked");

	public static final MultilingualTheme INFOBOXSOURCES = new MultilingualTheme(
			"infoboxSources", "Sources of infobox facts");

	@Override
	public Set<FollowUpExtractor> followUp() {
		return new FinalSet<FollowUpExtractor>(new Redirector(
				INFOBOXFACTS_TOREDIRECT.inLanguage(language),
				INFOBOXFACTS_TOTYPECHECK.inLanguage(language), this),
				new TypeChecker(INFOBOXFACTS_TOTYPECHECK.inLanguage(language),
						INFOBOXFACTS.inLanguage(language), this));
	}

	@Override
	public Set<Theme> input() {
		if (isEnglish()) {
			return (new FinalSet<>(PatternHardExtractor.INFOBOXPATTERNS,
					InfoboxTermExtractor.INFOBOXTERMS.inLanguage(language)));
		} else {
			return (new FinalSet<>(
					AttributeMatcher.MATCHED_INFOBOXATTS.inLanguage(language),
					InfoboxTermExtractor.INFOBOXTERMSTRANSLATED
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
		FactSource input;
		// Get the infobox patterns depending on the language
		if (isEnglish()) {
			infoboxAttributeMappings = PatternHardExtractor.INFOBOXPATTERNS
					.factCollection();
			input = InfoboxTermExtractor.INFOBOXTERMS.inLanguage(language);
		} else {
			infoboxAttributeMappings = AttributeMatcher.MATCHED_INFOBOXATTS
					.inLanguage(language).factCollection();
			input = InfoboxTermExtractor.INFOBOXTERMSTRANSLATED
					.inLanguage(language);
		}
		Map<String, Set<String>> attribute2relations = new HashMap<>();
		for (Fact f : infoboxAttributeMappings
				.getFactsWithRelation("<_infoboxPattern>")) {
			D.addKeyValue(attribute2relations, f.getSubject().toLowerCase(),
					f.getObject(), HashSet.class);
		}
		for (Fact f : input) {
			Set<String> relations = attribute2relations.get(f.getRelation()
					.toLowerCase());
			if (relations == null)
				continue;
			for (String relation : relations) {
				Fact fact;
				if (relation.endsWith("->")) {
					relation = Char17.cutLast(Char17.cutLast(relation)) + '>';
					fact = new Fact(f.getObject(), relation, f.getSubject());
				} else {
					fact = new Fact(f.getSubject(), relation, f.getObject());
				}
				// Since the TermExtractor extracts everything also as a string,
				// we get subjects that are strings. This is always wrong.
				if (FactComponent.isLiteral(f.getSubject()))
					continue;
				String source = isEnglish() ? FactComponent.wikipediaURL(f
						.getSubject()) : FactComponent
						.wikipediaBaseURL(language);
				write(INFOBOXFACTS_TOREDIRECT.inLanguage(language), fact,
						INFOBOXSOURCES.inLanguage(language), source,
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