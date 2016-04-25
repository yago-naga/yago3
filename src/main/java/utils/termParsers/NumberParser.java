package utils.termParsers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;

import javatools.administrative.Announce;
import basics.Fact;
import basics.Fact.ImplementationNote;
import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;

/** Class NumberParser
 * 
 * Extracts a number from a string 
 * 
 * @author Fabian
 * */
public class NumberParser extends LiteralParser {

	public NumberParser() throws IOException {
		super(PatternHardExtractor.NUMBERPARSER);
	}

	/** Parses a numerical expression with +, *, E */
	public static final BigDecimal parseNumerical(String expression) {
		// Remove any blanks that people added for readability
		expression = expression.replaceAll(" ", "");
		BigDecimal result = null;
		try {
			// Use a lookahead, so that we can retrieve the delimiter.
			String split[] = expression.split("(?=[\\+\\*])");
			result = new BigDecimal(split[0]);
			for (int i = 1; i < split.length; i++) {
				char operator = split[i].charAt(0);
				BigDecimal factor = new BigDecimal(split[i].substring(1));
				switch (operator) {
				case '*':
					result = result.multiply(factor);
					break;
				case '+':
					result = result.add(factor);
					break;
				default:
					Announce.warning("Faulty operator:", operator);
					return (null);
				}
			}
		} catch (Exception e) {
			Announce.warning("Cannot parse numerical expression", expression,
					"due to", e.toString());
			return (null);
		}
		return (result);
	}

	@Override
	public String resultEntity(Matcher resultMatch) {
		@Fact.ImplementationNote("Use toPlainString() so that subsequent regular expression type checks can identify integers")
		BigDecimal bigdec = parseNumerical(resultMatch.group(1));
		String unit = resultMatch.group(2).trim();
		if (bigdec == null) {
			Announce.warning("Cannot parse the numerical value",
					resultMatch.group());
			return (null);
		}
		return (FactComponent.forStringWithDatatype(bigdec.toPlainString(),
				unit));
	}

	
	/* This is the old code before taking into account Thomas Rebele's patterns in 2014.
	 * 
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

}
