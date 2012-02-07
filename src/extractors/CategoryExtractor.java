package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.filehandlers.FileLines;
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
		return Arrays.asList("categoryPatterns", "titlePatterns");
	}

	@Override
	public List<String> output() {
		return Arrays.asList("categoryTypes", "categoryFacts");
	}

	@Override
	public List<String> outputDescriptions() {
		return Arrays.asList("Types derived from the categories", "Facts derived from the categories");
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
				for (Fact pattern : factCollections.get(0).get("<categoryPattern>")) {
					Matcher m = Pattern.compile(pattern.getArgNoQuotes(1), Pattern.CASE_INSENSITIVE).matcher(category);
					if (m.matches()) {
						String object = pattern.id == null ? null : factCollections.get(0).getArg2(pattern.id,
								"<hasObject>");
						if (object == null)
							object = m.group(1);
						else
							object = FactComponent.stripQuotes(object).replace("$1", m.group(1));
						Fact fact = new Fact(null, titleEntity, pattern.arg2, FactComponent.forYagoEntity(object));
						if (fact.relation.equals("rdf:type"))
							writers.get(0).write(fact);
						else
							writers.get(1).write(fact);
					}
				}
			}
		}
	}

	public CategoryExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}
}
