package eval;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javatools.administrative.D;
import javatools.filehandlers.TSVFile;

/**
 * This class is only for the evaluation of the Multilingual YAGO stuff
 * 
 * @author Fabian
 * 
 */
public class Evaluator {

	public abstract static class Measure {
		public abstract boolean measure(int total, int correct, int wrong);

		public double ratio;

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + ": "
					+ String.format("%.3f", (double) ratio);
		}

		public String measureName() {
			return this.getClass().getSimpleName();
		}

		@Override
		public boolean equals(Object arg0) {
			return arg0.getClass().equals(this.getClass())
					&& Math.round(((Measure) arg0).ratio * 1000) == Math
							.round(ratio * 1000);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() ^ (int) Math.round(ratio * 1000);
		}
	}

	public static class Support extends Measure {

		public Support(int cutoff) {
			this.ratio = cutoff;
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			return correct >= ratio;
		}
	}

	public static class All extends Support {
		public All() {
			super(0);
		}
	}

	public static class Pca extends Measure {

		public Pca(double r) {
			ratio = r;
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			return correct / (double) (correct + wrong) >= ratio;
		}
	}

	public static class Confidence extends Measure {

		public Confidence(double r) {
			ratio = r;
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			return correct / (double) total >= ratio;
		}
	}

	public static class Wilson extends Measure {

		public Wilson(double r) {
			ratio = r;
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			double wilson[] = wilson(total, correct);
			return (wilson[0] - wilson[1] > ratio);
		}
	}

	/**
	 * Computes the Wilson Interval (see
	 * http://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval
	 * #Wilson_score_interval) Given the total number of events and the number
	 * of "correct" events, returns in a double-array in the first component the
	 * center of the Wilson interval and in the second component the width of
	 * the interval. alpha=95%.
	 */
	public static double[] wilson(int total, int correct) {
		double z = 1.96;
		double p = (double) correct / total;
		double center = (p + 1 / 2.0 / total * z * z)
				/ (1 + 1.0 / total * z * z);
		double d = z
				* Math.sqrt((p * (1 - p) + 1 / 4.0 / total * z * z) / total)
				/ (1 + 1.0 / total * z * z);
		return (new double[] { center, d });
	}

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
				"it", "ro");
		// List<String> languages = Arrays.asList("de");
		args = new String[] { "c:/fabian/Dropbox/Shared/multiYAGO/AttributeMatches/" };
		// Measures for which we want details
		Collection<Measure> littleDarlings = Arrays.asList(
				(Measure) new Wilson(0.030), new Wilson(0.035),
				new Wilson(0.04));
		List<Measure> measures = new ArrayList<Measure>();
		final int numSteps = 31;
		for (double i = 0; i <= 1.0; i += 1.0 / (numSteps - 1)) {
			measures.add(new Support((int) (i * 100)));
			measures.add(new Confidence(i));
			measures.add(new Pca(i));
			Wilson willie = new Wilson(i * 0.05);
			measures.add(willie);
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
							if (!val && printProblems)
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
						if (prec > 0.95)
							numLanHit[m + i * numMeasures]++;
						double rec = weighted ? weightedcorrectYells[m + i
								* numMeasures]
								/ weightedgoldYells : correctYells[m + i
								* numMeasures]
								/ goldYells;
						if (littleDarlings.contains(measures.get(m + i
								* numMeasures)))
							D.p(lan, measures.get(m + i * numMeasures), prec,
									rec);
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
