package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.N4Writer;

/**
 * CategoryExtractor - YAGO2s
 * 
 * Extracts facts and types from categories
 * 
 * @author Fabian
 * 
 */
public class CategoryExtractor extends Extractor {

	protected File wikipedia;

	@Override
	public List<String> input() {
		return Arrays.asList("categoryPatterns", "titlePatterns", "hardWiredFacts");
	}

	@Override
	public List<String> output() {
		return Arrays.asList("categoryTypes", "categoryFacts");
	}

	@Override
	public List<String> outputDescriptions() {
		return Arrays.asList("Types derived from the categories", "Facts derived from the categories");
	}

	/** Extracts facts from the category name */
	protected void extractFacts(String titleEntity, String category, Map<Pattern, String> patterns,
			Map<Pattern, String> objects, List<N4Writer> writers) throws IOException {
		for (Pattern pattern : patterns.keySet()) {
			Matcher m = pattern.matcher(category);
			if (m.matches()) {
				String object = objects.get(pattern);
				if (object == null)
					object = m.group(1);
				else
					object = FactComponent.stripQuotes(object).replace("$1", m.group(1));
				Fact fact = new Fact(null, titleEntity, patterns.get(pattern), FactComponent.forYagoEntity(object));
				if (fact.relation.equals("rdf:type"))
					writers.get(0).write(fact);
				else
					writers.get(1).write(fact);
			}
		}

	}

	/** Maps a category to a wordnet class */
	protected String category2class(String categoryName, Set<String> nonconceptual, Map<String, String> preferredMeaning) {
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
		if (wordnet != null && wordnet.startsWith("wordnet_"))
			return (wordnet);
		Announce.debug("Could not find type in", categoryName, "(no wordnet match)");
		return (null);
	}

	/** Extracts facts from the category name */
	protected void extractType(String titleEntity, String category, N4Writer writer, Set<String> nonconceptual, Map<String, String> preferredMeaning) throws IOException {
       String concept=category2class(category, nonconceptual, preferredMeaning);
       if(concept==null) return;
       writer.write(new Fact(null,titleEntity,FactComponent.forQname("rdf:","type"),FactComponent.forYagoEntity(concept)));
	}

	/** Reads the title entity, supposes that the reader is after "<title>" */
	public static String getTitleEntity(Reader in, FactCollection titlePatterns) throws IOException {
		if (!FileLines.scrollTo(in, "<title>"))
			return (null);
		String title = FileLines.readTo(in, "</title>").toString();
		for (Fact pattern : titlePatterns.get("<_wikiReplace>")) {
			title = title.replaceAll(pattern.getArgNoQuotes(1), pattern.getArgNoQuotes(2));
			if (title.contains("NIL") && pattern.arg2.equals("\"NIL\""))
				return (null);
		}
		return (FactComponent.forYagoEntity(title.replace(' ', '_')));
	}

	@Override
	public void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception {
		Announce.doing("Compiling category patterns");
		Map<Pattern, String> patterns = new HashMap<Pattern, String>();
		for (Fact fact : factCollections.get(0).get("<categoryPattern>")) {
			patterns.put(Pattern.compile(fact.getArgNoQuotes(1)), fact.getArgNoQuotes(2));
		}
		Map<Pattern, String> objects = new HashMap<Pattern, String>();
		for (Fact fact : factCollections.get(0).get("<categoryObject>")) {
			patterns.put(Pattern.compile(fact.getArgNoQuotes(1)), fact.getArgNoQuotes(2));
		}
		if(patterns.isEmpty()) Announce.error("No category patterns found");
		Announce.done();
		
		Announce.doing("Compiling word exceptions");
		Set<String> nonconceptual=new TreeSet<String>();
		
		Map<String, String> preferredMeaning=new HashMap<String, String>();
		
		Reader in = new BufferedReader(new FileReader(wikipedia));
		String titleEntity = null;
		while (true) {
			switch (FileLines.find(in, "<title>", "[[Category:")) {
			case -1:
				in.close();
				return;
			case 0:
				titleEntity = getTitleEntity(in, factCollections.get(1));
				break;
			case 1:
				if (titleEntity == null)
					continue;
				String category = FileLines.readTo(in, "]]").toString();
				if (!category.endsWith("]]"))
					continue;
				category = category.substring(0, category.length() - 2);
				extractFacts(titleEntity, category, patterns, objects, writers);
				extractType(titleEntity, category, writers.get(1));
			}
		}
	}

	public CategoryExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}
}
