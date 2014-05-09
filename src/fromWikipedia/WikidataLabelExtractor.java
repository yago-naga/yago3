package fromWikipedia;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;

/**
 * WikidataLabelExtractor - YAGO2s
 * 
 * Extracts labels from wikidata
 * 
 * @author Fabian
 * 
 */
public class WikidataLabelExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<Theme>(TransitiveTypeExtractor.TRANSITIVETYPE,
				InterLanguageLinks.INTERLANGUAGELINKS,
				PatternHardExtractor.LANGUAGECODEMAPPING);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<Theme>(TransitiveTypeExtractor.TRANSITIVETYPE,
				PatternHardExtractor.LANGUAGECODEMAPPING);
	}

	/** Facts deduced from categories */
	public static final Theme WIKIPEDIALABELS = new Theme("wikipediaLabels",
			"Labels derived from the name of the instance in Wikipedia");

	/** Sources */
	public static final Theme WIKIPEDIALABELSOURCES = new Theme(
			"wikipediaLabelSources", "Sources for the Wikipedia labels");

	/** Facts deduced from categories */
	public static final Theme WIKIDATAMULTILABELS = new Theme(
			"wikidataMultiLabels", "Labels from Wikidata in multiple languages");

	/** Sources */
	public static final Theme WIKIDATAMULTILABELSOURCES = new Theme(
			"wikidataMultiLabelSources", "Sources for the multilingual labels");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(WIKIPEDIALABELSOURCES, WIKIPEDIALABELS,
				WIKIDATAMULTILABELSOURCES, WIKIDATAMULTILABELS);
	}

	@Override
	public void extract() throws Exception {
		FactCollection languagemap = PatternHardExtractor.LANGUAGECODEMAPPING
				.factCollection();
		Set<String> entities = TransitiveTypeExtractor.TRANSITIVETYPE
				.factCollection().getSubjects();
		String lastGuy = "";
		for (Fact f : InterLanguageLinks.INTERLANGUAGELINKS.factSource()) {
			if (!entities.contains(f.getSubject()))
				continue;
			if (!f.getRelation().equals(RDFS.label))
				continue;
			// Write out standard names
			if (!lastGuy.equals(f.getSubject())) {
				lastGuy = f.getSubject();
				write(WIKIPEDIALABELS,
						new Fact(f.getSubject(), YAGO.hasPreferredName,
								FactComponent.forStringWithLanguage(
										preferredName(lastGuy), "eng")),
						WIKIPEDIALABELSOURCES, "Wikidata",
						"WikidataLabelExtractor");
				for (String name : trivialNamesOf(f.getSubject())) {
					write(WIKIPEDIALABELS, new Fact(f.getSubject(), RDFS.label,
							FactComponent.forStringWithLanguage(name, "eng")),
							WIKIPEDIALABELSOURCES, "Wikidata",
							"WikidataLabelExtractor");
				}
			}
			String label = f.getObjectAsJavaString();
			String lan = FactComponent.getLanguage(f.getObject());
			if (lan.length() == 2)
				lan = languagemap
						.getObject(lan, "<hasThreeLetterLanguageCode>");
			if (lan == null || lan.length() != 3)
				continue;
			write(WIKIDATAMULTILABELS,
					new Fact(f.getSubject(), YAGO.hasPreferredName,
							FactComponent.forStringWithLanguage(label, lan)),
					WIKIDATAMULTILABELSOURCES, "Wikidata",
					"WikidataLabelExtractor");
		}
	}

	/** returns the (trivial) names of an entity */
	public static Set<String> trivialNamesOf(String titleEntity) {
		Set<String> result = new TreeSet<>();
		String name = preferredName(titleEntity);
		result.add(name);
		String norm = Char.normalize(name);
		if (!norm.contains("[?]"))
			result.add(norm);
		if (name.contains(" (")) {
			result.add(name.substring(0, name.indexOf(" (")).trim());
		}
		if (name.contains(",") && !name.contains("(")) {
			result.add(name.substring(0, name.indexOf(",")).trim());
		}
		return (result);
	}

	/** returns the preferred name */
	public static String preferredName(String titleEntity) {
		return (Char.decode(FactComponent.stripBracketsAndLanguage(titleEntity)
				.replace('_', ' ')));
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new HardExtractor(new File("../basics2s/data")).extract(new File(
				"c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
		new PatternHardExtractor(new File("./data")).extract(new File(
				"c:/fabian/data/yago2s"), "Test on 1 wikipedia article");
	}
}
