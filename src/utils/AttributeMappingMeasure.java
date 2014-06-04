package utils;

/**
 * Class AttributeMappingMeasure
 * 
 * This code is part of the YAGO project at the Max Planck Institute for
 * Informatics and the Telecom ParisTech University. It is licensed under a
 * Creative Commons Attribution License by the YAGO team:
 * https://creativecommons.org/licenses/by/3.0/
 * 
 * This class defines different strategies for mapping foreign attributes to
 * YAGO relations. Every strategy takes as input the number of total facts of
 * the attribute, the number of matches and the number of clashes, and returns
 * TRUE or FALSE.
 * 
 * @author Fabian M. Suchanek
 */
public abstract class AttributeMappingMeasure {

	/** Threshold for mapping */
	public double threshold;

	public AttributeMappingMeasure(double r) {
		threshold = r;
	}

	public abstract boolean measure(int total, int correct, int wrong);

	@Override
	public String toString() {
		return String.format("%s(%.3f)", this.getClass().getSimpleName(),
				threshold);
	}

	/** Returns the name of the measure without the threshold */
	public String measureName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean equals(Object arg0) {
		return arg0.getClass().equals(this.getClass())
				&& Math.round(((AttributeMappingMeasure) arg0).threshold * 1000) == Math
						.round(threshold * 1000);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode() ^ (int) Math.round(threshold * 1000);
	}

	/** Maps if correct>threshold */
	public static class Support extends AttributeMappingMeasure {

		public Support(int cutoff) {
			super(cutoff);
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			return correct >= threshold;
		}
	}

	/** Always maps */
	public static class All extends Support {
		public All() {
			super(0);
		}
	}

	/** Maps if correct/(correct+clashes)>threshold */
	public static class Pca extends AttributeMappingMeasure {

		public Pca(double r) {
			super(r);
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			return correct / (double) (correct + wrong) >= threshold;
		}
	}

	/** Maps if correct/total>threshold */
	public static class Confidence extends AttributeMappingMeasure {

		public Confidence(double r) {
			super(r);
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			return correct / (double) total >= threshold;
		}
	}

	public static class Wilson extends AttributeMappingMeasure {

		public Wilson(double r) {
			super(r);
		}

		@Override
		public boolean measure(int total, int correct, int wrong) {
			double wilson[] = AttributeMappingMeasure.wilson(total, correct);
			return (wilson[0] - wilson[1] > threshold);
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
}