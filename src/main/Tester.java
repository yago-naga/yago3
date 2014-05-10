package main;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import basics.FactCollection;
import basics.Theme;
import extractors.DataExtractor;
import extractors.Extractor;
import extractors.MultilingualDataExtractor;
import extractors.MultilingualExtractor;
import extractors.MultilingualWikipediaExtractor;
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

	private static int succeeded = 0;

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
		File singleTest = Parameters.getFile("singleTest");

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
		Announce.message(succeeded, "/", total, "tests succeeded");
		Announce.close();
	}

	/** Runs a single test */
	private static void runTest(Extractor extractor, File yagoFolder,
			File inputFolder, File outputFolder, File goldFolder)
			throws Exception {
		setInputThemes(extractor, inputFolder);
		Announce.doing("Running extractor");
		total += extractor.output().size();
		try {
			extractor.extract(yagoFolder, outputFolder, "Test");
		} catch (Exception e) {
			Announce.warning(e);
			Announce.failed();
			return;
		}
		Announce.doing("Checking output");
		for (Theme theme : extractor.output()) {
			Announce.doing("Checking", theme);
			FactCollection goldStandard = null;
			FactCollection result = null;
			try {
				goldStandard = new FactCollection(theme.file(goldFolder));
				result = new FactCollection(theme.file(outputFolder));
			} catch (Exception ex) {
				Announce.message(ex);
			}
			if (result == null || goldStandard == null
					|| !result.checkEqual(goldStandard)) {
				Announce.done("--------> " + theme + " failed");
			} else {
				Announce.done("--------> " + theme + " OK");
				succeeded++;
			}
		}
		Announce.done();
	}

	/** Runs a single test */
	@SuppressWarnings("unchecked")
	private static void runTest(File testCase, File yagoFolder,
			File outputFolder) throws Exception {
		if (!testCase.isDirectory() || testCase.getName().startsWith("."))
			return;
		String extractorName = testCase.getName();
		Announce.doing("Testing", extractorName);
		Class<? extends Extractor> clss = null;
		try {
			clss = (Class<? extends Extractor>) Class.forName(extractorName);
		} catch (Exception e) {
			Announce.warning(e);
			Announce.failed();
			return;
		}
		if (clss.getSuperclass() == MultilingualWikipediaExtractor.class) {
			for (File language : testCase.listFiles()) {
				if (language.isFile() || language.getName().length() != 2) {
					Announce.warning(
							"Folder for MultilingualWikipediaExtractor should contain subfolders for each language, and not",
							language);
				}
				File wikipedia = getWikipedia(language);
				File gold = getGold(language);
				runTest(MultilingualDataExtractor.forName(
						(Class<MultilingualDataExtractor>) clss,
						language.getName(), wikipedia), language, yagoFolder,
						outputFolder, gold);
			}
		} else if (clss.getSuperclass() == MultilingualDataExtractor.class) {
			for (File language : testCase.listFiles()) {
				if (language.isFile() || language.getName().length() != 2) {
					Announce.warning(
							"Folder for MultilingualDataExtractor should contain subfolders for each language, and not",
							language);
				}
				File dataInput = getDataInput(language);
				File gold = getGold(language);
				runTest(MultilingualDataExtractor.forName(
						(Class<MultilingualDataExtractor>) clss,
						language.getName(), dataInput), language, yagoFolder,
						outputFolder, gold);
			}
		} else if (clss.getSuperclass() == MultilingualExtractor.class) {
			for (File language : testCase.listFiles()) {
				if (language.isFile() || language.getName().length() != 2) {
					Announce.warning(
							"Folder for MultilingualDataExtractor should contain subfolders for each language, and not",
							language);
				}
				File gold = getGold(language);
				runTest(MultilingualExtractor
						.forName((Class<MultilingualExtractor>) clss,
								language.getName()),
						language, yagoFolder, outputFolder, gold);
			}
		} else if (clss.getSuperclass() == DataExtractor.class) {
			File dataInput = getDataInput(testCase);
			File gold = getGold(testCase);
			runTest(DataExtractor.forName((Class<DataExtractor>) clss,
					dataInput), testCase, yagoFolder, outputFolder, gold);
		} else if (clss.getSuperclass() == Object.class) {
			File gold = getGold(testCase);
			runTest(Extractor.forName((Class<Extractor>) clss), testCase,
					yagoFolder, outputFolder, gold);
		} else {
			Announce.error("Unknown extractor class:", clss);
		}

		Announce.done();
	}

	/** Returns the gold folder */
	private static File getGold(File testCase) {
		File gold = new File(testCase, "goldOutput");
		if (!gold.exists() || !gold.isDirectory() || gold.list().length == 0) {
			Announce.error("A test case should contain a folder 'goldOutput' with the gold standard");
		}
		return (gold);
	}

	/** Returns the wikipedia file in this folder */
	private static File getWikipedia(File language) {
		File dataInput = new File(language, "wikipedia");
		if (!dataInput.exists() || !dataInput.isDirectory()) {
			Announce.error("A test case for a WikipediaExtractor should contain a folder 'wikipedia'");
		}
		File[] files = dataInput.listFiles();
		if (files.length != 1) {
			Announce.error("The 'wikipedia' folder for a WikipediaExtractor should contain exactly one file");
		}
		return (files[0]);
	}

	/** Returns the file for the data input */
	private static File getDataInput(File testCase) {
		File dataInput = new File(testCase, "dataInput");
		if (!dataInput.exists() || !dataInput.isDirectory()) {
			Announce.error("A test case for a DataExtractor should contain a folder 'dataInput'");
		}
		File[] files = dataInput.listFiles();
		if (files.length == 0) {
			Announce.error("The 'dataInput' folder for a DataExtractor should contain one or more data input files");
		}
		if (files.length == 1)
			return (files[0]);
		else
			return (dataInput);
	}

	/** Points the input themes to the testCases */
	private static void setInputThemes(Extractor ex, File testCase) {
		Theme.clear();
		File inputThemeFolder = new File(testCase, "input");
		if (!inputThemeFolder.exists() || !inputThemeFolder.isDirectory()) {
			Announce.warning("A test case should contain a subfolder 'input'");
			return;
		}
		List<File> files = new ArrayList<File>(Arrays.asList(inputThemeFolder
				.listFiles()));
		for (Theme t : ex.input()) {
			File themeFile = t.file(inputThemeFolder);
			if (!themeFile.exists())
				continue;
			t.setFile(themeFile);
			files.remove(themeFile);
		}
		if (!files.isEmpty()) {
			Announce.warning("Superfluous themes in input:", files);
		}
	}
}
