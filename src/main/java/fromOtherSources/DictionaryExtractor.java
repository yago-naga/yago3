package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;
import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import extractors.DataExtractor;
import extractors.MultilingualExtractor;

/**
 * YAGO2s - DictionaryExtractor
 * 
 * Extracts inter-language links from Wikidata and builds dictionaries.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class DictionaryExtractor extends DataExtractor {

	/** Output theme */
	public static final MultilingualTheme ENTITY_DICTIONARY = new MultilingualTheme(
			"entityDictionary",
			"Maps a foreign entity to a YAGO entity. Data from (http://http://www.wikidata.org/).");

	/** Words for "category" in different languages */
	public static final Theme CATEGORYWORDS = new Theme("categoryWords",
			"Words for 'category' in different languages.");

	/** Translations of infobox templates */
	public static final MultilingualTheme INFOBOX_TEMPLATE_DICTIONARY = new MultilingualTheme(
			"infoboxTemplateDictionary",
			"Maps a foreign infobox template name to the English name.");

	/**
	 * This TitleExtractor makes sure every foreign word gets mapped to a valid
	 * English one
	 */
	protected TitleExtractor titleExtractor;

	/** Translations of categories */
	public static final MultilingualTheme CATEGORY_DICTIONARY = new MultilingualTheme(
			"categoryDictionary",
			"Maps a foreign category name to the English name.");

	public DictionaryExtractor(File wikidata) {
		super(wikidata);
	}

	public DictionaryExtractor() {
		this(new File("./data/wikidata.rdf"));
	}

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.PREFMEANINGS);
	}

	@Override
	public Set<Theme> output() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(CATEGORYWORDS);
		result.addAll(CATEGORY_DICTIONARY.inLanguages(MultilingualExtractor
				.allLanguagesExceptEnglish()));
		result.addAll(ENTITY_DICTIONARY.inLanguages(MultilingualExtractor
				.allLanguagesExceptEnglish()));
		result.addAll(INFOBOX_TEMPLATE_DICTIONARY
				.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
		return (result);
	}

	/** Returns the most English language in the set, or NULL */
	public static String mostEnglishLanguage(Collection<String> langs) {
		for (int i = 0; i < MultilingualExtractor.wikipediaLanguages.size(); i++) {
			if (langs.contains(MultilingualExtractor.wikipediaLanguages.get(i)))
				return (MultilingualExtractor.wikipediaLanguages.get(i));
		}
		return (null);
	}

	@Override
	public void extract() throws Exception {
		// This TitleExtractor is used to filter out lists etc.
		// directly from the dictionary
		titleExtractor = new TitleExtractor("en");

		Announce.message("Input file is", inputData);
		Announce.message("There are 110m facts. On the real Wikidata file on a laptop, this process takes ages.");
		Announce.message("Even if the output files do not seem to fill, they do fill eventually.");

		// Categories for which we have already translated the word "category"
		Set<String> categoryWordLanguages = new HashSet<>();

		N4Reader nr = new N4Reader(inputData);
		// Maps a language such as "en" to the name in that language
		Map<String, String> language2name = new HashMap<String, String>();
		while (nr.hasNext()) {
			Fact f = nr.next();
			// Record a new name in the map
			if (f.getRelation().endsWith("/inLanguage>")) {
				String lan = FactComponent.stripQuotes(f.getObject());
				if (!MultilingualExtractor.wikipediaLanguages.contains(lan))
					continue;
				String object = FactComponent.stripBrackets(f.getSubject());
				object = object.substring(object.indexOf("/wiki/") + 6);
				object = Char17.decodePercentage(object);
				language2name.put(lan, object);
			} else if (f.getObject().endsWith("#Item>")
					&& !language2name.isEmpty()) {
				// New item starts, let's flush out the previous one
				String mostEnglishLan = mostEnglishLanguage(language2name
						.keySet());
				if (mostEnglishLan != null)
					flush(categoryWordLanguages, language2name, mostEnglishLan);
				language2name.clear();
			}
		}
		nr.close();
	}

	/** Flushes an entity, template, or category */
	private void flush(Set<String> categoryWordLanguages,
			Map<String, String> language2name, String mostEnglishLan)
			throws IOException {
		String mostEnglishName = language2name.get(mostEnglishLan);
		if (FactComponent.isEnglish(mostEnglishLan)
				&& mostEnglishName.startsWith("Category:")) {
			flushCategoryWord(categoryWordLanguages, language2name,
					mostEnglishName);
		} else if (FactComponent.isEnglish(mostEnglishLan)
				&& mostEnglishName.startsWith("Template:Infobox_")) {
			flushTemplateName(language2name, mostEnglishName);
		} else {
			flushEntity(language2name, mostEnglishLan, mostEnglishName);
		}
	}

	/** Flushes an entity */
	private void flushEntity(Map<String, String> language2name,
			String mostEnglishLan, String mostEnglishName) throws IOException {
		// Make sure that we exclude lists and general concepts
		// right up front.
		if (FactComponent.isEnglish(mostEnglishLan)) {
			if (titleExtractor.createTitleEntity(mostEnglishName.replace('_', ' ')) == null) {
				return;
			}
		}
		for (String lan : language2name.keySet()) {
			if (FactComponent.isEnglish(lan))
				continue;
			ENTITY_DICTIONARY.inLanguage(lan).write(
					new Fact(FactComponent.forForeignYagoEntity(
							language2name.get(lan), lan), "<_hasTranslation>",
							FactComponent.forForeignYagoEntity(mostEnglishName,
									mostEnglishLan)));
		}
	}

	/** Flushes a template */
	private void flushTemplateName(Map<String, String> language2name,
			String mostEnglishName) throws IOException {
		for (String lan : language2name.keySet()) {
			if (FactComponent.isEnglish(lan))
				continue;
			String name = language2name.get(lan);
			int cutpos = name.indexOf('_');
			if (cutpos == -1)
				continue;
			name = FactComponent.forInfoboxTemplate(name.substring(cutpos + 1),
					lan);
			INFOBOX_TEMPLATE_DICTIONARY.inLanguage(lan).write(
					new Fact(name, "<_hasTranslation>", FactComponent
							.forInfoboxTemplate(mostEnglishName.substring(17),
									"en")));
		}
	}

	/** Flushes a category word */
	private void flushCategoryWord(Set<String> categoryWordLanguages,
			Map<String, String> language2name, String mostEnglishName)
			throws IOException {
		for (String lan : language2name.keySet()) {
			String catword = language2name.get(lan);
			int cutpos = catword.indexOf(':');
			if (cutpos == -1)
				continue;
			String name = catword.substring(cutpos + 1);
			catword = catword.substring(0, cutpos);
			if (!categoryWordLanguages.contains(lan)) {
				CATEGORYWORDS
						.write(new Fact(FactComponent.forString(lan),
								"<_hasCategoryWord>", FactComponent
										.forString(catword)));
				categoryWordLanguages.add(lan);
			}
			if (!FactComponent.isEnglish(lan))
				CATEGORY_DICTIONARY.inLanguage(lan).write(
						new Fact(FactComponent
								.forForeignWikiCategory(name, lan),
								"<_hasTranslation>", FactComponent
										.forWikiCategory(mostEnglishName
												.substring(9))));
		}
	}

	public static void main(String[] args) throws Exception {
		new DictionaryExtractor(new File("./data/wikidata.rdf")).extract(
				new File("c:/fabian/data/yago3"), "test");
	}

}