package eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javatools.administrative.D;
import javatools.datatypes.IntHashMap;
import javatools.filehandlers.TSVFile;
import utils.AttributeMappingMeasure;

/**
 * This class is only for the evaluation of the Multilingual YAGO stuff
 * 
 * @author Fabian
 * 
 */
public class Evaluator {

	/** Loads a gold standard*/
	public static Map<String, Map<String, Boolean>> goldStandard(File folder,
			String lan) throws IOException {
		Map<String, Map<String, Boolean>> gold = new HashMap<>();
		for (List<String> line : new TSVFile(new File(folder,
				"goldAttributeMatches_" + lan + ".txt"))) {
			if (line.size() > 2 && !line.get(2).isEmpty()) {
				Map<String, Boolean> map = gold.get(line.get(0));
				if (map == null)
					gold.put(line.get(0), map = new HashMap<>());
				map.put(line.get(1), !line.get(2).equals("0"));
			}
		}
		return (gold);
	}

	/** Creates a list of measures, each varied in 50 steps*/
	public static List<AttributeMappingMeasure> measures(int numSteps) {
		List<AttributeMappingMeasure> measures = new ArrayList<AttributeMappingMeasure>();
		for (int i = 0; i < numSteps; i++) {
			measures.add(new AttributeMappingMeasure.Support(
					((i * 500) / numSteps)));
			measures.add(new AttributeMappingMeasure.Confidence(i * 1.0
					/ numSteps));
			measures.add(new AttributeMappingMeasure.Pca(i * 1.0 / numSteps));
			AttributeMappingMeasure.Wilson willie = new AttributeMappingMeasure.Wilson(
					i * 0.5 / numSteps);
			measures.add(willie);
		}
		return (measures);
	}

	public static class Mapping {
		String sourceAttribute;
		String targetAttribute;
		int total;
		int correct;
		int wrong;

		public Mapping(String sourceAttribute, String targetAttribute,
				int total, int correct, int wrong) {
			super();
			this.sourceAttribute = sourceAttribute;
			this.targetAttribute = targetAttribute;
			this.total = total;
			this.correct = correct;
			this.wrong = wrong;
		}

		@Override
		public String toString() {
			return sourceAttribute + "->" + targetAttribute + ": " + total
					+ ", " + correct + ", " + wrong;
		}
	}

	/** Loads an attribute mapping: source attribute to target attribute with total, correct, wrong*/
	public static List<Mapping> mapping(File folder, String lan)
			throws IOException {
		List<Mapping> mappings = new ArrayList<>();
		String lastAttr = "";
		String lastTarget = "";
		int lastCorrect = 0;
		int lastWrong = 0;
		int lastTotal = 0;
		for (List<String> line : new TSVFile(new File(folder,
				"_attributeMatches_" + lan + ".tsv"))) {
			String attr = line.get(0);
			String target = line.get(1);
			int total = Integer.parseInt(line.get(2));
			int correct = Integer.parseInt(line.get(3));
			int wrong = Integer.parseInt(line.get(4));
			if (attr.equals(lastAttr)) {
				if (correct > lastCorrect) {
					lastCorrect = correct;
					lastTarget = target;
					lastWrong = wrong;
					lastTotal = total;
				}
				continue;
			}
			if (!lastAttr.isEmpty())
				mappings.add(new Mapping(lastAttr, lastTarget, lastTotal,
						lastCorrect, lastWrong));
			lastAttr = attr;
			lastTarget = target;
			lastTotal = total;
			lastCorrect = correct;
			lastWrong = wrong;
		}
		mappings.add(new Mapping(lastAttr, lastTarget, lastTotal, lastCorrect,
				lastWrong));
		return (mappings);
	}

	public static void main(String[] args) throws Exception {
		// Parameters
		IntHashMap<String> languagesWithSize = new IntHashMap<String>().putAll(
				"ar", 310, "de", 1741, "es", 1115, "fa", 408, "fr", 1529, "it",
				1136, "nl", 1786, "pl", 1056, "ro", 250);
		File goldFolder = new File(
				"c:/fabian/Dropbox/Shared/multiYAGO/AttributeMatches/");
		List<AttributeMappingMeasure> measures = measures(50);
		double minPrec = 0.945;
		double minRec = 0.20;

		// Initialize
		List<String> languages = languagesWithSize.increasingKeys();
		for (String lan : languages)
			D.p(lan, languagesWithSize.get(lan));
		Map<AttributeMappingMeasure, Map<String, double[]>> measure2language2precAndRec = new HashMap<>();
		for (AttributeMappingMeasure m : measures) {
			measure2language2precAndRec.put(m, new HashMap<String, double[]>());
		}

		// Compute Precision and Recall
		for (String lan : languages) {
			Map<String, Map<String, Boolean>> gold = goldStandard(goldFolder,
					lan);
			List<Mapping> mappings = mapping(goldFolder, lan);

			IntHashMap<AttributeMappingMeasure> correctYells = new IntHashMap<>();
			IntHashMap<AttributeMappingMeasure> totalYells = new IntHashMap<>();
			int totalGoldYells = 0;
			for (Mapping mapping : mappings) {

				Map<String, Boolean> map = gold.get(mapping.sourceAttribute);
				if (map == null)
					continue;
				Boolean val = map.get(mapping.targetAttribute);
				if (val == null)
					continue;
				if (val)
					totalGoldYells += mapping.total;

				for (AttributeMappingMeasure measure : measures) {
					boolean m = measure.measure(mapping.total, mapping.correct,
							mapping.wrong);
					if (m && val)
						correctYells.add(measure, mapping.total);
					if (m)
						totalYells.add(measure, mapping.total);
				}

			}

			// Compute precision and recall
			for (AttributeMappingMeasure measure : measures) {
				double prec = correctYells.get(measure)
						/ (double) totalYells.get(measure);
				double rec = correctYells.get(measure)
						/ (double) totalGoldYells;
				measure2language2precAndRec.get(measure).put(lan,
						new double[] { prec, rec });
				if (prec > minPrec && rec > minRec)
					System.out.printf(Locale.US, "%s %s: %.2f, %.2f\n", lan,
							measure.toString(), prec, rec);
			}

			// Print reasonable ones so far
			List<AttributeMappingMeasure> reasonableMeasures = new ArrayList<>(
					measures);
			for (String l : languages) {
				Iterator<AttributeMappingMeasure> it = reasonableMeasures
						.iterator();
				while (it.hasNext()) {
					AttributeMappingMeasure m = it.next();
					if (measure2language2precAndRec.get(m).get(l)[0] < minPrec
							|| measure2language2precAndRec.get(m).get(l)[1] < minRec) {
						it.remove();
					}
				}
				if (l == lan)
					break;
			}
			System.out.println("Reasonable measures after language " + lan
					+ ":");
			if (reasonableMeasures.size() > 10) {
				System.out.println(" many");
			} else {
				for (AttributeMappingMeasure m : reasonableMeasures) {
					System.out.println(" " + m);
				}
			}
		}
	}
}
