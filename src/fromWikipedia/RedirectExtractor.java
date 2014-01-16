package fromWikipedia;

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
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TypeChecker;

/**
 * Extracts all redirects from Wikipedia
 * 
 * @author Johannes Hoffart
 * 
 */
public class RedirectExtractor extends Extractor {

	/** Input file */
	private File wikipedia;
	
	private String language;

    @Override
    public File inputDataFile() {   
      return wikipedia;
    }

	private static final Pattern pattern = Pattern.compile("\\[\\[([^#\\]]*?)\\]\\]");
   
	public static final HashMap<String, Theme> RAWREDIRECTFACTS_MAP = new HashMap<String, Theme>();

	public static final HashMap<String, Theme> REDIRECTLABELS_MAP = new HashMap<String, Theme>();
   
	static {
		for (String s : Extractor.languages) {
			RAWREDIRECTFACTS_MAP.put(s, new Theme("redirectFacts" +  Extractor.langPostfixes.get(s), "Redirect facts from Wikipedia redirect pages"));
			REDIRECTLABELS_MAP.put(s, new Theme("redirectLabels" +  Extractor.langPostfixes.get(s), "Redirect facts from Wikipedia redirect pages"));
		}
	}

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(RAWREDIRECTFACTS_MAP.get(this.language));
	}

	@Override
	public Set<Extractor> followUp() {	
	  return new HashSet<Extractor>(Arrays.asList(new TypeChecker(RAWREDIRECTFACTS_MAP.get(this.language), REDIRECTLABELS_MAP.get(this.language), this)));
	}
	
	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting Redirects");
		Map<String, String> redirects = new HashMap<>();

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);

		String titleEntity = null;
		redirect: while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "<redirect")) {
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
				FileLines.readTo(in, "[[").toString().trim();
				String redirect = FileLines.readTo(in, "]]").toString().trim();
				redirect = "[[" + redirect;
				String redirectTarget = getRedirectTarget(redirect);

				if (redirectTarget != null) {
					redirects.put(titleEntity, redirectTarget);
				}
			}
		}

		FactWriter out = output.get(RAWREDIRECTFACTS_MAP.get(this.language));

		for (Entry<String, String> redirect : redirects.entrySet()) {
			out.write(new Fact(
					FactComponent.forYagoEntity(redirect.getValue().replace(' ','_')), 
					"<redirectedFrom>", 
					FactComponent.forStringWithLanguage(redirect.getKey(), this.language.equals("en") ? "eng" : this.language)));
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
		this(wikipedia, decodeLang(wikipedia.getName()));
	}
	
	public RedirectExtractor(File wikipedia, String lang) {
		this.wikipedia = wikipedia;
		this.language = lang;
	}
}
