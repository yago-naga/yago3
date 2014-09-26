package followUp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.FinalSet;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import fromOtherSources.DictionaryExtractor;

/**
 * EntityTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English
 * language.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class EntityTranslator extends FollowUpExtractor {

	/** Target language */
	protected String language;

	/** Object dictionary */
	protected Theme objectDictionaryTheme;

	@Override
	public Set<Theme> input() {
		// Do not use a FinalSet here because
		// objectDictionary might be equivalent to
		// entityDictionary
		return (new HashSet<>(Arrays.asList(checkMe,
				DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language),
				objectDictionaryTheme)));
	}

	@Override
	public Set<Theme> inputCached() {
		// Do not use a FinalSet here because
		// objectDictionary might be equivalent to
		// entiyDictionary
		return (new HashSet<>(Arrays.asList(
				DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language),
				objectDictionaryTheme)));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	/**
	 * Translates the object as an entity, or returns it simply if it's a
	 * literal. To be overwritten in subclasses.
	 */
	protected String translateObject(String object,
			Map<String, String> dictionary) {
		if (FactComponent.isLiteral(object))
			return (object);
		return (dictionary.get(object));
	}

	@Override
	public void extract() throws Exception {

		Map<String, String> subjectDictionary = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language).dictionary();
		Map<String, String> objectDictionary = objectDictionaryTheme
				.dictionary();

		for (Fact f : checkMe) {
			String translatedSubject = subjectDictionary.get(f
					.getSubject());
			if (translatedSubject == null)
				continue;
			String translatedObject = translateObject(f.getObject(), objectDictionary);
			if (translatedObject == null)
				continue;
			checked.write(new Fact(translatedSubject, f.getRelation(),
					translatedObject));
		}
	}

	public EntityTranslator(Theme in, Theme out, Extractor parent) {
		super(in, out, parent);
		this.language = in.language();
		if (language == null || FactComponent.isEnglish(language))
			throw new RuntimeException(
					"Don't translate English. This is useless and very costly.");
		// By default, we translate entities.
		// May be overwritten in subclasses
		objectDictionaryTheme = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language);
	}

}
