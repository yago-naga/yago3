package fromWikipedia;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import followUp.FollowUpExtractor;
import fromOtherSources.InterLanguageLinks;

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

	public enum ObjectType {
		Entity, InfoboxType, Category
	}

	protected ObjectType objectType;

	@Override
	public Set<Theme> input() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(checkMe);
		result.add(InterLanguageLinks.INTERLANGUAGELINKS);
		switch (objectType) {
		case InfoboxType:
			result.add(InterLanguageLinks.INFOBOX_TEMPLATE_TRANSLATIONS);
			break;
		case Category:
			result.add(InterLanguageLinks.CATEGORY_TRANSLATIONS);
		default:
		}
		return (result);
	}

	@Override
	public Set<Theme> inputCached() {
		Set<Theme> result = new HashSet<Theme>();
		switch (objectType) {
		case InfoboxType:
			result.add(InterLanguageLinks.INFOBOX_TEMPLATE_TRANSLATIONS);
			break;
		case Category:
			result.add(InterLanguageLinks.CATEGORY_TRANSLATIONS);
		default:
		}
		return (result);
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	@Override
	public void extract() throws Exception {

		Map<String, String> foreign2yagoentity = new HashMap<String, String>();
		for(Fact f : InterLanguageLinks.INTERLANGUAGELINKS.factSource()) {
			
		}

		for (Fact f : checkMe.factSource()) {

			/*
			 * If subject or object is not found in the dictionary, it remains
			 * the same as itself. All the subjects are assumed to be entities.
			 */

			String translatedSubject = FactComponent.stripBrackets(f.getArg(1));
			String translatedObject = f.getArg(2);

			if (foreign2yagoentity.containsKey(translatedSubject)) {
				translatedSubject = foreign2yagoentity.get(translatedSubject);
			}

			if (objectType.equals("Entity")) {

				// For non-literals: translate the entity and put back into
				// yagoEntity form
				if (!FactComponent.isLiteral(translatedObject)) {

					translatedObject = FactComponent
							.stripBrackets(translatedObject);

					if (foreign2yagoentity.containsKey(translatedObject)) {
						translatedObject = foreign2yagoentity.get(translatedObject);
					}
					translatedObject = FactComponent.forUri(translatedObject);
				}

				checked.write(new Fact(FactComponent
						.forYagoEntity(translatedSubject), f.getRelation(),
						translatedObject));

			} else if (objectType.equals("Infobox")) {

				String infoboxWord = InterLanguageLinksDictionary
						.getInfDictionary(
								input.get(InterLanguageLinks.INTERLANGUAGELINKS))
						.get(language);

				String word = FactComponent.stripBrackets(f.getArg(2).replace(
						" ", "_"));
				if (word.length() > 0)
					word = word.substring(0, 1).toUpperCase()
							+ word.substring(1);

				translatedObject = infoboxWord + "_" + word;

				if (foreign2yagoentity.containsKey(translatedObject))
					translatedObject = foreign2yagoentity.get(translatedObject);

				if (translatedObject.contains("_")) {
					translatedObject = translatedObject
							.substring(translatedObject.lastIndexOf("_") + 1);
				}

				output.get(checked).write(
						new Fact(
								FactComponent.forYagoEntity(translatedSubject),
								"<hasInfoboxType/en>", FactComponent
										.forString(translatedObject)));

			} else if (objectType.equals("Category")) {

				String categoryWord = InterLanguageLinksDictionary
						.getCatDictionary(
								input.get(InterLanguageLinks.INTERLANGUAGELINKS))
						.get(language);
				translatedObject = categoryWord
						+ ":"
						+ FactComponent.stripQuotes(f.getArg(2).replace(" ",
								"_"));
				if (foreign2yagoentity.containsKey(translatedObject))
					translatedObject = foreign2yagoentity.get(translatedObject);

				if (translatedObject.contains(":")) {
					translatedObject = translatedObject
							.substring(translatedObject.lastIndexOf(":") + 1);
				}

				checked.write(new Fact(FactComponent
						.forYagoEntity(translatedSubject),
						"<hasWikiCategory/en>", FactComponent
								.forString(translatedObject)));
			}
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
