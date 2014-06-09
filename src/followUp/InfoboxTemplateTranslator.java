package followUp;

import java.util.Map;

import utils.Theme;
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
	protected String translateObject(String object,
			Map<String, String> dictionary) {
		return dictionary.get(object);
	}

	public InfoboxTemplateTranslator(Theme in, Theme out, Extractor parent) {
		super(in, out, parent);
		objectDictionaryTheme = DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY
				.inLanguage(language);
	}
}
