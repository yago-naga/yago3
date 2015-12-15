package retired;

import java.io.File;
import java.io.IOException;

import utils.FactCollection;
import javatools.administrative.Announce;

public class TransitionChecker {

	public static boolean check(File gold, File... result) throws IOException {
		Announce.doing("Checking", gold.getName());
		FactCollection goldStandard = new FactCollection(gold, true);
		FactCollection results = new FactCollection();
		for (File f : result)
			results.loadFast(f);
		if (results.checkEqual(goldStandard, "new", "old")) {
			Announce.done();
			return (true);
		} else {
			Announce.failed();
			return (false);
		}
	}

	public static void main(String[] args) throws Exception {
		// check(new
		// File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.CategoryExtractor\\categoryClasses.ttl"),
		// new File("c:/fabian/data/yago2s/wikipediaClasses.ttl"));
		// check(new
		// File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.CategoryExtractor\\categoryFactsDirty.ttl"),
		// new File("c:/fabian/data/yago2s/categoryFactsToBeRedirected.ttl"));
		// check(new
		// File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.CategoryExtractor\\categoryTypes.ttl"),
		// new File("c:/fabian/data/yago2s/yagoTypes.ttl"));
		// check(new
		// File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.InfoboxExtractor\\infoboxfactsVeryDirty.ttl"),
		// new File("c:/fabian/data/yago2s/infoboxFactsToBeRedirected.ttl"));
		// check(new
		// File("c:\\Fabian\\eclipseProjects\\yago2s\\testCases\\extractors.InfoboxExtractor\\infoboxTypes.ttl"),
		// new File("c:/fabian/data/yago2s/yagoTypes.ttl"));
	}
}
