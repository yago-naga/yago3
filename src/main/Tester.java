package main;

import java.io.File;
import java.io.FileWriter;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.RelationChecker;

/**
 * YAGO2s - Tester
 * 
 * Runs test cases for extractors in the folder "testCases"
 * 
 * @author Fabian
 * 
 */
public class Tester {

	private static int total = 0;

	private static int failed = 0;

	/** Runs the tester */
	public static void main(String[] args) throws Exception {
		if (args.length != 2)
			Announce.help("Tester yago.ini logFile.log", "",
					"Runs all tests in the test folder");
		String initFile = args[0];
		Announce.setWriter(new FileWriter(new File(args[1])));
		Announce.doing("Testing YAGO extractors");
		Announce.doing("Initializing from", initFile);
		Parameters.init(initFile);
		Announce.done();
		File yagoFolder = Parameters
				.getOrRequestAndAddFile(
						"yagoFolder",
						"Enter the folder where the real version of YAGO lives (for the inputs if they are not checked in):");
		File outputFolder = Parameters
				.getOrRequestAndAddFile("testYagoFolder",
						"Enter the folder where the test version of YAGO should be created:");
		ParallelCaller.createWikipediaList(Parameters.getList("languages"),
				Parameters.getList("wikipedias"));
		// Run hard extractors to update patterns and relations
		new PatternHardExtractor(new File("./data"))
				.extract(yagoFolder, "test");
		new HardExtractor(new File("../basics2s/data")).extract(yagoFolder,
				"test");

		if (singleTest != null) {
			runTest(singleTest, yagoFolder, outputFolder);
		} else {
			new RelationChecker().extract(yagoFolder, "Check relations");
			File testCases = new File("testCases");
			for (File testCase : testCases.listFiles()) {
				runTest(testCase, yagoFolder, outputFolder);
			}
		}
		Announce.done();
		Announce.message((total - failed), "/", total, "tests succeeded");
		Announce.close();
	}

	/** Runs a single test */
	private static void runTest(File testCase, File yagoFolder,
			File outputFolder) throws Exception {
		if (!testCase.isDirectory() || testCase.getName().startsWith("."))
			return;
		Announce.doing("Testing", testCase.getName());
		File inputDataFile=null;
		if()
		Extractor extractor = null;
		try {
			if (dataFolder(testCase) != null) {
				extractor = Extractor.forName(testCase.getName(),
						dataFolder(testCase));
			} else {
				extractor = Extractor.forName(testCase.getName(),
						dataFile(testCase));
			}
			if (inputFolder(testCase) != null) {

				extractor.extract(inputFolder(testCase), outputFolder,
						"Test of YAGO2s");
			} else {

				extractor.extract(yagoFolder, outputFolder, "Test of YAGO2s");
			}
		} catch (Exception e) {
			Announce.message(e);
			Announce.failed();
			total++;
			failed++;
			return;
		}
		Announce.doing("Checking output");
		for (Theme theme : extractor.output()) {
			total++;
			Announce.doing("Checking", theme);
			FactCollection goldStandard = null;
			FactCollection result = null;
			try {
				goldStandard = new FactCollection(theme.file(testCase));
				result = new FactCollection(theme.file(outputFolder));
			} catch (Exception ex) {
				Announce.message(ex);
			}
			if (result == null || goldStandard == null
					|| !result.checkEqual(goldStandard)) {
				Announce.done("--------> " + theme + " failed");
				failed++;
			} else {
				Announce.done("--------> " + theme + " OK");
			}
		}
		Announce.done();
		Announce.done();
	}

	private static File inputFolder(File testCase) {
		for (File f : testCase.listFiles()) {
			if (f.isDirectory() && f.getName().equals("input")) {
				return f;
			}
		}
		return null;
	}

	private static File dataFolder(File testCase) {
		for (File f : testCase.listFiles()) {
			if (f.isDirectory() && f.getName().equals("data")) {
				return f;
			}
		}
		return null;
	}
}
