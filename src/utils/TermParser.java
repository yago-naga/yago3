package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javatools.administrative.Announce;
import utils.literalParsers.ClassParser;
import utils.literalParsers.DateParser;
import utils.literalParsers.NumberParser;
import utils.literalParsers.StringParser;
import utils.literalParsers.UrlParser;
import utils.literalParsers.WikiLinkParser;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;

/**
 * Class TermParser
 * 
 * Methods that extract entities from Wikipedia strings
 * 
 * @author Fabian M. Suchanek
 */
public abstract class TermParser {

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Watch out: forClass needs to be handled separately
	 * 
	 * @throws IOException
	 */
	public static TermParser forType(String type) throws IOException {
		switch (type) {
		case RDFS.clss:
			Announce.error("Call TermParser.forClass() for classes!");
			return (null);
		case YAGO.entity:
		case "rdf:Resource":
			return (new WikiLinkParser("eng"));
		case "xsd:date":
			return (new DateParser());
		case "<yagoLanString>":
		case "xsd:string":
		case "<yagoTLD>":
		case "<yagoISBN>":
		case "<yagoIdentifier>":
			return (new StringParser());
		case "<yagoURL>":
			return (new UrlParser());
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
			return (new NumberParser());
		}
		return (new WikiLinkParser("eng"));
	}

	/**
	 * Returns all available parsers
	 * 
	 * @throws IOException
	 */
	public static List<TermParser> allParsers(
			Map<String, String> preferredMeanings, String language)
			throws IOException {
		List<TermParser> all = new ArrayList<>();
		if (FactComponent.isEnglish(language))
			all.add(new ClassParser(preferredMeanings));
		all.add(new WikiLinkParser(language));
		all.add(new NumberParser());
		all.add(new StringParser());
		all.add(new DateParser());
		all.add(new UrlParser());
		return all;
	}

	/** Extracts multiple entities from a string. Return NULL if this fails. */
	public abstract List<String> extractList(String s);

	/*
	 * Extracts a number form a string public static TermParser forNumber = new
	 * TermParser("number") {
	 * 
	 * @Override public List<String> extractList(String s) { List<String> result
	 * = new ArrayList<>(); for (String num : NumberParser
	 * .getNumbers(NumberParser.normalize(s))) { String[] nd =
	 * NumberParser.getNumberAndUnit(num, new int[2]); if (nd.length == 1 ||
	 * nd[1] == null) result.add(FactComponent.forNumber(nd[0])); else
	 * result.add(FactComponent.forStringWithDatatype(nd[0],
	 * FactComponent.forYagoEntity(nd[1]))); } if (result.size() == 0) {
	 * Announce.debug("No number found in", s); } return (result); }
	 * 
	 * };
	 */

	/*
	 * Extracts a YAGO string from a string public static TermParser forString =
	 * new TermParser("string") {
	 * 
	 * @Override public List<String> extractList(String s) { return (new
	 * utils.literalParsers.StringParser(
	 * PatternHardExtractor.STRINGPARSER).extract(s));
	 * 
	 * s = s.trim(); List<String> result = new ArrayList<String>(3); if
	 * (s.startsWith("[[")) { for (String link : forWikiLink.extractList(s)) {
	 * result.add(FactComponent.forString(FactComponent
	 * .stripBracketsAndLanguage(link).replace('_', ' '))); } return (result); }
	 * for (String w : s.split(";|,?\n|'''|''|, ?;|\"")) { w =
	 * w.replaceAll("\\(.*\\)", ""); // Remove bracketed parts w =
	 * w.replace("(", "").replace(")", ""); // remove remaining // brackets w =
	 * w.trim(); w = Char17.decodeAmpersand(w); // Before: // w.length() > 2 &&
	 * !w.contains("{{") && !w.contains("[[") if
	 * (w.matches("[\\p{Alnum} ]{2,}")) result.add(FactComponent.forString(w));
	 * } if (result.size() == 0) Announce.debug("Could not find string in", s);
	 * return (result);
	 * 
	 * } };
	 */

}
