package followUp;

import basics.Theme;
import extractors.Extractor;
import fromOtherSources.DictionaryExtractor;

/**
 * InfoboxTemplateTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English
 * language. Objects are infobox templates.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class InfoboxTemplateTranslator extends EntityTranslator {

	@Override
	protected String translateObject(String me) {
		// Return NULL in case we cannot translate
		String trans = objectDictionaryCache.getObject(me, "<_hasTranslation>");
		return (trans);
	}

	public InfoboxTemplateTranslator(Theme in, Theme out, Extractor parent) {
		super(in, out, parent);
		objectDictionary = DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY
				.inLanguage(language);
	}
}
