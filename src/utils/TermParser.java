package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.parsers.Char;
import javatools.parsers.DateParser;
import javatools.parsers.NumberParser;
import javatools.parsers.PlingStemmer;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;

/**
 * Class TermExtractor
 * 
 * Methods that extract entities from Wikipedia strings
 * 
 * @author Fabian M. Suchanek
 */
public abstract class TermParser {

	/** Holds the name of the extractor */
	public final String name;

	protected TermParser(String n) {
		name = "TermParser for " + n;
	}

	@Override
	public String toString() {
		return name;
	}

	/** Watch out: forClass needs to be handled separately */
	public static TermParser forType(String type) {
		switch (type) {
		case RDFS.clss:
			Announce.error("Call TermParser.forClass() for classes!");
			return (null);
		case YAGO.entity:
		case "rdf:Resource":
			return (forWikiLink);
		case "xsd:date":
			return (forDate);
		case "<yagoLanString>":
		case "xsd:string":
		case "<yagoTLD>":
		case "<yagoISBN>":
		case "<yagoIdentifier>":
			return (forString);
		case "<yagoURL>":
			return (forUrl);
		case "xsd:decimal":
		case "<degrees>":
		case "<m^2>":
		case "<yagoMonetaryValue>":
		case "<%>":
		case "</km^2>":
		case "xsd:integer":
		case "xsd:duration":
		case "<g>":
		case "<m>":
		case "<s>":
		case "<yago0to100>":
		case "xsd:nonNegativeInteger":
		case "<yagoFraction>":
			return (forNumber);
		}
		return (forWikiLink);
	}

	/** Returns all available parsers */
	public static List<TermParser> all(Map<String, String> preferredMeanings) {
		List<TermParser> all = new ArrayList<>(all());
		all.add(new TermParser.ForClass(preferredMeanings));
		return all;
	}

	/** Returns all available parsers, excluding the class parser */
	public static List<TermParser> all() {
		return Arrays
				.asList(forWikiLink, forDate, forString, forUrl, forNumber);
	}

	// also needs to match \ for yago-encoded stuff
	private static List<Pattern> urlPatterns = Arrays.asList(
			Pattern.compile("http[s]?://([-\\w\\./\\\\]+)"),
			Pattern.compile("(www\\.[-\\w\\./\\\\]+)"));

	/** Extracts multiple entities from a string. Return NULL if this fails. */
	public abstract List<String> extractList(String s);

	/** Extracts a number form a string */
	public static TermParser forNumber = new TermParser("number") {

		@Override
		public List<String> extractList(String s) {
			List<String> result = new ArrayList<>();
			for (String num : NumberParser
					.getNumbers(NumberParser.normalize(s))) {
				String[] nd = NumberParser.getNumberAndUnit(num, new int[2]);
				if (nd.length == 1 || nd[1] == null)
					result.add(FactComponent.forNumber(nd[0]));
				else
					result.add(FactComponent.forStringWithDatatype(nd[0],
							FactComponent.forYagoEntity(nd[1])));
			}
			if (result.size() == 0) {
				Announce.debug("No number found in", s);
			}
			return (result);
		}

	};

	/** Extracts a URL form a string */
	public static TermParser forUrl = new TermParser("url") {

		@Override
		public List<String> extractList(String s) {
			// URL encode before matching - beacuse of unicode titles
			// s = Normalize.string(s) + ' ';

			List<String> urls = new ArrayList<String>(3);

			for (Pattern p : urlPatterns) {
				Matcher m = p.matcher(s);
				while (m.find()) {
					String url = FactComponent.forUri("http://" + m.group(1));
					urls.add(url);
				}
			}

			if (urls.size() == 0)
				Announce.debug("Could not find URL in", s);
			return urls;
		}
	};

	/** Extracts a date form a string */
	public static TermParser forDate = new TermParser("date") {

		@Override
		public List<String> extractList(String s) {
			List<String> result = new ArrayList<String>();
			for (String d : DateParser.getDates(DateParser.normalize(s))) {
				result.add(FactComponent.forDate(d));
			}
			if (result.size() == 0) {
				Announce.debug("No date found in", s);
			}
			return (result);
		}

	};

	/** Extracts an entity form a string */
	public static TermParser forEntity = new TermParser("entity") {

		@Override
		public List<String> extractList(String s) {
			return Arrays.asList(FactComponent.forYagoEntity(s));
		}

	};

	/** Extracts a YAGO string from a string */
	public static TermParser forString = new TermParser("string") {

		@Override
		public List<String> extractList(String s) {
			s = s.trim();
			List<String> result = new ArrayList<String>(3);
			if (s.startsWith("[[")) {
				for (String link : forWikiLink.extractList(s)) {
					result.add(FactComponent.forString(FactComponent
							.stripBrackets(link).replace('_', ' ')));
				}
				return (result);
			}
			for (String w : s.split(";|,?\n|'''|''|, ?;|\"")) {
				w = w.replaceAll("\\(.*\\)", ""); // Remove bracketed parts
				w = w.replace("(", "").replace(")", ""); // remove remaining
															// brackets
				w = w.trim();
				w = Char.decodeAmpersand(w);
				// Before:
				// w.length() > 2 && !w.contains("{{") && !w.contains("[[")
				if (w.matches("[\\p{IsAlphabetic} ]{2,}"))
					result.add(FactComponent.forString(w));
			}
			if (result.size() == 0)
				Announce.debug("Could not find string in", s);
			return (result);
		}
	};

	/** Extracts a cleaned YAGO string form a part of text */
	public static TermParser forText = new TermParser("text") {

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

			return Arrays.asList(FactComponent.forString(clean));
		}
	};

	/** Extracts a language form a string */
	public static TermParser forLanguageCode = new TermParser("language") {

		@Override
		public List<String> extractList(String s) {
			return (Arrays.asList()); // TODO not yet implemented
			// String language = Basics.code2language.get(s);
			// return (language == null ? new ArrayList<String>() :
			// Arrays.asList(language));
		}
	};

	/** Extracts a wiki link form a string */
	public static TermParser forWikiLink = new TermParser("wikilink") {

		Pattern wikipediaLink = Pattern
				.compile("\\[\\[([^\\|\\]]+)(?:\\|([^\\]]+))?\\]\\]");

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
				if (result.startsWith("["))
					result = result.substring(1);
				if (result.isEmpty())
					continue; // the result was composed only of whitespaces

				result = result.replace(' ', '_');
				links.add(FactComponent.forWikipediaTitle(result));
			}

			if (links.isEmpty()) {
				for (String c : s.split("\n")) {
					c = FactComponent.stripQuotes(c.trim());
					if (c.contains(" ") && c.matches("[\\p{L} ]+")) {
						Announce.debug("Finding suboptimal wikilink", c, "in",
								s);
						links.add(FactComponent.forWikipediaTitle(c));
					}
				}
			}
			if (links.isEmpty()) {
				Announce.debug("Could not find wikilink in", s);
			}
			return links;
		}

	};

	/** Extracts a wordnet class form a string */
	public static class ForClass extends TermParser {

		public Map<String, String> preferredMeanings;

		public ForClass(Map<String, String> preferredMeanings) {
			super("class");
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

	};

}
