package utils.literalParsers;

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

}
