package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fromOtherSources.InterLanguageLinks;
import fromWikipedia.Extractor.FollowUpExtractor;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * EntityTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English language.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class Translator extends FollowUpExtractor {

	private String language;

	private String objectType;

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(checkMe,
				InterLanguageLinks.INTERLANGUAGELINKS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output,
			Map<Theme, FactSource> input) throws Exception {

		Map<String, String> tempDictionary = InterLanguageLinksDictionary.get(
				language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));

		for (Fact f : input.get(checkMe)) {

			/*
			 * If subject or object is not found in the dictionary, it remains
			 * the same as itself. All the subjects are assumed to be entities.
			 */

			String translatedSubject = FactComponent.stripBrackets(f.getArg(1));
			String translatedObject = f.getArg(2);
					
			if (tempDictionary.containsKey(translatedSubject)) {
				translatedSubject = tempDictionary.get(translatedSubject);
			}
			
			if (objectType.equals("Entity")) {

				// For non-literals: translate the entity and put back into yagoEntity form
				if (!FactComponent.isLiteral(translatedObject)) {
					
					translatedObject = FactComponent.stripBrackets(translatedObject);
					
					if (tempDictionary.containsKey(translatedObject)) {
						translatedObject = tempDictionary.get(translatedObject);
					}
					translatedObject = FactComponent.forUri(translatedObject);
				}

				output.get(checked).write(
						new Fact(
								FactComponent.forYagoEntity(translatedSubject),
								f.getRelation(), translatedObject));

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

				if (tempDictionary.containsKey(translatedObject))
					translatedObject = tempDictionary.get(translatedObject);

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
				if (tempDictionary.containsKey(translatedObject))
					translatedObject = tempDictionary.get(translatedObject);

				if (translatedObject.contains(":")) {
					translatedObject = translatedObject
							.substring(translatedObject.lastIndexOf(":") + 1);
				}

				output.get(checked).write(
						new Fact(
								FactComponent.forYagoEntity(translatedSubject),
								"<hasWikiCategory/en>", FactComponent
										.forString(translatedObject)));
			}
		}
	}

	public Translator(Theme in, Theme out, String lang, String objectType) {
		this.checkMe = in;
		this.checked = out;
		this.language = lang;
		this.objectType = objectType;
	}

	protected FactCollection loadFacts(FactSource factSource,
			FactCollection temp) {
		for (Fact f : factSource) {
			temp.add(f);
		}
		return (temp);
	}

	public static void main(String[] args) throws Exception {
		Theme res = new Theme("res", "");
		new Translator(CategoryExtractor.CATEGORYMEMBERS_MAP.get("de"), res,
				"de", "Category").extract(new File("D:/data3/yago2s"), "");
	}

}
