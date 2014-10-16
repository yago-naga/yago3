package utils.literalParsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javatools.administrative.Announce;
import javatools.parsers.PlingStemmer;
import utils.TermParser;
import fromOtherSources.WordnetExtractor;

/** Class ClassParser
 * 
 * Extracts a wordnet class form a string 
 * 
 * @author Fabian
 * */
public class ClassParser extends TermParser {

	/** Preferred meanings to map names to classes */
	public Map<String, String> preferredMeanings;

	/** Loads the preferred meanings from WordnetExtractor.PREFMEANINGS */
	public ClassParser() throws IOException {
		if (!WordnetExtractor.PREFMEANINGS.isAvailableForReading()) {
			Announce.error(WordnetExtractor.PREFMEANINGS,
					"must be available for reading.",
					"Consider caching the theme by declaring it in inputCached()");
		}
		this.preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection()
				.getPreferredMeanings();
	}

	/** Needs the preferred meanings */
	public ClassParser(Map<String, String> preferredMeanings) {
		this.preferredMeanings = preferredMeanings;
	}

	@Override
	public List<String> extractList(String s) {
		List<String> result = new ArrayList<String>(3);
		for (String word : s.split(",|\n")) {
			word = word.trim().replace("[", "").replace("]", "");
			// Announce.debug(word);
			if (word.length() < 4)
				continue;
			String meaning = preferredMeanings.get(word);
			if (meaning == null)
				meaning = preferredMeanings.get(PlingStemmer.stem(word));
			if (meaning == null)
				meaning = preferredMeanings.get(word.toLowerCase());
			if (meaning == null)
				meaning = preferredMeanings.get(PlingStemmer.stem(word
						.toLowerCase()));
			if (meaning == null)
				continue;
			// Announce.debug("Match",meaning);
			result.add(meaning);
		}
		if (result.size() == 0)
			Announce.debug("Could not find class in", s);
		return (result);
	}

}