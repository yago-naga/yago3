package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.PatternList;
import utils.TermExtractor;

import javatools.datatypes.FinalSet;
import javatools.parsers.Char;

import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.PatternHardExtractor;
import fromWikipedia.Translator;
import fromWikipedia.Extractor;
import fromWikipedia.InfoboxExtractor;

public class InfoboxTermExtractor extends Extractor {
	
	public static final HashMap<String, Theme> INFOBOXTERMS_TOREDIRECT_MAP = new HashMap<String, Theme>();
	public static final HashMap<String, Theme> INFOBOXTERMS_MAP = new HashMap<String, Theme>();
	public static final HashMap<String, Theme> INFOBOXATTSTRANSLATED_MAP = new HashMap<String, Theme>();
	
	private String language;
	
	static {
		for (String s : Extractor.languages) {
			INFOBOXTERMS_TOREDIRECT_MAP.put(s, new Theme("infoboxTermsToBeRedirected" + Extractor.langPostfixes.get(s), "Attribute terms of infobox, still to be redirected", ThemeGroup.OTHER));
			INFOBOXTERMS_MAP.put(s, new Theme("infoboxTerms" + Extractor.langPostfixes.get(s), "Attribute terms of infobox", ThemeGroup.OTHER));
			INFOBOXATTSTRANSLATED_MAP.put(s, new Theme("infoboxAttributesTranslated" + Extractor.langPostfixes.get(s), "Attribute terms of infobox translated", ThemeGroup.OTHER));
	    }
	}

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				InfoboxExtractor.INFOBOXATTS_MAP.get(this.language)));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				INFOBOXTERMS_TOREDIRECT_MAP.get(this.language));
	}
	
	@Override
	public Set<Extractor> followUp() {
		return new HashSet<Extractor>(Arrays.asList(new Redirector(
				INFOBOXTERMS_TOREDIRECT_MAP.get(language), INFOBOXTERMS_MAP.get(language), this, this.language),
				new Translator(INFOBOXTERMS_MAP.get(language), INFOBOXATTSTRANSLATED_MAP.get(this.language), this.language, "Entity")));
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		
		FactWriter out = output.get(INFOBOXTERMS_TOREDIRECT_MAP.get(language));
		
		FactCollection infoboxPatterns = new FactCollection(input.get(PatternHardExtractor.INFOBOXPATTERNS));
		PatternList replacements = new PatternList(infoboxPatterns,"<_infoboxReplace>");
		
		for (Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(this.language))) {
			for(TermExtractor extractor : TermExtractor.all()) {
				
				String attrs = f.getArgJavaString(2);
				attrs = replacements.transform(Char.decodeAmpersand(attrs));
				attrs = attrs.replace("$0", FactComponent.stripBrackets(f.getArg(1)));
				attrs = attrs.trim();
			    if (attrs.length() == 0)
			    	continue;
				
				List<String> objects = extractor.extractList(attrs);
				
				for (String object : objects) {
			        Fact fact = new Fact(f.getArg(1), f.getRelation(), object);
			        out.write(fact);
				}
			}
		}
	}
	
	public InfoboxTermExtractor(String lang) {
		super();
		this.language = lang; 
	}
	
	public static void main(String[] args) throws Exception {
		InfoboxTermExtractor extractor = new InfoboxTermExtractor("en");
		extractor.extract(new File("/home/jbiega/data/yago2s/"),
				"mapping infobox attributes into infobox facts");
  	}

}
