package fromThemes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.PatternList;
import utils.TermParser;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.MultilingualExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import followUp.Redirector;
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

	public static final MultilingualTheme INFOBOXTERMS = new MultilingualTheme(
			"infoboxTerms",
			"The attribute facts of the Wikipedia infoboxes, split into terms");
	public static final MultilingualTheme INFOBOXTERMS_TOREDIRECT = new MultilingualTheme(
			"infoboxTermsToBeRedirected",
			"The attribute facts of the Wikipedia infoboxes, split into terms, still to be redirected.");
	public static final MultilingualTheme INFOBOXTERMSTRANSLATED = new MultilingualTheme(
			"infoboxTermsTranslated",
			"The attribute facts of the Wikipedia infoboxes, split into terms, redirected, subject translated");

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				WordnetExtractor.PREFMEANINGS, HardExtractor.HARDWIREDFACTS,
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
	public Set<FollowUpExtractor> followUp() {
		Set<FollowUpExtractor> result = new HashSet<FollowUpExtractor>();
		result.add(new Redirector(INFOBOXTERMS_TOREDIRECT
				.inLanguage(this.language), INFOBOXTERMS
				.inLanguage(this.language), this));
		if (!isEnglish())
			result.add(new EntityTranslator(INFOBOXTERMS.inLanguage(language),
					INFOBOXTERMSTRANSLATED.inLanguage(this.language), this));
		return (result);
	}

	@Override
	public void extract() throws Exception {
		PatternList replacements = new PatternList(
				PatternHardExtractor.INFOBOXPATTERNS.factCollection(),
				"<_infoboxReplace>");
		Map<String, String> unitDictionary = PatternHardExtractor.INFOBOXPATTERNS.factCollection()
				.getMap("<_hasPredefinedUnit>");
		Map<String, String> preferredMeanings = WordnetExtractor.PREFMEANINGS
				.factCollection().getPreferredMeanings();

		List<TermParser> parsers = TermParser.allParsers(preferredMeanings,
				language);

		for (Fact f : InfoboxExtractor.INFOBOX_ATTRIBUTES
				.inLanguage(this.language)) {
			String val = f.getObjectAsJavaString();
			val = Char17.decodeAmpersand(val);
			// Sometimes we get empty values here
			if (val == null || val.isEmpty())
				continue;
			val = replacements.transform(val);
			val = val
					.replace("$0", FactComponent.stripBrackets(f.getSubject()));
			val = val.trim();
			if (val.length() == 0)
				continue;
			@ImplementationNote("This has to be a list, because the first date mentioned is usually the right one")
			List<String> objects = new ArrayList<>();
			for (TermParser termParser : parsers) {
				for(String s : termParser.extractList(val)) {
					
					//Add predefined units
					if (unitDictionary.containsKey(f.getRelation())) {
						String datatype = FactComponent.getDatatype(s);
						if (datatype.equals(YAGO.decimal) || datatype.equals(YAGO.integer)) {
							String value = FactComponent.getString(s);
							s = FactComponent.forStringWithDatatype(value, 
									unitDictionary.get(f.getRelation()));
						}
					}
					
					if(!objects.contains(s)) objects.add(s);
				}
			}
			for (String object : objects) {
				INFOBOXTERMS_TOREDIRECT.inLanguage(language).write(
						new Fact(f.getSubject(), f.getRelation(), object));
			}
		}
	}

	public InfoboxTermExtractor(String lang) {
		super(lang);
	}

	public static void main(String[] args) throws Exception {
		InfoboxTermExtractor extractor = new InfoboxTermExtractor("en");
		extractor.extract(new File("c:/fabian/data/yago3"),
				"mapping infobox attributes into infobox facts");
	}

}
