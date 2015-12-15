package utils.termParsers;

import java.io.IOException;
import java.util.regex.Matcher;

import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;

/**
 * Class StringParser
 * 
 * Extracts a string from a Wikipedia string
 * 
 * @author Fabian
 * */
public class StringParser extends LiteralParser {

	public StringParser() throws IOException {
		super(PatternHardExtractor.STRINGPARSER);
	}

	@Override
	public String resultEntity(Matcher resultMatch) {
		return (FactComponent.forString(resultMatch.group(1)));
	}

	/* This is the old code before taking into account Thomas Rebele's patterns in 2014.
	 * 
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
