package fromThemes;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import utils.MultilingualTheme;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.RDFS;
import extractors.MultilingualExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.InfoboxExtractor;

/**
 * WikipediaTypeExtractor - YAGO2s
 * 
 * Extracts types from infoboxes
 * 
 * @author Fabian
 * 
 */
public class InfoboxTypeExtractor extends MultilingualExtractor {

	/** Infobox type facts */
	public static final MultilingualTheme INFOBOXTYPES = new MultilingualTheme(
			"infoboxTypes", "The infobox type facts");

	/** Sources for these facts */
	public static final MultilingualTheme INFOBOXTYPESOURCES = new MultilingualTheme(
			"infoboxTypeSources", "Sources for the infobox type facts");

	public Set<Theme> input() {
		if (isEnglish())
			return (new FinalSet<Theme>(
					InfoboxExtractor.INFOBOX_TEMPLATES.inLanguage(language),
					PatternHardExtractor.INFOBOXPATTERNS,
					WordnetExtractor.PREFMEANINGS));
		else
			return (new FinalSet<Theme>(
					InfoboxExtractor.INFOBOX_TEMPLATES_TRANSLATED
							.inLanguage(language),
					PatternHardExtractor.INFOBOXPATTERNS,
					WordnetExtractor.PREFMEANINGS));
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(PatternHardExtractor.INFOBOXPATTERNS);
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(INFOBOXTYPES.inLanguage(language),
				INFOBOXTYPESOURCES.inLanguage(language));
	}

	/** Holds the nonconceptual infoboxes */
	protected Set<String> nonConceptualInfoboxes;

	/** Holds the preferred meanings */
	protected Map<String, String> preferredMeanings;

	/** Maps a category to a wordnet class */
	public String infobox2class(String infoboxName) {
		NounGroup category = new NounGroup(infoboxName);
		if (category.head() == null) {
			Announce.debug("Could not find type in", infoboxName,
					"(has empty head)");
			return (null);
		}

		// If the category is an acronym, drop it
		if (Name.isAbbreviation(category.head())) {
			Announce.debug("Could not find type in", infoboxName,
					"(is abbreviation)");
			return (null);
		}
		category = new NounGroup(infoboxName.toLowerCase());

		String stemmedHead = PlingStemmer.stem(category.head());

		// Exclude the bad guys
		if (nonConceptualInfoboxes.contains(stemmedHead)) {
			Announce.debug("Could not find type in", infoboxName,
					"(is non-conceptual)");
			return (null);
		}

		// Try all premodifiers (reducing the length in each step) + head
		if (category.preModifier() != null) {
			String wordnet = null;
			String preModifier = category.preModifier().replace('_', ' ');

			for (int start = 0; start != -1 && start < preModifier.length() - 2; start = preModifier
					.indexOf(' ', start + 1)) {
				wordnet = preferredMeanings
						.get((start == 0 ? preModifier : preModifier
								.substring(start + 1)) + " " + stemmedHead);
				// take the longest matching sequence
				if (wordnet != null)
					return (wordnet);
			}
		}

		// Try postmodifiers to catch "head of state"
		if (category.postModifier() != null && category.preposition() != null
				&& category.preposition().equals("of")) {
			String wordnet = preferredMeanings.get(stemmedHead + " of "
					+ category.postModifier().head());
			if (wordnet != null)
				return (wordnet);
		}

		// Try head
		String wordnet = preferredMeanings.get(stemmedHead);
		if (wordnet != null)
			return (wordnet);
		Announce.debug("Could not find type in", infoboxName, "(" + stemmedHead
				+ ") (no wordnet match)");
		return (null);
	}

	@Override
	public void extract() throws Exception {
		String typeRelation = FactComponent
				.forInfoboxTypeRelation(this.language);

		nonConceptualInfoboxes = PatternHardExtractor.INFOBOXPATTERNS
				.factCollection().collectObjects(RDFS.type,
						"<_yagoNonConceptualInfobox>");
		preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection()
				.getPreferredMeanings();

		FactSource in = isEnglish() ? InfoboxExtractor.INFOBOX_TEMPLATES
				.inLanguage(language)
				: InfoboxExtractor.INFOBOX_TEMPLATES_TRANSLATED
						.inLanguage(language);

		for (Fact f : in) {
			if (!f.getRelation().equals(typeRelation))
				continue;
			String clss = infobox2class(f.getObjectAsJavaString());
			if (clss == null)
				continue;
			write(INFOBOXTYPES.inLanguage(language), new Fact(f.getSubject(),
					RDFS.type, clss), INFOBOXTYPESOURCES.inLanguage(language),
					FactComponent.wikipediaSourceURL(f.getSubject(), language),
					FactComponent.forString("Infobox template extractor from "
							+ f.getObject()));
		}
		this.nonConceptualInfoboxes = null;
		this.preferredMeanings = null;
	}

	public InfoboxTypeExtractor(String lang) {
		super(lang);
	}

	public static void main(String[] args) throws Exception {
		new InfoboxTypeExtractor("en").extract(
				new File("c:/fabian/data/yago3"), "Test");
	}

}
