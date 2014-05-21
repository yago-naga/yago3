package extractors;

import java.util.Arrays;
import java.util.List;

import basics.FactComponent;
import javatools.administrative.Announce;

/**
 * MultilingualExtractor - Yago2s
 * 
 * Superclass of all multilingual extractors. This class determines which
 * Wikipedia versions can be read!
 * 
 * By convention, all subclasses have a constructor that takes as argument the
 * language.
 * 
 * @author Fabian
 * 
 */

public abstract class MultilingualExtractor extends Extractor {

	/** List of language suffixes from most English to least English. */
	public static List<String> wikipediaLanguages = Arrays.asList("en", "de",
			"fr");

	/** List of all languages except English */
	public static List<String> allLanguagesExceptEnglish() {
		return(wikipediaLanguages.subList(1, wikipediaLanguages.size()));
	}
	
	/** The language of this extractor */
	public final String language;

	/** TRUE if the language is english*/
	public boolean isEnglish() {
		return(FactComponent.isEnglish(language));
	}

	@Override
	public String name() {
		return (this.getClass().getName() + "(" + this.language + ")");
	}

	public MultilingualExtractor(String lan) {
		this.language = lan;
	}

	/** Creates an extractor given by name */
	public static Extractor forName(Class<MultilingualExtractor> className,
			String language) {
		Announce.doing("Creating extractor", className + "(" + language + ")");
		if (language == null) {
			throw new RuntimeException("Language is null");
		}
		Extractor extractor = null;
		try {
			extractor = className.getConstructor(String.class).newInstance(
					language);
		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}
}
