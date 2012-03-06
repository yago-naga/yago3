package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractorUtils.FactTemplateExtractor;
import extractorUtils.TitleExtractor;

/**
 * CategoryExtractor - YAGO2s
 * 
 * Extracts facts and types from categories
 * 
 * @author Fabian
 * 
 */
public class CategoryExtractor extends Extractor {

	/** The file from which we read */
	protected File wikipedia;

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS,
				PatternHardExtractor.TITLEPATTERNS, HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETWORDS));
	}

	/** Types deduced from categories */
	public static final Theme CATEGORYTYPES = new Theme("categoryTypes");
	/** Facts deduced from categories */
	public static final Theme CATEGORYFACTS = new Theme("categoryFacts");
	/** Classes deduced from categories */
	public static final Theme CATEGORYCLASSES = new Theme("categoryClasses");

	@Override
	public Map<Theme, String> output() {
		return new FinalMap<Theme, String>(CATEGORYTYPES, "Types derived from the categories", CATEGORYFACTS,
				"Facts derived from the categories", CATEGORYCLASSES, "Classes derived from the categories");
	}

	/** Maps a category to a wordnet class */
	public static String category2class(String categoryName, Set<String> nonconceptual,
			Map<String, String> preferredMeaning) {
		// Check out whether the new category is worth being added
		NounGroup category = new NounGroup(categoryName);
		if (category.head() == null) {
			Announce.debug("Could not find type in", categoryName, "(has empty head)");
			return (null);
		}

		// If the category is an acronym, drop it
		if (Name.isAbbreviation(category.head())) {
			Announce.debug("Could not find type in", categoryName, "(is abbreviation)");
			return (null);
		}
		category = new NounGroup(categoryName.toLowerCase());

		// Only plural words are good hypernyms
		if (PlingStemmer.isSingular(category.head()) && !category.head().equals("people")) {
			Announce.debug("Could not find type in", categoryName, "(is singular)");
			return (null);
		}
		String stemmedHead = PlingStemmer.stem(category.head());

		// Exclude the bad guys
		if (nonconceptual.contains(stemmedHead)) {
			Announce.debug("Could not find type in", categoryName, "(is non-conceptual)");
			return (null);
		}

		// Try premodifier + head
		if (category.preModifier() != null) {
			String wordnet = preferredMeaning.get(category.preModifier() + ' ' + stemmedHead);
			if (wordnet != null)
				return (wordnet);
		}
		// Try head
		String wordnet = preferredMeaning.get(stemmedHead);
		if (wordnet != null)// && wordnet.startsWith("<wordnet_"))
			return (wordnet);
		Announce.debug("Could not find type in", categoryName, stemmedHead, "(no wordnet match)");
		return (null);
	}

	/**
	 * Extracts type from the category name
	 * 
	 * @param classWriter
	 */
	protected void extractType(String titleEntity, String category, FactCollection facts, Set<String> nonconceptual,
			Map<String, String> preferredMeaning) throws IOException {
		String concept = category2class(category, nonconceptual, preferredMeaning);
		if (concept == null)
			return;
		facts.add(new Fact(null, titleEntity, RDFS.type, FactComponent.forWikiCategory(category)));
		facts.add(new Fact(null, FactComponent.forWikiCategory(category), RDFS.subclassOf, concept));
	}

	/** Returns the set of non-conceptual words */
	public static Set<String> nonConceptualWords(FactCollection categoryPatterns) {
		return (categoryPatterns.asStringSet("<_yagoNonConceptualWord>"));
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {

		FactCollection categoryPatternCollection = new FactCollection(input.get(PatternHardExtractor.CATEGORYPATTERNS));
		FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(categoryPatternCollection,
				"<_categoryPattern>");
		Set<String> nonconceptual = nonConceptualWords(categoryPatternCollection);
		Map<String, String> preferredMeanings = WordnetExtractor.preferredMeanings(
				new FactCollection(input.get(HardExtractor.HARDWIREDFACTS)),
				new FactCollection(input.get(WordnetExtractor.WORDNETWORDS)));
		TitleExtractor titleExtractor = new TitleExtractor(input);

		// Extract the information
		Announce.doing("Extracting");
		Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		String titleEntity = null;
		FactCollection facts = new FactCollection();
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:")) {
			case -1:
				flush(facts, writers);
				Announce.done();
				in.close();
				return;
			case 0:
				flush(facts, writers);
				titleEntity = titleExtractor.getTitleEntity(in);
				facts = new FactCollection();
				if (titleEntity != null) {
					for (String name : namesOf(titleEntity)) {
						facts.add(new Fact(titleEntity, RDFS.label, name));
					}
				}
				break;
			case 1:
				if (titleEntity == null)
					continue;
				String category = FileLines.readTo(in, "]]").toString();
				if (!category.endsWith("]]"))
					continue;
				category = category.substring(0, category.length() - 2);
				for (Fact fact : categoryPatterns.extract(category, titleEntity)) {
					if (fact != null)
						facts.add(fact);
				}
				extractType(titleEntity, category, facts, nonconceptual, preferredMeanings);
			}
		}
	}

	/** Writes the facts */
	public static void flush(FactCollection facts, Map<Theme, FactWriter> writers) throws IOException {
		if (facts.get("rdf:type").isEmpty())
			return;
		for (Fact fact : facts) {
			switch (fact.getRelation()) {
			case RDFS.type:
				writers.get(CATEGORYTYPES).write(fact);
				break;
			case RDFS.subclassOf:
				writers.get(CATEGORYCLASSES).write(fact);
				break;
			default:
				writers.get(CATEGORYFACTS).write(fact);
			}
		}
	}

	/** returns the (trivial) names of an entity */
	public static Set<String> namesOf(String titleEntity) {
		Set<String> result = new TreeSet<>();
		if (titleEntity.startsWith("<"))
			titleEntity = titleEntity.substring(1);
		if (titleEntity.endsWith(">"))
			titleEntity = Char.cutLast(titleEntity);
		String name = Char.decode(titleEntity.replace('_', ' '));
		result.add(FactComponent.forString(name));
		result.add(FactComponent.forString(Char.normalize(name)));
		if (name.contains(" (")) {
			result.add(FactComponent.forString(name.substring(0, name.indexOf(" (")).trim()));
		}
		if (name.contains(",")) {
			result.add(FactComponent.forString(name.substring(0, name.indexOf(",")).trim()));
		}
		return (result);
	}

	/** Constructor from source file */
	public CategoryExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new PatternHardExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"),
				"Test on 1 wikipedia article");
		new CategoryExtractor(new File("./testCases/wikitest.xml")).extract(new File("c:/fabian/data/yago2s"),
				"Test on 1 wikipedia article");
	}
}
