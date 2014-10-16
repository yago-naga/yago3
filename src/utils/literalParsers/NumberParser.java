package utils.literalParsers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;

import javatools.administrative.Announce;
import basics.FactComponent;
import extractors.Extractor.ImplementationNote;
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
		@ImplementationNote("Use toPlainString() so that subsequent regular expression type checks can identify integers")
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

}
