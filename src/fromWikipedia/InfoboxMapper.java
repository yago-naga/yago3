package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.parsers.Char;
import utils.PatternList;
import utils.TermExtractor;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import basics.Theme.ThemeGroup;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.TypeChecker;

/**
 * Class InfoboxMapper - YAGO2S
 * 
 * 
 * 
 * @author Farzaneh Mahdisoltani
 */

public abstract class InfoboxMapper extends Extractor {
	
	protected String language; 
  public static final HashMap<String, Theme> INFOBOXFACTS_TOREDIRECT_MAP = new HashMap<String, Theme>();
  public static final HashMap<String, Theme> INFOBOXSOURCES_MAP = new HashMap<String, Theme>();
  
  
  static {
    for (String s : Extractor.languages) {
      INFOBOXFACTS_TOREDIRECT_MAP.put(s, new Theme("infoboxFactsToBeRedirected" + Extractor.langPostfixes.get(s), "Facts of infobox", ThemeGroup.OTHER));
      INFOBOXSOURCES_MAP.put(s, new Theme("infoboxSources" + Extractor.langPostfixes.get(s), "Sources of infobox", ThemeGroup.OTHER));
    }

  }

//	public static final Theme INFOBOXFACTS = new Theme("infoboxFactsToBeRedirected", "Facts of infobox");
//	public static final Theme INFOBOXSOURCES = new Theme("infoboxSources", "Sources for facts of inforbox");




	@Override
	public Set<Theme> input() {
		// return new HashSet<>(Arrays.asList(InfoboxExtractor.INFOBOXATTS));
		// //new finalset
		return new HashSet<Theme>(
		    Arrays.asList(
				PatternHardExtractor.INFOBOXPATTERNS,
				HardExtractor.HARDWIREDFACTS, InfoboxExtractor.INFOBOXATTS_MAP.get(language),
				WordnetExtractor.WORDNETWORDS,
				PatternHardExtractor.TITLEPATTERNS)
				);

	}

	@Override
	public Set<Theme> output() {
		return new HashSet<>(Arrays.asList(INFOBOXFACTS_TOREDIRECT_MAP.get(language),
				INFOBOXSOURCES_MAP.get(language)));
	}
	
	
	
  public InfoboxMapper(String lang) {
     language = lang; 
  }

	
}