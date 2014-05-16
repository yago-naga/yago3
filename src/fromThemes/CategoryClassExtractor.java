package fromThemes;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import extractors.Extractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryExtractor;

/**
 * CategoryClassExtractor - YAGO2s
 * 
 * Extracts classes from the English category membership facts.
 * 
 * @author Fabian
 * 
 */
public class CategoryClassExtractor extends Extractor {

	/** Classes deduced from categories with their connection to WordNet */
	public static final Theme CATEGORYCLASSES = new Theme(
			"categoryClasses",
			"Classes derived from the Wikipedia categories, with their connection to the WordNet class hierarchy leaves");

	public Set<Theme> input() {
		return (new FinalSet<Theme>(PatternHardExtractor.CATEGORYPATTERNS,
				WordnetExtractor.PREFMEANINGS,
				CategoryExtractor.CATEGORYMEMBERS.inEnglish()));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(CATEGORYCLASSES);
	}

	/** Holds the nonconceptual categories */
	protected Set<String> nonConceptualCategories;

	/** Holds the preferred meanings */
	protected Map<String, String> preferredMeanings;

	/** Maps a category to a wordnet class */
	public String category2class(String categoryName) {
		// Check out whether the new category is worth being added
		NounGroup category = new NounGroup(categoryName);
		if (category.head() == null) {
			Announce.debug("Could not find type in", categoryName,
					"(has empty head)");
			return (null);
		}

		// If the category is an acronym, drop it
		if (Name.isAbbreviation(category.head())) {
			Announce.debug("Could not find type in", categoryName,
					"(is abbreviation)");
			return (null);
		}
		category = new NounGroup(categoryName.toLowerCase());

		// Only plural words are good hypernyms
		if (PlingStemmer.isSingular(category.head())
				&& !category.head().equals("people")) {
			Announce.debug("Could not find type in", categoryName,
					"(is singular)");
			return (null);
		}
		String stemmedHead = PlingStemmer.stem(category.head());

		// Exclude the bad guys
		if (nonConceptualCategories.contains(stemmedHead)) {
			Announce.debug("Could not find type in", categoryName,
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
		Announce.debug("Could not find type in", categoryName, "("
				+ stemmedHead + ") (no wordnet match)");
		return (null);
	}

	/**
	 * Extracts the statement subclassOf(category name, wordnetclass)
	 * 
	 * @param classWriter
	 */
	protected void extractClassStatement(String category, String titleEntity)
			throws IOException {
		String concept = category2class(category);
		if (concept == null)
			return;
		CATEGORYCLASSES.write(new Fact(FactComponent.forWikiCategory(category),
				RDFS.subclassOf, concept));
		String name = new NounGroup(category).stemmed().replace('_', ' ');
		if (!name.isEmpty())
			CATEGORYCLASSES.write(new Fact(null, FactComponent
					.forWikiCategory(category), RDFS.label, FactComponent
					.forStringWithLanguage(name, "eng")));
	}

	@Override
	public void extract() throws Exception {
		nonConceptualCategories = PatternHardExtractor.CATEGORYPATTERNS
				.factCollection().seekStringsOfType("<_yagoNonConceptualWord>");
		preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection()
				.getPreferredMeanings();

		// Holds the categories we already did
		Set<String> categoriesDone = new HashSet<>();

		// Extract the information
		for (Fact f : CategoryExtractor.CATEGORYMEMBERS.inEnglish()) {
			if (!f.getRelation().equals("<hasWikiCategory>"))
				continue;
			String category = f.getArgJavaString(2);
			if (categoriesDone.contains(category))
				continue;
			categoriesDone.add(category);
			extractClassStatement(category, f.getArg(1));
		}
		this.nonConceptualCategories = null;
		this.preferredMeanings = null;
	}

	public static void main(String[] args) throws Exception {
		new CategoryClassExtractor().extract(new File("c:/fabian/data/yago3"),
				"Test");
	}

}
