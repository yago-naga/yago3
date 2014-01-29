package fromWikipedia;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import basics.Theme;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;

/**
 * Class InfoboxMapper - YAGO2S
 * 
 * Maps the facts in the output of InfoboxExtractor 
 * for non-English languages.
 * 
 * @author Farzaneh Mahdisoltani
 */

public abstract class InfoboxMapper extends Extractor {
	
  protected String language; 
  public static final HashMap<String, Theme> INFOBOXFACTS_TOREDIRECT_MAP = new HashMap<String, Theme>();
  public static final HashMap<String, Theme> INFOBOXFACTS_TOTYPECHECK_MAP = new HashMap<String, Theme>();
  public static final HashMap<String, Theme> INFOBOXFACTS_MAP = new HashMap<String, Theme>();
  public static final HashMap<String, Theme> INFOBOXSOURCES_MAP = new HashMap<String, Theme>();
  
  
  static {
    for (String s : Extractor.languages) {
      INFOBOXFACTS_TOREDIRECT_MAP.put(s, new Theme("infoboxFactsToBeRedirected" + Extractor.langPostfixes.get(s), "Facts of infobox, still to be redirected and type-checked", ThemeGroup.OTHER));
      INFOBOXFACTS_TOTYPECHECK_MAP.put(s, new Theme("infoboxFactsToBeTypechecked" + Extractor.langPostfixes.get(s), "Facts of infobox, redirected, still to be type-checked", ThemeGroup.OTHER));
      INFOBOXFACTS_MAP.put(s, new Theme("infoboxFacts" + Extractor.langPostfixes.get(s), "Facts of infobox, redirected and type-checked", ThemeGroup.OTHER));
      INFOBOXSOURCES_MAP.put(s, new Theme("infoboxSources" + Extractor.langPostfixes.get(s), "Sources of infobox", ThemeGroup.OTHER));
    }

  }

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				HardExtractor.HARDWIREDFACTS, 
				InfoboxExtractor.INFOBOXATTS_MAP.get(language),
				WordnetExtractor.WORDNETWORDS,
				PatternHardExtractor.TITLEPATTERNS
				));

	}

	@Override
	public Set<Theme> output() {
		return new HashSet<>(Arrays.asList(INFOBOXFACTS_TOREDIRECT_MAP.get(language),INFOBOXFACTS_TOTYPECHECK_MAP.get(language),
				INFOBOXSOURCES_MAP.get(language)));
	}
	
	
	
  public InfoboxMapper(String lang) {
     language = lang; 
  }

	
}