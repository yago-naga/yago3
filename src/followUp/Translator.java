package followUp;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.Theme;
import fromOtherSources.DictionaryExtractor;
import fromWikipedia.CategoryExtractor;

/**
 * EntityTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English
 * language.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class Translator extends FollowUpExtractor {

	protected String language;

	public enum ObjectType {
		Entity, InfoboxType, Category
	}

	protected ObjectType objectType;

	@Override
	public Set<Theme> input() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(checkMe);
		result.add(DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language));
		switch (objectType) {
		case InfoboxType:
			result.add(DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY
					.inLanguage(language));
			break;
		case Category:
			result.add(DictionaryExtractor.CATEGORY_DICTIONARY
					.inLanguage(language));
		default:
		}
		return (result);
	}

	@Override
	public Set<Theme> inputCached() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(DictionaryExtractor.ENTITY_DICTIONARY.inLanguage(language));
		switch (objectType) {
		case InfoboxType:
			result.add(DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY
					.inLanguage(language));
			break;
		case Category:
			result.add(DictionaryExtractor.CATEGORY_DICTIONARY
					.inLanguage(language));
		default:
		}
		return (result);
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

		FactCollection foreign2yagoentity = DictionaryExtractor.ENTITY_DICTIONARY
				.inLanguage(language).factCollection();

		switch (objectType) {
		case Entity:
			for (Fact f : checkMe.factSource()) {
				String translatedSubject = translate(f.getSubject(),
						foreign2yagoentity, f.getSubject());
				String translatedObject = FactComponent
						.isLiteral(f.getObject()) ? f.getObject() : translate(
						f.getObject(), foreign2yagoentity, f.getObject());
				checked.write(new Fact(translatedSubject, f.getRelation(),
						translatedObject));
			}
			break;
		case InfoboxType:
			FactCollection infobox2english = DictionaryExtractor.INFOBOX_TEMPLATE_DICTIONARY
					.inLanguage(language).factCollection();
			for (Fact f : checkMe.factSource()) {
				String translatedSubject = translate(f.getSubject(),
						foreign2yagoentity, f.getSubject());
				String translatedObject = translate(f.getObject(),
						infobox2english, null);
				if (translatedObject != null)
					checked.write(new Fact(translatedSubject, f.getRelation(),
							translatedObject));
			}
			break;
		case Category:
			FactCollection cat2english = DictionaryExtractor.CATEGORY_DICTIONARY
					.inLanguage(language).factCollection();
			for (Fact f : checkMe.factSource()) {
				String translatedSubject = translate(f.getSubject(),
						foreign2yagoentity, f.getSubject());
				String translatedObject = translate(f.getObject(), cat2english,
						null);
				if (translatedObject != null)
					checked.write(new Fact(translatedSubject, f.getRelation(),
							translatedObject));
			}
			break;
		}
	}

	public Translator(Theme in, Theme out, String lang, ObjectType objectType) {
		this.checkMe = in;
		this.checked = out;
		this.language = lang;
		this.objectType = objectType;
	}

	public static void main(String[] args) throws Exception {
		Theme res = new Theme("res", "");
		new Translator(CategoryExtractor.CATEGORYMEMBERS.inLanguage("de"), res,
				"de", ObjectType.Category).extract(new File("D:/data3/yago2s"),
				"");
	}

}
