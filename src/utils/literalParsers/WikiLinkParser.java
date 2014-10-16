package utils.literalParsers;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import utils.TermParser;
import basics.FactComponent;

/**
 * Class WikiLinkParser
 * 
 * Extracts a wiki link from a string
 * 
 * This could in principle be done by a LiteralParser, but it would be very
 * complicated
 * 
 * @author Fabian
 * */
public class WikiLinkParser extends TermParser {

	/** language in which the entities will be generated */
	protected String language;

	public WikiLinkParser(String language) {
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
					Announce.debug("Finding suboptimal wikilink", c, "in", s);
					links.add(FactComponent.forForeignYagoEntity(c, language));
				}
			}
		}
		if (links.isEmpty()) {
			Announce.debug("Could not find wikilink in", s);
		}
		return links;
	}

}