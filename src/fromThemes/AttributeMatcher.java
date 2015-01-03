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
import utils.AttributeMappingMeasure;
import utils.AttributeMappingMeasure.Wilson;
import utils.FactCollection;
import utils.MultilingualTheme;
import utils.Theme;
import basics.Fact;
import basics.RDFS;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.PatternHardExtractor;

/**
 * YAGO2s - AttributeMatcher
 * 
 * This Extractor matches multilingual Infobox attributes to English attributes.
 * 'German' can be replaced by any arbitrary language as second language.
 * 
 * @author Farzaneh Mahdisoltani
 */

public class AttributeMatcher extends MultilingualExtractor {

	/** Strategy for matching */
	public AttributeMappingMeasure measure = new Wilson(0.04);

	public static final MultilingualTheme MATCHED_INFOBOXATTS = new MultilingualTheme(
			"matchedAttributes",
			"Attributes of the Wikipedia infoboxes in different languages with their YAGO counterparts.");

	/**
	 * Our input theme. This is a separate field so that it can be set from
	 * outside for testing purposes.
	 */
	public Theme inputTheme;

	/**
	 * The English YAGO facts. This is a separate field so that it can be set
	 * from outside for testing purposes.
	 */
	public Theme referenceTheme = InfoboxMapper.INFOBOXFACTS.inEnglish();

	@Override
	public Set<Theme> input() {
		return new FinalSet<Theme>(referenceTheme, inputTheme, PatternHardExtractor.MULTILINGUALATTRIBUTES);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(referenceTheme, PatternHardExtractor.MULTILINGUALATTRIBUTES);
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

		Theme germanFacts = inputTheme;

		// Load the map of manual mappings
		Map<String,String> manualMapping=PatternHardExtractor.MULTILINGUALATTRIBUTES.factCollection().getMap("<_infoboxPattern>");

		// Counts, for every german attribute, how often it appears with every
		// YAGO relation
		Map<String, Map<String, Integer>> german2yago2count = new HashMap<String, Map<String, Integer>>();
		// Counts, for every german attribute, how often it appears with every
		// YAGO relation but is false
		Map<String, Map<String, Integer>> german2yagowrong2count = new HashMap<String, Map<String, Integer>>();

		// Counts for every german attribute how many facts there are
		Map<String, Integer> germanFactCountPerAttribute = new HashMap<>();

		FactCollection yagoFacts = referenceTheme.factCollection();

		// We collect all objects for a subject and a relation
		// to account for the fact that we have the same object
		// in several types at this point.
		String currentSubject = "";
		String currentRelation = "";
		Set<String> currentObjects = new HashSet<>();

		for (Fact germanFact : germanFacts) {
			String germanRelation = germanFact.getRelation();
			String germanSubject = germanFact.getArg(1);
			String germanObject = germanFact.getArg(2);

			if (currentSubject.equals(germanSubject)
					&& currentRelation.equals(germanRelation)) {
				currentObjects.add(germanObject);
				continue;
			}

			// Come here if we have a new relation.
			// Treat currentRelation and currentObjects
			if (!currentObjects.isEmpty()
					&& yagoFacts.containsSubject(currentSubject)) {
				// Let's now see to which YAGO relations the german attribute
				// could map
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
			D.addKeyValue(germanFactCountPerAttribute, currentRelation, 1);
		}

		// Now output the matches
		for (String germanAttribute : german2yago2count.keySet()) {
			// Find the best YAGO relation
			int bestCorrect = 0;
			int bestWrong = 0;
			int bestTotal = 0;
			String bestRelation = manualMapping.get(germanAttribute);
			if(bestRelation!=null) {
				bestCorrect=Integer.MAX_VALUE;
				bestWrong=0;
				bestTotal=bestCorrect;
				tsv.write(germanAttribute + "\t" + bestCorrect+ "\t" + bestTotal
						+ "\t" + bestCorrect+ "\t" + bestWrong+ "\n");
			}
			for (String yagoRelation : german2yago2count.get(germanAttribute)
					.keySet()) {
				int correct = german2yago2count.get(germanAttribute).get(
						yagoRelation);
				if (correct == 0)
					continue;
				Integer wrong = german2yagowrong2count.get(germanAttribute)
						.get(yagoRelation);
				if (wrong == null)
					wrong = 0;
				int total = germanFactCountPerAttribute.get(germanAttribute);
				if (correct > bestCorrect) {
					bestCorrect = correct;
					bestTotal = total;
					bestWrong = wrong;
					bestRelation = yagoRelation;
				}
				tsv.write(germanAttribute + "\t" + yagoRelation + "\t" + total
						+ "\t" + correct + "\t" + wrong + "\n");
			}
			// Write the best YAGO relation
			if (bestRelation != null && !bestRelation.equals(RDFS.nothing)
					&& measure.measure(bestTotal, bestCorrect, bestWrong))
				out.write(new Fact(germanAttribute, "<_infoboxPattern>",
						bestRelation));
		}
		tsv.close();
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
		inputTheme = isEnglish() ? InfoboxTermExtractor.INFOBOXTERMS
				.inLanguage(language)
				: InfoboxTermExtractor.INFOBOXTERMSTRANSLATED
						.inLanguage(language);
	}

	/** Constructor used to test the matching with/of other sources */
	protected AttributeMatcher(Theme inputTheme, Theme referenceTheme,
			String outputSuffix) {
		super(outputSuffix);
		this.inputTheme = inputTheme;
		this.referenceTheme = referenceTheme;
	}

	/**
	 * Use this class if you want to match any attributes (not necessarily
	 * multilingual ones).
	 */
	public static class CustomAttributeMatcher extends Extractor {

		/** We have a small matcher that does the work */
		protected AttributeMatcher amatch;

		@Override
		public Set<Theme> input() {
			return amatch.input();
		}

		@Override
		public Set<Theme> output() {
			return amatch.output();
		}

		@Override
		public Set<Theme> inputCached() {
			return amatch.inputCached();
		}

		public CustomAttributeMatcher(Theme inputTheme, Theme referenceTheme,
				String outputSuffix) {
			super();
			amatch = new AttributeMatcher(inputTheme, referenceTheme,
					outputSuffix);
		}

		@Override
		public void extract() throws Exception {
			amatch.extract();
		}
	}

	public static void main(String[] args) throws Exception {

		new AttributeMatcher("de").extract(new File(args[0]),
				"mapping infobox attributes in different languages");
	}

}
