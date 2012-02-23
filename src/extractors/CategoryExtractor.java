package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.parsers.DateParser;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.N4Writer;
import basics.Theme;
import extractorUtils.PatternList;
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
		return new TreeSet<Theme>(Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS,PatternHardExtractor.TITLEPATTERNS,HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETWORDS));
	}

	/** Patterns of infoboxes*/
	public static final Theme CATEGORTYPES=new Theme("categoryTypes");
	/** Patterns of titles*/
	public static final Theme CATEGORYFACTS=new Theme("categoryFacts");

	@Override
	public Map<Theme,String> output() {
		return new FinalMap<Theme,String>(CATEGORTYPES, "Types derived from the categories", CATEGORYFACTS,  "Facts derived from the categories");
	}


	/** Extracts facts from the category name */
	protected void extractFacts(String titleEntity, String category, PatternList patterns,
			Map<String, String> objects, Map<Theme,N4Writer> writers) throws IOException {
		for (Pair<Pattern,String> pattern : patterns.patterns) {
			Matcher m = pattern.first.matcher(category);
			if (!m.matches())
				continue;
			// See whether we have a predefined pattern
			// Note: we cannot look up the pattern in the hash map
			// because patterns do not support equals().
			// Therefore, we look up the string behind the pattern.
			String object = objects.get(pattern.first.pattern());
			if (object == null) {
				object = DateParser.normalize(m.group(1).replace(' ', '_'));
				if (object.matches("\\d+"))
					object = FactComponent.forYear(object);
				else
					object = FactComponent.forAny(object);
			} else {
				if (m.groupCount() > 0)
					object = object.replace("$1", m.group(1));
				object = FactComponent.forAny(object.replace(' ', '_'));
			}
			Fact fact = new Fact(null, titleEntity, pattern.second, object);
			if (fact.relation.equals("rdf:type"))
				writers.get(CATEGORTYPES).write(fact);
			else
				writers.get(CATEGORYFACTS).write(fact);
		}
	}

	/** Maps a category to a wordnet class */
	public static String category2class(String categoryName, Set<String> nonconceptual, Map<String, String> preferredMeaning) {
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

	/** Extracts type from the category name */
	protected void extractType(String titleEntity, String category, N4Writer writer, Set<String> nonconceptual,
			Map<String, String> preferredMeaning) throws IOException {
		String concept = category2class(category, nonconceptual, preferredMeaning);
		if (concept == null)
			return;
		writer.write(new Fact(null, titleEntity, FactComponent.forQname("rdf:", "type"), concept));
	}

	public static Set<String> nonConceptualWords(Map<Theme,FactCollection> factCollections) {
		return(factCollections.get(PatternHardExtractor.CATEGORYPATTERNS).asStringSet("<_yagoNonConceptualWord>"));
	}
	
	@Override
	public void extract(Map<Theme,N4Writer> writers, Map<Theme,FactCollection> factCollections) throws Exception {

		PatternList categoryPatterns=new PatternList(factCollections.get(PatternHardExtractor.CATEGORYPATTERNS),"<_categoryPattern>");
		Map<String, String> objects = factCollections.get(PatternHardExtractor.CATEGORYPATTERNS).asStringMap("<_categoryObject>");
		Set<String> nonconceptual = nonConceptualWords(factCollections);
		Map<String,String> preferredMeanings=WordnetExtractor.preferredMeanings(factCollections.values());
        TitleExtractor titleExtractor=new TitleExtractor(factCollections);
        
		// Extract the information
		Announce.doing("Extracting");
		Reader in = new BufferedReader(new FileReader(wikipedia));
		String titleEntity = null;
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 1:
				if (titleEntity == null)
					continue;
				String category = FileLines.readTo(in, "]]").toString();
				if (!category.endsWith("]]"))
					continue;
				category = category.substring(0, category.length() - 2);
				extractFacts(titleEntity, category, categoryPatterns, objects, writers);
				extractType(titleEntity, category, writers.get(CATEGORTYPES), nonconceptual, preferredMeanings);
			}
		}
	}

	/** Constructor from source file */
	public CategoryExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		// new PatternHardExtractor(new File("./data")).extract(new
		// File("../../yago2/newfacts"),
		// "Test on 1 wikipedia article");
		new CategoryExtractor(new File("./testCases/wikitest.xml")).extract(new File("../../yago2/newfacts"),
				"Test on 1 wikipedia article");
	}
}
