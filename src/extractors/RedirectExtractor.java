package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * Extracts all redirects from Wikipedia
 * 
 * @author Johannes Hoffart
 * 
 */
public class RedirectExtractor extends Extractor {

	/** Input file */
	private File wikipedia;

	private static final Pattern pattern = Pattern.compile("\\[\\[([^#\\]]*?)\\]\\]");

	 /** Redirect facts from Wikipedia redirect pages */
  public static final Theme REDIRECTFACTS = new Theme("redirectFacts",
      "Redirect facts from Wikipedia redirect pages");
	
	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(REDIRECTFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting Redirects");
		Map<String, String> redirects = new HashMap<>();

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);

		String titleEntity = null;
		redirect: while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "#REDIRECT")) {
			case -1:
				Announce.done();
				in.close();
				break redirect;
			case 0:
        titleEntity = FileLines.readToBoundary(in, "</title>");
				break;
			default:
				if (titleEntity == null)
					continue;
				String redirect = FileLines.readTo(in, "]]").toString().trim();
				String redirectTarget = getRedirectTarget(redirect);

				if (redirectTarget != null) {
					redirects.put(titleEntity, redirectTarget);
				}
			}
		}

		FactWriter out = output.get(REDIRECTFACTS);

		for (Entry<String, String> redirect : redirects.entrySet()) {
			out.write(new Fact(FactComponent.forString(redirect.getKey()), "<_isWikipediaRedirectTo>", FactComponent.forYagoEntity(redirect.getValue())));
		}
		
		Announce.done();
	}

	private String getRedirectTarget(String redirect) {
		Matcher m = pattern.matcher(redirect);

		if (m.find()) {
			return m.group(1);
		} else {
			return null;
		}
	}

	/**
	 * Needs Wikipedia as input
	 * 
	 * @param wikipedia
	 *            Wikipedia XML dump
	 */
	public RedirectExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}
}
