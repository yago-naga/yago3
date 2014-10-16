package utils.literalParsers;

import java.io.IOException;
import java.util.regex.Matcher;

import basics.FactComponent;
import fromOtherSources.PatternHardExtractor;

/**
 * Class DateParser
 * 
 * Extracts dates from a string
 * 
 * @author Fabian
 * */
public class DateParser extends LiteralParser {

	public DateParser() throws IOException {
		super(PatternHardExtractor.DATEPARSER);
	}

	@Override
	public String resultEntity(Matcher resultMatch) {
		return FactComponent.forDate(resultMatch.group(1).trim());
	}

}
