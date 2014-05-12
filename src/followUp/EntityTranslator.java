package followUp;

import java.util.Set;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.Theme;
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

	/** Dictionary for the objects */
	protected Theme objectDictionary;

	@Override
	public Set<Theme> input() {
		return (new FinalSet<>(checkMe,
				DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language),
				objectDictionary));
	}

	@Override
	public Set<Theme> inputCached() {
		return (new FinalSet<>(
				DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language),
				objectDictionary));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	/** Translates a word, returns default by default */
	protected static String translate(String me, FactCollection dic, String def) {
		String trans = dic.getObject(me, "<_hasTranslation>");
		if (trans == null)
			return (def);
		return (trans);
	}

	@Override
	public void extract() throws Exception {

		FactCollection subjectDictionaryCache = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language).factCollection();
		FactCollection objectDictionaryCache = objectDictionary
				.factCollection();

		for (Fact f : checkMe) {
			String translatedSubject = translate(f.getSubject(),
					subjectDictionaryCache, f.getSubject());
			String translatedObject = translate(f.getObject(),
					objectDictionaryCache, f.getObject());
			checked.write(new Fact(translatedSubject, f.getRelation(),
					translatedObject));
		}
	}

	public EntityTranslator(Theme in, Theme out, Extractor parent) {
		super(in, out, parent);
		this.language = in.language();
		if (language == null || language.equals("en"))
			throw new RuntimeException(
					"Don't translate English. This is useless and very costly.");
		// By default, we translate entities.
		// May be overwritten in subclasses
		objectDictionary = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language);
	}

}
