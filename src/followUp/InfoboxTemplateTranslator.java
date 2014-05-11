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
	public InfoboxTemplateTranslator(Theme in, Theme out, Extractor parent) {
		super(in, out, parent);
		objectDictionary = DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY
				.inLanguage(language);
	}
}
