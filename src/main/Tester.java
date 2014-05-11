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
import extractors.EnglishWikipediaExtractor;
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

	private static List<String> failedExtractors = new ArrayList<>();

	/** Runs a single test */
	private static void runTest(Extractor extractor, File inputFolder,
			File yagoFolder, File outputFolder, File goldFolder)
			throws Exception {
		setInputThemes(extractor, inputFolder);
		total += extractor.output().size();
		try {
			extractor.extract(yagoFolder, outputFolder, "Test");
		} catch (Exception e) {
			failedExtractors.add(extractor.name());
			e.printStackTrace();
			return;
		}
		Announce.doing("Checking output");
		boolean allGood = true;
		for (Theme theme : extractor.output()) {
			Announce.doing("Checking", theme);
			FactCollection goldStandard = null;
			FactCollection result = null;
			try {
				goldStandard = new FactCollection(theme.findFile(goldFolder));
				result = new FactCollection(theme.findFile(outputFolder));
			} catch (Exception ex) {
				Announce.message(ex);
			}
			if (result == null
					|| goldStandard == null
					|| !result.checkEqual(goldStandard, "output",
							"gold standard")) {
				Announce.done("--------> " + theme + " failed");
				allGood = false;
			} else {
				Announce.done("--------> " + theme + " OK");
				succeeded++;
			}
		}
		if (!allGood)
			failedExtractors.add(extractor.name());
		Announce.done();
	}

	/** Runs a single test */
	@SuppressWarnings("unchecked")
	private static void runTest(File testCase, File yagoFolder,
			File outputFolder) throws Exception {
		testCase = testCase.getAbsoluteFile();
		if (!testCase.exists() || !testCase.isDirectory()
				|| testCase.getName().startsWith(".")) {
			Announce.warning(
					"Test cases should be folders of the form packageName.extractorName, not",
					testCase.getName());
			return;
		}
		String extractorName = testCase.getName();
		Announce.doing("Testing", extractorName);
		Class<? extends Extractor> clss = null;
		try {
			clss = (Class<? extends Extractor>) Class.forName(extractorName);
		} catch (Exception e) {
			e.printStackTrace();
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
		} else if (clss.getSuperclass() == EnglishWikipediaExtractor.class) {
			File wikipedia = getWikipedia(testCase);
			File gold = getGold(testCase);
			runTest(EnglishWikipediaExtractor.forName(
					(Class<DataExtractor>) clss, wikipedia), testCase,
					yagoFolder, outputFolder, gold);
		} else {
			File gold = getGold(testCase);
			runTest(Extractor.forName((Class<Extractor>) clss), testCase,
					yagoFolder, outputFolder, gold);
		}
		Announce.done();
	}

	/** Returns the gold folder */
	private static File getGold(File testCase) {
		File gold = new File(testCase, "goldOutput");
		if (!gold.exists() || !gold.isDirectory() || gold.list().length == 0) {
			Announce.error(
					"A test case should contain a folder 'goldOutput' with the gold standard:",
					testCase);
		}
		return (gold);
	}

	/** Returns the wikipedia file in this folder */
	private static File getWikipedia(File language) {
		File dataInput = new File(language, "wikipedia");
		if (!dataInput.exists() || !dataInput.isDirectory()) {
			Announce.error(
					"A test case for a WikipediaExtractor should contain a folder 'wikipedia':",
					language);
		}
		File[] files = dataInput.listFiles();
		if (files.length != 1) {
			Announce.error(
					"The 'wikipedia' folder for a WikipediaExtractor should contain exactly one file:",
					dataInput);
		}
		return (files[0]);
	}

	/** Returns the file for the data input */
	private static File getDataInput(File testCase) {
		File dataInput = new File(testCase, "dataInput");
		if (!dataInput.exists() || !dataInput.isDirectory()) {
			Announce.error(
					"A test case for a DataExtractor should contain a folder 'dataInput':",
					testCase);
		}
		File[] files = dataInput.listFiles();
		if (files.length == 0) {
			Announce.error(
					"The 'dataInput' folder for a DataExtractor should contain one or more data input files:",
					dataInput);
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
			Announce.warning("A test case should contain a subfolder 'input':",
					testCase);
			return;
		}
		List<File> files = new ArrayList<File>(Arrays.asList(inputThemeFolder
				.listFiles()));
		for (Theme t : ex.input()) {
			t.forgetFile();
			try {
				t.assignToFolder(inputThemeFolder);
			} catch (Exception e) {
				// file is not there, no problem, get it later from YAGO
				continue;
			}
			files.remove(t.file());
		}
		if (!files.isEmpty()) {
			Announce.warning("Superfluous themes in input:", files);
		}
	}

	/** Runs the tester */
	public static void main(String[] args) throws Exception {
		args = new String[] { "yagoTest.ini" };
		if (args.length < 1)
			Announce.help("Tester yago.ini [ logFile.log ]", "",
					"Runs all tests in the test folder");
		String initFile = args[0];
		if (args.length > 1)
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
				.getOrRequestAndAddFile("yagoTestFolder",
						"Enter the folder where the test version of YAGO should be created:");
		String singleTest = Parameters.get("singleTest", null);

		if (singleTest != null) {
			runTest(new File("testCases", singleTest), yagoFolder, outputFolder);
		} else {
			new RelationChecker().extract(yagoFolder, "Check relations");
			File testCases = new File("testCases");
			for (File testCase : testCases.listFiles()) {
				runTest(testCase, yagoFolder, outputFolder);
			}
		}
		Announce.done();
		Announce.message(succeeded, "/", total, "tests succeeded");
		Announce.message("Failed extractors:", failedExtractors);
		Announce.close();
	}

}
