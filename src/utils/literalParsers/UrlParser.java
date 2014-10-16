package utils.literalParsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import utils.TermParser;
import basics.FactComponent;

/**
 * Class UrlParser
 * 
 * Extracts a URL from a string
 * 
 * This could in principle be done by a LiteralParser, but since URLs can
 * contain underscores, the built-in pattern matching would be derailed.
 * 
 * @author Fabian
 * */
public class UrlParser extends TermParser {

	// also needs to match \ for yago-encoded stuff
	private static List<Pattern> urlPatterns = Arrays.asList(
			Pattern.compile("http[s]?://([-\\w\\./\\\\]+)"),
			Pattern.compile("(www\\.[-\\w\\./\\\\]+)"));

	@Override
	public List<String> extractList(String s) {
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
}
