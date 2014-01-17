package fromThemes;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.parsers.Char;

import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import fromWikipedia.Extractor;
import fromWikipedia.InfoboxExtractor;

public class AttributeRedirector extends Redirector {
	
	private static final Pattern pattern = Pattern.compile("\\[\\[([^#\\]]*?)\\]\\]");

	public AttributeRedirector(Theme in, Theme out, Extractor parent, String lang) {
		super(in, out, parent, lang);
	}
	
	public AttributeRedirector(Theme in, Theme out, String lang) {
		this(in, out, null, lang);
	}
	
	@Override
	protected Fact redirectArguments(Fact dirtyFact, Map<String, String> redirects) {
		String redirectedArg1 = dirtyFact.getArg(1);
		if (redirects.containsKey(dirtyFact.getArg(1))) {
			redirectedArg1 = redirects.get(dirtyFact.getArg(1));
		}

		String redirectedArg2 = dirtyFact.getArg(2);
		Matcher matcher = pattern.matcher(redirectedArg2);
		while (matcher.find()) {
			String found = matcher.group();
			String[] parts = found.substring(2, found.length() - 2).split("\\|");
			
			String entity = FactComponent.forYagoEntity(parts[0]);
			if(redirects.containsKey(entity)){
				String redirectValue = redirects.get(entity);
				parts[0] = FactComponent.stripBrackets(redirectValue).replace("_",  " ");
			}
			
			String updatedFound = "";
			for (String s : parts) {
				updatedFound += s + "|";
			}
			updatedFound = "[[" + Char.cutLast(updatedFound) + "]]";
			
			redirectedArg2 = redirectedArg2.replace(found, updatedFound);
		}
		
		Fact redirectedFact = new Fact(redirectedArg1, dirtyFact.getRelation(), redirectedArg2);
		redirectedFact.makeId();

		return redirectedFact;
	}
	
	public static void main(String[] args) throws Exception {
	    Announce.setLevel(Announce.Level.DEBUG);
	    
      AttributeRedirector extractor = new AttributeRedirector(InfoboxExtractor.INFOBOXATTS_MAP.get("en"), InfoboxExtractor.INFOBOXATTS_REDIRECTED_MAP.get("en"), "en");
      extractor.extract(new File("/home/jbiega/data/yago2s/"),
          "mapping infobox attributes into infobox facts");
	}
}
