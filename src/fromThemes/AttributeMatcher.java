package fromThemes;

import java.io.File;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.util.FileUtils;
import basics.BaseTheme;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.Theme;
import extractors.MultilingualExtractor;

/**
 * YAGO2s - AttributeMatcher
 * 
 * This Extractor matches multilingual Infobox attributes to English attributes.
 * 'German' can be replaced by any arbitrary language as second language.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class AttributeMatcher extends MultilingualExtractor {

	/** Minimum requires support for output */
	public static final int MINSUPPORT = 5;

	private static FactCollection yagoFacts = null;

	public static final BaseTheme MATCHED_INFOBOXATTS = new BaseTheme(
			"matchedAttributes",
			"Attributes of the Wikipedia infoboxes in different languages with their YAGO counterparts.");

	public static final BaseTheme MATCHED_INFOBOXATTS_SOURCES = new BaseTheme(
			"matchedAttributeSources",
			"Sources for the attributes of the Wikipedia infoboxes in different languages with their YAGO counterparts.");

	@Override
	public Set<Theme> input() {
		HashSet<Theme> result = new HashSet<Theme>(
				Arrays.asList(InfoboxMapper.INFOBOXFACTS.inLanguage("en"),
						InfoboxTermExtractor.INFOBOXATTSTRANSLATED
								.inLanguage(language)));
		return result;
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(InfoboxMapper.INFOBOXFACTS.inLanguage("en"));
	}

	@Override
	public Set<Theme> output() {
		return new HashSet<>(Arrays.asList(
				MATCHED_INFOBOXATTS.inLanguage(language),
				MATCHED_INFOBOXATTS_SOURCES.inLanguage(language)));
	}

	@Override
	public void extract() throws Exception {

		Theme out = MATCHED_INFOBOXATTS.inLanguage(language);
		Writer tsv = FileUtils.getBufferedUTF8Writer(new File(out.file()
				.getParent(), "attributeMatches_" + language + ".tsv"));
		Theme sources = MATCHED_INFOBOXATTS_SOURCES.inLanguage(language);

		Theme germanFacts = InfoboxTermExtractor.INFOBOXATTSTRANSLATED
				.inLanguage(language);
		// Counts, for every german attribute, how often it appears with every
		// YAGO relation
		Map<String, Map<String, Integer>> german2yago2count = new HashMap<String, Map<String, Integer>>();
		// Counts, for every german attribute, how often it appears with every
		// YAGO relation but is false
		Map<String, Map<String, Integer>> german2yagowrong2count = new HashMap<String, Map<String, Integer>>();

		// Counts for every german attribute how many facts there are
		Map<String, Integer> germanFactCountPerAttribute = new HashMap<>();

		yagoFacts = InfoboxMapper.INFOBOXFACTS.inLanguage("en")
				.factCollection();
		for (Fact germanFact : germanFacts.factSource()) {
			String germanRelation = germanFact.getRelation();
			String germanSubject = germanFact.getArg(1);
			String germanObject = germanFact.getArg(2);
			/*
			 * We look for German attributes where both subject and object (in
			 * translated form) appear in YAGO. If the attribute is skipped at
			 * this level, it still has the chance to be processed if appears
			 * with 'good' subject and object in other facts
			 */
			if (!yagoFacts.containsSubject(germanSubject)
					|| !yagoFacts.containsObject(germanObject))
				continue;

			// We increase the counter for the attribute of the german fact
			D.addKeyValue(germanFactCountPerAttribute, germanRelation, 1);
			// System.out.println(germanFactCountPerAttribute.get(germanRelation));

			// Let's now see to which YAGO relations the german attribute could
			// map
			Map<String, Integer> germanMap = german2yago2count
					.get(germanRelation);
			if (germanMap == null)
				german2yago2count.put(germanRelation,
						germanMap = new HashMap<String, Integer>());
			Map<String, Integer> germanWrongMap = german2yagowrong2count
					.get(germanRelation);
			if (germanWrongMap == null)
				german2yagowrong2count.put(germanRelation,
						germanWrongMap = new HashMap<String, Integer>());

			for (String yagoRelation : yagoFacts.getRelations(germanSubject)) {
				Set<String> yagoObjects = yagoFacts.collectObjects(
						germanSubject, yagoRelation);
				if (yagoObjects.contains(germanObject))
					D.addKeyValue(germanMap, yagoRelation, 1);
				else
					D.addKeyValue(germanWrongMap, yagoRelation, 1);
			}
		}

		// Now output the results:
		for (String germanAttribute : german2yago2count.keySet()) {
			for (String yagoRelation : german2yago2count.get(germanAttribute)
					.keySet()) {
				int correct = german2yago2count.get(germanAttribute).get(
						yagoRelation);
				if (correct < MINSUPPORT)
					continue;
				int wrong = german2yagowrong2count.get(germanAttribute).get(
						yagoRelation);
				int total = germanFactCountPerAttribute.get(germanAttribute);

				Fact matchFact = new Fact(germanAttribute, "<_infoboxPattern>",
						yagoRelation);
				matchFact.makeId();
				write(out, matchFact, sources,
						FactComponent.forTheme(germanFacts), "Counting");
				out.write(new Fact(matchFact.getId(), "<_hasTotal>",
						FactComponent.forNumber(total)));
				out.write(new Fact(matchFact.getId(), "<_hasCorrect>",
						FactComponent.forNumber(correct)));
				out.write(new Fact(matchFact.getId(), "<_hasIncorrect>",
						FactComponent.forNumber(wrong)));
				tsv.write(germanAttribute + "\t" + yagoRelation + "\t" + total
						+ "\t" + correct + "\t" + wrong + "\n");
			}
		}
		tsv.close();
	}

	public AttributeMatcher(String secondLang) {
		super(secondLang);
	}

	public static void main(String[] args) throws Exception {

		new AttributeMatcher("de").extract(new File("D:/data3/yago2s"),
				"mapping infobox attributes in different languages");
	}

}
