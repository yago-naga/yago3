package extractorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.parsers.DateParser;
import javatools.parsers.NumberParser;
import javatools.parsers.PlingStemmer;
import basics.FactComponent;

/**
 * Class TermExtractor
 * 
 * Methods that extract entities from Wikipedia strings
 * 
 * @author Fabian M. Suchanek
 */
public abstract class TermExtractor {

	/** Watch out: forClass needs to be handled separately*/
	public static TermExtractor forType(String type) {
		switch (type) {
		case "rdf:Resource":
			return (forWikiLink);
		case "xsd:date":
			return (forDate);
		case "<yagoWord>":
		case "xsd:string":
		case "<yagoTLD>":
		case "<yagoISBN>":
		case "<yagoIdentifier>":
			return (forString);
		case "<yagoUrl>":
			return (forUrl);
		case "xsd:decimal":
		case "<yagoGeoCoordinate>":
		case "<yagoArea>":
		case "<yagoMonetaryValue>":
		case "<yagoProportion>":
		case "<yagoDensityPerArea>":
		case "xsd:integer":
		case "xsd:duration":
		case "<yagoWeight>":
		case "<yagoLength>":
		case "xsd:nonNegativeInteger":
			return (forNumber);
		}
		return (forWikiLink);
	}

	// also needs to match \ for yago-encoded stuff
	private static List<Pattern> urlPatterns = Arrays.asList(
			Pattern.compile("http[s]?://([-\\w\\./\\\\]+)"),
			Pattern.compile("(www\\.[-\\w\\./\\\\]+)"));

	/** Extracts an entity from a string. Return NULL if this fails. */
	public String extractSingle(String s) {
		List<String> elements = extractList(s);
		if (elements != null) {
			return elements.get(0);
		} else {
			return null;
		}
	}

	/** Extracts multiple entities from a string. Return NULL if this fails. */
	public abstract List<String> extractList(String s);

	/** TRUE if the resulting entity has to be type checked */
	public boolean requiresTypecheck() {
		return (false);
	}

	/** Extracts a number form a string */
	public static TermExtractor forNumber = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			List<String> result = NumberParser.getNumbers(NumberParser
					.normalize(s));
			if (result.size() == 0) {
				Announce.debug("No number found in", s);
			}
			return (result);
		}

	};

	/** Extracts a URL form a string */
	public static TermExtractor forUrl = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			// URL encode before matching - beacuse of unicode titles
			// s = Normalize.string(s) + ' ';

			List<String> urls = new ArrayList<String>(3);

			boolean match = true;
			int pos = 0;

			while (match) {
				for (Pattern p : urlPatterns) {
					Matcher m = p.matcher(s);
					if (m.find(pos)) {
						String url = FactComponent.forUri("http://"
								+ m.group(1));
						urls.add(url);
						match = true;
						pos = m.end(1);
					} else {
						match = false;
					}
				}
			}

			if (urls.size() == 0)
				Announce.debug("Could not find URL in", s);
			return urls;
		}
	};

	/** Extracts a date form a string */
	public static TermExtractor forDate = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			List<String> result = new ArrayList<String>();
			for(String d : DateParser.getDates(DateParser.normalize(s))) {
				result.add(FactComponent.forDate(d));
			}
			if (result.size() == 0) {
				Announce.debug("No date found in", s);
			}
			return (result);
		}

	};

	/** Extracts an entity form a string */
	public static TermExtractor forEntity = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			return Arrays.asList(FactComponent.forYagoEntity(s));
		}

		@Override
		public boolean requiresTypecheck() {
			return (true);
		}
	};

	/** Extracts a YAGO string from a string */
	public static TermExtractor forString = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			s = s.trim();
			List<String> result = new ArrayList<String>(3);
			for (String w : s.split(";|,?<br />|'''|''|, ?;|\"")) {
				w = w.trim();
				if (w.length() > 2 && !w.contains("{{") && !w.contains("[["))
					result.add(FactComponent.forString(w, null, null));
			}
			if (result.size() == 0)
				Announce.debug("Could not find string in", s);
			return (result);
		}
	};

	/** Extracts a cleaned YAGO string form a part of text */
	public static TermExtractor forText = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			StringBuilder sb = new StringBuilder();
			int brackets = 0;

			for (int i = 0; i < s.length(); i++) {
				char current = s.charAt(i);

				if (current == '{') {
					brackets++;
				} else if (current == '}') {
					brackets--;
				} else if (brackets == 0) {
					sb.append(current);
				}
			}

			String clean = sb.toString().trim();

			clean = clean.replaceAll("\\s+", " ");
			clean = clean.replaceAll("\\[\\[[^\\]\n]+?\\|([^\\]\n]+?)\\]\\]",
					"$1");
			clean = clean.replaceAll("\\[\\[([^\\]\n]+?)\\]\\]", "$1");
			clean = clean.replaceAll("\\[https?:.*?\\]", "");
			clean = clean.replaceAll("'{2,}", "");
			clean = clean.trim();

			if (clean.length() == 0) {
				Announce.debug("Could not find text in", s);
				return (Arrays.asList());
			}

			return Arrays.asList(FactComponent.forString(clean, null, null));
		}
	};

	/** Extracts a language form a string */
	public static TermExtractor forLanguageCode = new TermExtractor() {

		@Override
		public List<String> extractList(String s) {
			return (Arrays.asList()); // TODO not yet implemented
			// String language = Basics.code2language.get(s);
			// return (language == null ? new ArrayList<String>() :
			// Arrays.asList(language));
		}
	};

	/** Extracts a wiki link form a string */
	public static TermExtractor forWikiLink = new TermExtractor() {

		Pattern wikipediaLink = Pattern
				.compile("\\[\\[([^\\|\\]]+)(?:\\|([^\\]]+))?\\]\\]");

		@Override
		public boolean requiresTypecheck() {
			return (true);
		}

		@Override
		public List<String> extractList(String s) {
			List<String> links = new LinkedList<String>();

			Matcher m = wikipediaLink.matcher(s);
			while (m.find()) {
				String result = m.group(1);
				if (result.contains(":") || result.contains("#"))
					continue;
				if (result.contains(" and ") || result.contains("#"))
					continue;
				if (s.substring(m.end()).startsWith(" [["))
					continue; // It was an adjective
				if (result.matches("\\d+"))
					continue; // It's the year in which sth happened
				result = result.trim();
				if(result.startsWith("[")) result=result.substring(1);
				if (result.isEmpty())
					continue; // the result was composed only of whitespaces

				result=result.replace(' ','_');
				// resolve redirect and add to links
				// String target = resolveRedirect(result); TODO redirects not
				// yet implemented
				links.add(FactComponent.forYagoEntity(result));
			}

			if (links.size() == 0)
				Announce.debug("Could not find wikilink in", s);
			return links;
		}

	};

	/** Extracts a wordnet class form a string */
	public static class ForClass extends TermExtractor {

		public Map<String, String> preferredMeanings;

		public ForClass(Map<String, String> preferredMeanings) {
			this.preferredMeanings = preferredMeanings;
		}

		@Override
		public List<String> extractList(String s) {
			List<String> result = new ArrayList<String>(3);
			for (String word : s.split(",")) {
				word = word.trim().replace("[", "").replace("]]", "");
				if (word.length() < 4)
					continue;
				String meaning = preferredMeanings.get(word);
				if (meaning == null)
					meaning = preferredMeanings.get(PlingStemmer.stem(word));
				if (meaning == null)
					continue;
				result.add(meaning);
			}
			if (result.size() == 0)
				Announce.debug("Could not find class in", s);
			return (result);
		}

	};

}
