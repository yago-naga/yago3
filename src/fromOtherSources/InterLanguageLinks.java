package fromOtherSources;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
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

	public InterLanguageLinks(File inputFolder) {
		this.inputFile = inputFolder.isFile() ? inputFolder : new File(
				inputFolder, "wikidata.rdf");
		if (!inputFile.exists())
			throw new RuntimeException("File not found: " + inputFile);
	}

	@Override
	public Set<Theme> input() {
		return Collections.emptySet();
	}

	@Override
	public Set<Theme> output() {
		return (new FinalSet<Theme>(INTERLANGUAGELINKS));
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
	public void extract(File input, FactWriter writer) throws Exception {
		N4Reader nr = new N4Reader(input);
		// Maps a language such as "en" to the name in that language
		Map<String, String> language2name = new HashMap<String, String>();
		while (nr.hasNext()) {
			Fact f = nr.next();
			// D.p(f);
			// Record a new name in the map
			if (f.getRelation().endsWith("/inLanguage>")) {
				String[] parts = f.getArg(1).split("/");
				String name = parts[parts.length - 1];
				language2name.put(FactComponent.stripQuotes(f.getArg(2)), name);
			} else if (f.getArg(2).endsWith("#Item>")
					&& !language2name.isEmpty()) {
				// New item starts, find the most English name for the entity
				String mostEnglishLan = mostEnglishLanguage(language2name
						.keySet());
				String mostEnglishName = language2name.get(mostEnglishLan);
				mostEnglishName = Char.cutLast(mostEnglishName);
				for (Map.Entry<String, String> entry : language2name.entrySet()) {
					writer.write(new Fact(FactComponent.forYagoEntity(Char
							.decodePercentage(mostEnglishName)), "rdfs:label",
							FactComponent.forStringWithLanguage(Char
									.decodePercentage(Char.cutLast(entry
											.getValue())), entry.getKey())));
				}
				language2name.clear();
			}
		}
		nr.close();
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers,
			Map<Theme, FactSource> factCollections) throws Exception {
		Announce.doing("Copying language links");
		Announce.message("Input folder is", inputFile);
		extract(inputFile, writers.get(INTERLANGUAGELINKS));
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