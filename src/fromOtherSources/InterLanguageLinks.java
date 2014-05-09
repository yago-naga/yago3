package fromOtherSources;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.Theme;
import fromWikipedia.Extractor;

/**
 * YAGO2s - InterLanguageLinks
 * 
 * Extracts inter-language links from Wikidata.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class InterLanguageLinks extends Extractor {

	/** List of language suffixes from most English to least English. */
	public static final List<String> languages = Arrays.asList("en", "de",
			"fr", "es", "it");

	/** Input file */
	protected File inputFile;

	/** Output theme */
	public static final Theme INTERLANGUAGELINKS = new Theme(
			"yagoInterLanguageLinks",
			"The inter-language synonyms from Wikidata (http://http://www.wikidata.org/).");

	/** Words for "category" in different languages */
	public static final Theme CATEGORYWORDS = new Theme("categoryWords",
			"Words for 'category' in different languages.");

	/** Translations of infobox templates */
	public static final Theme INFOBOX_TEMPLATE_TRANSLATIONS = new Theme(
			"infoboxTemplateTranslations",
			"Template names in different languages.");

	/** Translations of categories */
	public static final Theme CATEGORY_TRANSLATIONS = new Theme(
			"categoryTranslations", "Category names in different languages.");

	public InterLanguageLinks(File inputFolder) {
		this.inputFile = inputFolder.isFile() ? inputFolder : new File(
				inputFolder, "wikidata.rdf");
		if (!inputFile.exists())
			throw new RuntimeException("File not found: " + inputFile);
	}

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(PatternHardExtractor.LANGUAGECODEMAPPING);
	}

	@Override
	public Set<Theme> output() {
		return (new FinalSet<Theme>(INTERLANGUAGELINKS, CATEGORYWORDS,
				CATEGORY_TRANSLATIONS, INFOBOX_TEMPLATE_TRANSLATIONS));
	}

	@Override
	public File inputDataFile() {
		return inputFile;
	}

	/** Returns the most English language in the set */
	public static String mostEnglishLanguage(Collection<String> langs) {
		for (int i = 0; i < languages.size(); i++) {
			if (langs.contains(languages.get(i)))
				return (languages.get(i));
		}
		// Otherwise take smallest language
		String smallest = "zzzzzzzzz";
		for (String l : langs) {
			if (smallest.compareTo(l) > 0)
				smallest = l;
		}
		return (smallest);
	}

	/** Extracts the language links from wikidata */
	public void extract(File input, Theme writer) throws Exception {
		Set<String> goodLanguages = new HashSet<>();
		goodLanguages.addAll(PatternHardExtractor.LANGUAGECODEMAPPING
				.factCollection().getSubjects());
		goodLanguages.addAll(PatternHardExtractor.LANGUAGECODEMAPPING
				.factCollection().getObjects());

		// Categories for which we have already translated the word "category"
		Set<String> categoryWordLanguages = new HashSet<>();

		N4Reader nr = new N4Reader(input);
		// Maps a language such as "en" to the name in that language
		Map<String, String> language2name = new HashMap<String, String>();
		while (nr.hasNext()) {
			Fact f = nr.next();
			// Record a new name in the map
			if (f.getRelation().endsWith("/inLanguage>")) {
				String lan = FactComponent.stripQuotes(f.getObject());
				if (!goodLanguages.contains(lan))
					continue;
				language2name.put(lan, FactComponent.stripPrefix(Char
						.decodePercentage(f.getSubject())));
			} else if (f.getArg(2).endsWith("#Item>")
					&& !language2name.isEmpty()) {
				// New item starts, let's flush out the previous one
				String mostEnglishLan = mostEnglishLanguage(language2name
						.keySet());
				String mostEnglishName = language2name.get(mostEnglishLan);
				for (String lan : language2name.keySet()) {
					writer.write(new Fact(FactComponent.forForeignYagoEntity(
							mostEnglishName, mostEnglishLan), "rdfs:label",
							FactComponent.forStringWithLanguage(
									language2name.get(lan), lan)));
				}
				if (mostEnglishLan.equals("en")
						&& mostEnglishName.startsWith("Category:")) {
					for (String lan : language2name.keySet()) {
						String catword = language2name.get(lan);
						int cutpos = catword.indexOf(':');
						if (cutpos == -1)
							continue;
						String name = catword.substring(cutpos + 1);
						catword = catword.substring(cutpos);
						if (!categoryWordLanguages.contains(lan)) {
							CATEGORYWORDS.write(new Fact(FactComponent
									.forString(lan), "<_hasCategoryWord>",
									FactComponent.forString(catword)));
						}
						CATEGORY_TRANSLATIONS.write(new Fact(FactComponent
								.forString(name), "<_translatedFrom/" + lan
								+ ">", FactComponent.forString(mostEnglishName
								.substring(8))));
					}
				}
				if (mostEnglishLan.equals("en")
						&& mostEnglishName.startsWith("Template:Infobox_")) {
					for (String lan : language2name.keySet()) {
						String name = language2name.get(lan);
						int cutpos = name.indexOf('_');
						if (cutpos == -1)
							continue;
						name = name.substring(cutpos + 1);
						INFOBOX_TEMPLATE_TRANSLATIONS.write(new Fact(
								FactComponent.forString(name),
								"<_translatedFrom/" + lan + ">", FactComponent
										.forString(mostEnglishName
												.substring(17))));
					}
				}
				language2name.clear();
			}
		}
		nr.close();
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Copying language links");
		Announce.message("Input folder is", inputFile);
		extract(inputFile, INTERLANGUAGELINKS);
		Announce.done();
	}

	public static void main(String[] args) {
		// try {
		// new InterLanguageLinks(new File("D:/wikidata.rdf"))
		// .extract(new File("D:/data2/yago2s/"), "test");
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		try {
			new InterLanguageLinks(new File("./data/wikidata.rdf")).extract(
					new File("../"), "test");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}