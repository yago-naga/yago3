package followUp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import utils.FactCollection;
import utils.Theme;
import javatools.datatypes.FinalSet;
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
	protected Theme objectDictionary;

	/** Caches the dictionary of subjects */
	protected FactCollection subjectDictionaryCache;

	/** Caches the dictionary of objects */
	protected FactCollection objectDictionaryCache;

	@Override
	public Set<Theme> input() {
		// Do not use a FinalSet here because
		// objectDictionary might be equivalent to
		// entiyDictionary
		return (new HashSet<>(Arrays.asList(checkMe,
				DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language),
				objectDictionary)));
	}

	@Override
	public Set<Theme> inputCached() {
		// Do not use a FinalSet here because
		// objectDictionary might be equivalent to
		// entiyDictionary
		return (new HashSet<>(Arrays.asList(
				DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language),
				objectDictionary)));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	/** Translates an entity, returns the entity itself by default */
	protected String translateSubject(String me) {
		String trans = subjectDictionaryCache
				.getObject(me, "<_hasTranslation>");
		// Translate to itself?
		// If so, we get duplicates in case of missing links
		//if (trans == null) return (me);
		return (trans);
	}

	/** Translates an entity, returns the entity itself by default */
	protected String translateObject(String me) {
		if (FactComponent.isLiteral(me))
			return (me);
		return (translateSubject(me));
	}

	@Override
	public void extract() throws Exception {

		subjectDictionaryCache = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language).factCollection();

		objectDictionaryCache = objectDictionary.factCollection();

		for (Fact f : checkMe) {
			String translatedSubject = translateSubject(f.getSubject());
			String translatedObject = translateObject(f.getObject());
			if (translatedSubject != null && translatedObject != null)
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
		objectDictionary = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language);
	}

}
