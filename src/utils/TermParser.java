package utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.parsers.Char17;
import javatools.parsers.DateParser;
import javatools.parsers.NumberParser;
import javatools.parsers.PlingStemmer;
import literalparser.Generator;
import literalparser.configuration.XMLConfiguration;
import literalparser.literal.Literal;
import literalparser.literal.Literal.BigDecimalLiteral;
import literalparser.literal.Literal.DateLiteral;
import literalparser.literal.Literal.QuantityValueLiteral;
import literalparser.literal.Literal.StringLiteral;
import literalparser.literal.LiteralFind;
import literalparser.parser.dag.DAGParser;
import literalparser.parser.dag.LinearGetLiteralsStrategy;
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

	protected DAGParser parser = null;

	protected TermParser(String n) {
		name = "TermParser for " + n;
		if (n == "literal") {
			XMLConfiguration c = new XMLConfiguration(
					"./data/literalparser.xml");
			DAGParser.Builder builder = new DAGParser.Builder();
			builder.useGetLiteralsStrategy(new LinearGetLiteralsStrategy());
			parser = (new Generator()).createParser(c, builder);
		}
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
		case "<m\\u005e2>":
		case "<yagoMonetaryValue>":
		case "<%>":
		case "</km^2>":
		case "</km\\u005e2>":
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
	public static List<TermParser> allParsers(
			Map<String, String> preferredMeanings, String language) {
		List<TermParser> all = new ArrayList<>(literalParsers());
		if (FactComponent.isEnglish(language))
			all.add(new TermParser.ForClass(preferredMeanings));
		all.add(new TermParser.ForWikiLink(language));
		return all;
	}

	/** Returns all available parsers, excluding the class parser */
	public static List<TermParser> literalParsers() {
		return Arrays.asList(forLiterals, forString);
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

	public static TermParser forLiterals = new TermParser("literal") {

		@Override
		public List<String> extractList(String s) {
			List<String> result = new ArrayList<String>();
			for (LiteralFind literalFind : parser.getLiterals(s)) {
				Literal literal = literalFind.literal;
				if (literal instanceof QuantityValueLiteral) {
					result.add(FactComponent.forStringWithDatatype(
							((QuantityValueLiteral) literal).getValue() + "",
							FactComponent
									.forYagoEntity(((QuantityValueLiteral) literal)
											.getUnit())));
				}
				else if (literal instanceof DateLiteral)
					result.add(FactComponent.forDate(((DateLiteral) literal)
							.toString()));
				else if (literal instanceof BigDecimalLiteral) {
					BigDecimalLiteral lit = (BigDecimalLiteral) literal;
					if ((lit.getValue().intValue() < Calendar.getInstance()
							.get(Calendar.YEAR))) {
						String year = lit.toString().replace("-","");
						if (year.length()>2 && year.indexOf(".") == -1)
							result.add(FactComponent.forYear(lit.toString()));
					}
					result.add(FactComponent.forNumber(lit.toString()));
				} 
				else if (literal instanceof StringLiteral) {
					result.add(FactComponent
							.forString(((StringLiteral) literal).toString()));
				}
			}

			return result;
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
							.stripBracketsAndLanguage(link).replace('_', ' ')));
				}
				return (result);
			}
			for (String w : s.split(";|,?\n|'''|''|, ?;|\"")) {
				w = w.replaceAll("\\(.*\\)", ""); // Remove bracketed parts
				w = w.replace("(", "").replace(")", ""); // remove remaining
															// brackets
				w = w.trim();
				w = Char17.decodeAmpersand(w);
				// Before:
				// w.length() > 2 && !w.contains("{{") && !w.contains("[[")
				if (w.matches("[\\p{Alnum} ]{2,}"))
					result.add(FactComponent.forString(w));
			}
			if (result.size() == 0)
				Announce.debug("Could not find string in", s);
			return (result);
		}
	};

	/** Extracts Wikipedia entity form a wiki link */
	public static class ForWikiLink extends TermParser {

		/** language in which the entities will be generated */
		protected String language;

		protected ForWikiLink(String language) {
			super("forEntity(" + language + ")");
			this.language = language;
		}

		/** Wikipedia link pattern */
		protected static Pattern wikipediaLink = Pattern
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

				links.add(FactComponent.forForeignYagoEntity(result, language));
			}

			if (links.isEmpty()) {
				for (String c : s.split("\n")) {
					c = FactComponent.stripQuotes(c.trim());
					if (c.contains(" ") && c.matches("[\\p{L} ]+")) {
						Announce.debug("Finding suboptimal wikilink", c, "in",
								s);
						links.add(FactComponent.forForeignYagoEntity(c,
								language));
					}
				}
			}
			if (links.isEmpty()) {
				Announce.debug("Could not find wikilink in", s);
			}
			return links;
		}

	};

	/** Extracts an English YAGO entity string */
	public static TermParser forWikiLink = new ForWikiLink("eng");

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

	public static void main(String[] args) throws Exception {
		System.out.println(TermParser.forString.extractList("012436437"));
	}

}
