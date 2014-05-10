package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import utils.PatternList;
import utils.TermExtractor;
import basics.BaseTheme;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import followUp.Redirector;
import followUp.Translator;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.InfoboxExtractor;

/**
 * YAGO2s - InfoboxTermExtractor
 * 
 * Extracts the terms from the
 * 
 * @author Fabian
 * 
 */
public class InfoboxTermExtractor extends MultilingualExtractor {

	public static final BaseTheme INFOBOXTERMS = new BaseTheme("infoboxTerms",
			"The attribute facts of the Wikipedia infoboxes, split into terms");
	public static final BaseTheme INFOBOXTERMS_TOREDIRECT = new BaseTheme(
			"infoboxTermsToBeRedirected",
			"The attribute facts of the Wikipedia infoboxes, split into terms, still to be redirected.");
	public static final BaseTheme INFOBOXATTSTRANSLATED = new BaseTheme(
			"infoboxAttributesTranslated",
			"The attribute facts of the Wikipedia infoboxes, split into terms, redirected, subject translated");

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				WordnetExtractor.WORDNETWORDS, HardExtractor.HARDWIREDFACTS,
				InfoboxExtractor.INFOBOX_ATTRIBUTES.inLanguage(this.language)));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				INFOBOXTERMS_TOREDIRECT.inLanguage(this.language));
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(PatternHardExtractor.INFOBOXPATTERNS,
				WordnetExtractor.PREFMEANINGS);
	}

	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(
				new Redirector(INFOBOXTERMS_TOREDIRECT
						.inLanguage(this.language), INFOBOXTERMS
						.inLanguage(this.language), this, this.language),
				new Translator(INFOBOXTERMS.inLanguage(language),
						INFOBOXATTSTRANSLATED.inLanguage(this.language),
						this.language, Translator.ObjectType.Entity)));
	}

	@Override
	public void extract() throws Exception {
		PatternList replacements = new PatternList(
				PatternHardExtractor.INFOBOXPATTERNS.factCollection(),
				"<_infoboxReplace>");
		Map<String, String> preferredMeanings = WordnetExtractor.PREFMEANINGS
				.factCollection().getPreferredMeanings();
		for (Fact f : InfoboxExtractor.INFOBOX_ATTRIBUTES.inLanguage(
				this.language).factSource()) {
			String val = f.getObjectAsJavaString();
			val = replacements.transform(Char.decodeAmpersand(val));
			val = val
					.replace("$0", FactComponent.stripBrackets(f.getSubject()));
			val = val.trim();
			if (val.length() == 0)
				continue;
			for (TermExtractor extractor : TermExtractor.all(preferredMeanings)) {
				List<String> objects = extractor.extractList(val);
				for (String object : objects) {
					INFOBOXTERMS_TOREDIRECT.inLanguage(language).write(
							new Fact(f.getSubject(), f.getRelation(), object));
				}
			}
		}
	}

	public InfoboxTermExtractor(String lang) {
		super(lang);
	}

	public static void main(String[] args) throws Exception {
		InfoboxTermExtractor extractor = new InfoboxTermExtractor("en");
		extractor.extract(new File("/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");
	}

}
