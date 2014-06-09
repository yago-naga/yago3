package eval;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javatools.administrative.D;
import javatools.filehandlers.TSVFile;
import utils.AttributeMappingMeasure;

/**
 * This class is only for the evaluation of the Multilingual YAGO stuff
 * 
 * @author Fabian
 * 
 */
public class Evaluator {

	public static boolean exclude(String yagoRel) {
		// return(yagoRel.equals("<hasNumberOfPeople>") ||
		// yagoRel.contains("Date"));
		return (false);
		// return (yagoRel.equals(RDFS.label));
	}

	public static void main(String[] args) throws Exception {
		boolean dowinnerplot = true;
		boolean dolanguageplot = true;
		boolean printProblems = false;
		boolean weighted = true;
		List<String> languages = Arrays.asList("ar", "de", "es", "fa", "fr",
				"it", "nl", "pl", "ro");
		// List<String> languages = Arrays.asList("dbp");
		args = new String[] { "c:/fabian/Dropbox/Shared/multiYAGO/AttributeMatches/" };
		// Measures for which we want details
		Collection<AttributeMappingMeasure> littleDarlings = Arrays.asList(
				(AttributeMappingMeasure) new AttributeMappingMeasure.Wilson(
						0.04), new AttributeMappingMeasure.Confidence(0.16), new AttributeMappingMeasure.Support(110));
		
		List<AttributeMappingMeasure> measures = new ArrayList<AttributeMappingMeasure>();
		final int numSteps = 50;
		for (int i = 0; i < numSteps; i++) {
			measures.add(new AttributeMappingMeasure.Support(
					((i * 500) / numSteps)));
			measures.add(new AttributeMappingMeasure.Confidence(i * 1.0
					/ numSteps));
			measures.add(new AttributeMappingMeasure.Pca(i * 1.0 / numSteps));
			AttributeMappingMeasure.Wilson willie = new AttributeMappingMeasure.Wilson(
					i * 0.5 / numSteps);
			measures.add(willie);
			D.p(i, measures.subList(measures.size() - 4, measures.size()));
		}
		// Contains for each measure the number of languages
		// for which precision > 95%
		int[] numLanHit = new int[measures.size()];
		// Contains for each measure the recalls per language
		List<Map<String, Double>> recalls = new ArrayList<>();
		for (int i = 0; i < measures.size(); i++)
			recalls.add(new HashMap<String, Double>());
		for (String lan : languages) {
			Map<String, Map<String, Boolean>> gold = new HashMap<>();
			for (List<String> line : new TSVFile(new File(args[0],
					"goldAttributeMatches_" + lan + ".txt"))) {
				if (line.size() > 2 && !line.get(2).isEmpty()) {
					if (exclude(line.get(1)))
						continue;
					Map<String, Boolean> map = gold.get(line.get(0));
					if (map == null)
						gold.put(line.get(0), map = new HashMap<>());
					map.put(line.get(1), !line.get(2).equals("0"));
				}
			}

			int[] yells = new int[measures.size()];
			int[] correctYells = new int[measures.size()];
			double goldYells = 0;
			int[] weightedyells = new int[measures.size()];
			int[] weightedcorrectYells = new int[measures.size()];
			double weightedgoldYells = 0;
			String lastAttr = "";
			String lastTarget = "";
			int lastCorrect = 0;
			int lastWrong = 0;
			int lastTotal = 0;
			for (List<String> line : new TSVFile(new File(args[0],
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

				do {
					Map<String, Boolean> map = gold.get(lastAttr);
					if (map == null)
						break;
					Boolean val = map.get(lastTarget);
					if (val == null)
						break;
					if (val) {
						goldYells++;
						weightedgoldYells += lastTotal;
					}
					for (int i = 0; i < measures.size(); i++) {
						boolean m = measures.get(i).measure(lastTotal,
								lastCorrect, lastWrong);
						if (m && val) {
							correctYells[i]++;
							weightedcorrectYells[i] += lastTotal;
						}
						if (m) {
							if (!val && printProblems
									&& littleDarlings.contains(measures.get(i)))
								D.p("Incorrect:", measures.get(i), lastAttr,
										lastTarget, lastTotal);
							yells[i]++;
							weightedyells[i] += lastTotal;
						}
					}
				} while (false);
				lastCorrect = correct;
				lastTarget = target;
				lastWrong = wrong;
				lastTotal = total;
				lastAttr = attr;
			}
			if (dolanguageplot) {
				int numMeasures = measures.size() / numSteps;
				for (int m = 0; m < numMeasures; m++) {
					Writer w = new FileWriter(new File(args[0], "plot_" + lan
							+ "_" + measures.get(m).measureName() + ".dat"));
					for (int i = 0; i < numSteps; i++) {
						double prec = weighted ? weightedcorrectYells[m + i
								* numMeasures]
								/ (double) weightedyells[m + i * numMeasures]
								: correctYells[m + i * numMeasures]
										/ (double) yells[m + i * numMeasures];
						if (Double.isNaN(prec))
							prec = 0;
						if (Math.round(prec * 100) >= 95) {
							numLanHit[m + i * numMeasures]++;
							if (numLanHit[m + i * numMeasures] == languages
									.size())
								D.p("Winner:",
										measures.get(m + i * numMeasures));
						} else if (littleDarlings.contains(measures.get(m + i
								* numMeasures))) {
							D.p("Did not make it:",
									measures.get(m + i * numMeasures), lan,
									prec);
						}
						double rec = weighted ? weightedcorrectYells[m + i
								* numMeasures]
								/ weightedgoldYells : correctYells[m + i
								* numMeasures]
								/ goldYells;
						if (littleDarlings.contains(measures.get(m + i
								* numMeasures)))
							D.p(String.format(Locale.US,
									"%s     %s & %.0f & %.0f & %.0f\\\\",
									measures.get(m + i * numMeasures)
											.toString(), lan, prec * 100,
									rec * 100, prec * rec * 2 / (prec + rec)
											* 100));
						w.write(prec + "\t" + rec + "\n");
						recalls.get(m + i * numMeasures).put(lan, rec);
					}
					w.close();
				}
			}
		}
		if (dowinnerplot) {
			int numMeasures = measures.size() / numSteps;
			for (int m = 0; m < numMeasures; m++) {
				Writer w = new FileWriter(new File(args[0], "plot_"
						+ measures.get(m).measureName() + ".dat"));
				for (int i = 0; i < numSteps; i++) {
					w.write(i * 1.0 / (numSteps - 1) + "\t"
							+ numLanHit[m + i * numMeasures] + "\n");
					if (numLanHit[m + i * numMeasures] == languages.size()) {
						Writer w2 = new FileWriter(new File(args[0], "plot_"
								+ measures.get(m).measureName() + "_" + i
								+ ".dat"));
						for (String lan : languages) {
							w2.write(lan + "\t"
									+ recalls.get(m + i * numMeasures).get(lan)
									+ "\n");
						}
						w2.close();
					}
				}
				w.close();
			}
		}
	}
}
