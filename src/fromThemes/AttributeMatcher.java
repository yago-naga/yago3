package fromThemes;

import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.MultilingualTheme;
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

	public static final MultilingualTheme MATCHED_INFOBOXATTS = new MultilingualTheme(
			"matchedAttributes",
			"Attributes of the Wikipedia infoboxes in different languages with their YAGO counterparts.");

	@Override
	public Set<Theme> input() {
		return new FinalSet<Theme>(InfoboxMapper.INFOBOXFACTS.inEnglish(),
				isEnglish() ? InfoboxTermExtractor.INFOBOXTERMS
						.inEnglish()
						: InfoboxTermExtractor.INFOBOXTERMSTRANSLATED
								.inLanguage(language));
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(InfoboxMapper.INFOBOXFACTS.inLanguage("en"));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(MATCHED_INFOBOXATTS.inLanguage(language));
	}

	@Override
	public void extract() throws Exception {

		Theme out = MATCHED_INFOBOXATTS.inLanguage(language);
		Writer tsv = FileUtils.getBufferedUTF8Writer(new File(out.file()
				.getParent(), "_attributeMatches_" + language + ".tsv"));

		Theme germanFacts = isEnglish() ? InfoboxTermExtractor.INFOBOXTERMS
				.inLanguage(language)
				: InfoboxTermExtractor.INFOBOXTERMSTRANSLATED
						.inLanguage(language);
		// Counts, for every german attribute, how often it appears with every
		// YAGO relation
		Map<String, Map<String, Integer>> german2yago2count = new HashMap<String, Map<String, Integer>>();
		// Counts, for every german attribute, how often it appears with every
		// YAGO relation but is false
		Map<String, Map<String, Integer>> german2yagowrong2count = new HashMap<String, Map<String, Integer>>();

		// Counts for every german attribute how many facts there are
		Map<String, Integer> germanFactCountPerAttribute = new HashMap<>();

		yagoFacts = InfoboxMapper.INFOBOXFACTS.inEnglish()
				.factCollection();

		// We collect all objects for a subject and a relation
		// to account for the fact that we have the same object
		// in several types at this point.
		String currentSubject = "";
		String currentRelation = "";
		Set<String> currentObjects = new HashSet<>();

		int before=0;
		int after=0;
		int third=0;
		int fourth=0;
		
		for (Fact germanFact : germanFacts) {
			String germanRelation = germanFact.getRelation();
			String germanSubject = germanFact.getArg(1);
			String germanObject = germanFact.getArg(2);
			/*
			 * We look for German attributes where both subject and object (in
			 * translated form) appear in YAGO. If the attribute is skipped at
			 * this level, it still has the chance to be processed if appears
			 * with 'good' subject and object in other facts
			 */
			if(germanRelation.equals("<infobox/de/namerumänisch>")) before++;
			
			if (!yagoFacts.containsSubject(germanSubject))
				continue;
			
			if(germanRelation.equals("<infobox/de/namerumänisch>")) after++;
			
			//if (!FactComponent.isLiteral(germanObject)
			//		&& !yagoFacts.containsObject(germanObject))
			//	continue;

			if (currentSubject.equals(germanSubject)
					&& currentRelation.equals(germanRelation)) {
				currentObjects.add(germanObject);
				continue;
			}

			if(germanRelation.equals("<infobox/de/namerumänisch>")) third++;

			// Come here if we have a new relation.
			// Treat currentRelation and currentObjects
			if (!currentObjects.isEmpty()) {
				if(currentRelation.equals("<infobox/de/namerumänisch>")) fourth++;
				// We increase the counter for the attribute of the german fact
				D.addKeyValue(germanFactCountPerAttribute, currentRelation, 1);
				// System.out.println(germanFactCountPerAttribute.get(germanRelation));

				// Let's now see to which YAGO relations the german attribute
				// could
				// map
				Map<String, Integer> germanMap = german2yago2count
						.get(currentRelation);
				if (germanMap == null)
					german2yago2count.put(currentRelation,
							germanMap = new HashMap<String, Integer>());
				Map<String, Integer> germanWrongMap = german2yagowrong2count
						.get(currentRelation);
				if (germanWrongMap == null)
					german2yagowrong2count.put(currentRelation,
							germanWrongMap = new HashMap<String, Integer>());
				for (String yagoRelation : yagoFacts
						.getRelations(currentSubject)) {
					Set<String> yagoObjects = yagoFacts.collectObjects(
							currentSubject, yagoRelation);
					if (containsAny(yagoObjects, currentObjects))
						D.addKeyValue(germanMap, yagoRelation, 1);
					else
						D.addKeyValue(germanWrongMap, yagoRelation, 1);
				}
			}

			currentSubject = germanSubject;
			currentRelation = germanRelation;
			currentObjects.clear();
			currentObjects.add(germanObject);
		}

		// Now output the results:
		for (String germanAttribute : german2yago2count.keySet()) {
			for (String yagoRelation : german2yago2count.get(germanAttribute)
					.keySet()) {
				int correct = german2yago2count.get(germanAttribute).get(
						yagoRelation);
				if (correct < MINSUPPORT)
					continue;
				Integer wrong = german2yagowrong2count.get(germanAttribute).get(
						yagoRelation);
				if(wrong==null) wrong=0;
				int total = germanFactCountPerAttribute.get(germanAttribute);

				Fact matchFact = new Fact(germanAttribute, "<_infoboxPattern>",
						yagoRelation);
				matchFact.makeId();
				out.write(matchFact);
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
		
		D.p(before,after,third,fourth);
	}

	/** TRUE if intersection is non-empty */
	public static <K> boolean containsAny(Set<K> yagoObjects,
			Set<K> currentObjects) {
		for (K k : yagoObjects) {
			if (currentObjects.contains(k))
				return (true);
		}
		return false;
	}

	public AttributeMatcher(String secondLang) {
		super(secondLang);
	}

	public static void main(String[] args) throws Exception {

		new AttributeMatcher("de").extract(new File(args[0]),
				"mapping infobox attributes in different languages");
	}

}
