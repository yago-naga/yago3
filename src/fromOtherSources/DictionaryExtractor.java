package fromOtherSources;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.parsers.Char;
import basics.MultilingualTheme;
import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.Theme;
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
		return Collections.emptySet();
	}

	@Override
	public Set<Theme> output() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(CATEGORYWORDS);
		result.addAll(CATEGORY_DICTIONARY
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		result.addAll(ENTITY_DICTIONARY
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		result.addAll(INFOBOX_TEMPLATE_DICTIONARY
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
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
		Announce.message("Input file is", inputData);
		Announce.message("There are 110m facts. On the real Wikidata file on a laptop, this process takes ages.");
		Announce.message("Even if the output files do not seem to fill, they do fill eventually.");

		// Categories for which we have already translated the word "category"
		Set<String> categoryWordLanguages = new HashSet<>();

		N4Reader nr = new N4Reader(inputData);
		// int counter=0;
		// Maps a language such as "en" to the name in that language
		Map<String, String> language2name = new HashMap<String, String>();
		while (nr.hasNext()) {
			// if(++counter%1000000==0)
			// Announce.message("Parsed facts:",counter);
			Fact f = nr.next();
			// D.p(f);
			// Record a new name in the map
			if (f.getRelation().endsWith("/inLanguage>")) {
				String lan = FactComponent.stripQuotes(f.getObject());
				if (!MultilingualExtractor.wikipediaLanguages.contains(lan))
					continue;
				language2name.put(lan, FactComponent.stripPrefix(Char
						.decodePercentage(f.getSubject())));
			} else if (f.getObject().endsWith("#Item>")
					&& !language2name.isEmpty()) {
				// New item starts, let's flush out the previous one
				String mostEnglishLan = mostEnglishLanguage(language2name
						.keySet());
				if (mostEnglishLan != null) {
					String mostEnglishName = language2name.get(mostEnglishLan);
					if (mostEnglishLan.equals("en")
							&& mostEnglishName.startsWith("Category:")) {
						for (String lan : language2name.keySet()) {
							String catword = language2name.get(lan);
							int cutpos = catword.indexOf(':');
							if (cutpos == -1)
								continue;
							String name = catword.substring(cutpos + 1);
							catword = catword.substring(0, cutpos);
							if (!categoryWordLanguages.contains(lan)) {
								CATEGORYWORDS.write(new Fact(FactComponent
										.forString(lan), "<_hasCategoryWord>",
										FactComponent.forString(catword)));
								categoryWordLanguages.add(lan);
							}
							CATEGORY_DICTIONARY
									.inLanguage(lan)
									.write(new Fact(
											FactComponent
													.forForeignWikiCategory(
															name, lan),
											"<_hasTranslation>",
											FactComponent
													.forWikiCategory(mostEnglishName
															.substring(9))));
						}
					} else if (mostEnglishLan.equals("en")
							&& mostEnglishName.startsWith("Template:Infobox_")) {
						for (String lan : language2name.keySet()) {
							String name = language2name.get(lan);
							int cutpos = name.indexOf('_');
							if (cutpos == -1)
								continue;
							name = FactComponent.forInfoboxTemplate(
									name.substring(cutpos + 1), lan);
							INFOBOX_TEMPLATE_DICTIONARY.inLanguage(lan).write(
									new Fact(name, "<_hasTranslation>",
											FactComponent.forInfoboxTemplate(
													mostEnglishName
															.substring(17),
													"en")));
						}
					} else {
						for (String lan : language2name.keySet()) {
							ENTITY_DICTIONARY
									.inLanguage(lan)
									.write(new Fact(
											FactComponent
													.forForeignYagoEntity(
															language2name
																	.get(lan),
															lan),
											"<_hasTranslation>", FactComponent
													.forForeignYagoEntity(
															mostEnglishName,
															mostEnglishLan)));
						}
					}
				}
				language2name.clear();
			}
		}
		nr.close();
	}

	public static void main(String[] args) throws Exception {
		new DictionaryExtractor(new File("./data/wikidata.rdf")).extract(
				new File("c:/fabian/data/yago3"), "test");
	}

}