package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.filehandlers.FileSet;
import utils.FactCollection;
import utils.Theme;
import extractors.DataExtractor;
import extractors.EnglishWikipediaExtractor;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.FollowUpExtractor;
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

	/** Total number of themes that should be produced */
	private static int total = 0;

	/** Number of themes that succeeded */
	private static int succeeded = 0;

	/** Extractors that failed */
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
				goldStandard = new FactCollection(
						theme.findFileInFolder(goldFolder));
			} catch (Exception ex) {
				Announce.warning("Can't load gold theme",theme,"in",goldFolder);
			}
			try {
			result = new FactCollection(
					theme.findFileInFolder(outputFolder));
			} catch (Exception ex) {
				Announce.warning("Can't load test theme",theme,"in",outputFolder);
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
		Set<Class<?>> superclasses = new HashSet<>();
		Class<?> s = clss;
		while (s != Object.class) {
			superclasses.add(s);
			s = s.getSuperclass();
		}
		if (superclasses.contains(MultilingualWikipediaExtractor.class)) {
			for (File language : listFiles(testCase)) {
				if (language.isFile() || language.getName().length() != 2) {
					Announce.warning(
							"Folder for MultilingualWikipediaExtractor should contain subfolders for each language, and not",
							language);
				}
				File wikipedia = getWikipedia(language);
				File gold = getGold(language);
				runTest(MultilingualWikipediaExtractor.forName(
						(Class<MultilingualWikipediaExtractor>) clss,
						language.getName(), wikipedia), language, yagoFolder,
						outputFolder, gold);
			}
		} else if (superclasses.contains(MultilingualExtractor.class)) {
			for (File language : listFiles(testCase)) {
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
		} else if (superclasses.contains(EnglishWikipediaExtractor.class)) {
			File wikipedia = getWikipedia(testCase);
			File gold = getGold(testCase);
			runTest(EnglishWikipediaExtractor.forName(
					(Class<DataExtractor>) clss, wikipedia), testCase,
					yagoFolder, outputFolder, gold);
		} else if (superclasses.contains(DataExtractor.class)) {
			File dataInput = getDataInput(testCase);
			File gold = getGold(testCase);
			runTest(DataExtractor.forName((Class<DataExtractor>) clss,
					dataInput), testCase, yagoFolder, outputFolder, gold);
		} else if (superclasses.contains(FollowUpExtractor.class)) {
			File gold = getGold(testCase);
			List<File> goldFiles = listFiles(gold);			
			if (goldFiles.size()!= 1
					|| !goldFiles.get(0).getName().startsWith("checked.")) {
				Announce.error("FollowUpExtractors need exactly one gold output theme called 'checked'");
			}
			List<File> inputFiles = listFiles(new File(testCase, "input"));
			Theme in = null;
			if (inputFiles != null) {
				for (File f : inputFiles) {
					if (f.getName().startsWith("checkMe"))
						in = Theme.getOrCreate(FileSet.newExtension(f, null).getName(),
								"Facts to be checked by " + clss,null);
				}
			}
			if (in == null) {
				Announce.error("FollowUpExtractors need a folder 'input' with an input theme called 'checkMe' or 'checkMe_XY'");
			}
			Theme out=Theme.getOrCreate("checked", "Facts checked by " + clss,null);
			runTest(FollowUpExtractor.forName((Class<FollowUpExtractor>) clss,
					in, out),
					testCase, yagoFolder, outputFolder, gold);
		} else if (superclasses.contains(Extractor.class)) {
			File gold = getGold(testCase);
			runTest(Extractor.forName((Class<Extractor>) clss), testCase,
					yagoFolder, outputFolder, gold);
		} else {
			Announce.message("Test class has to be a subclass of Extractor:",
					clss);
			Announce.failed();
			return;
		}
		Announce.done();
	}

	/** Lists all files except hidden files*/
	private static List<File> listFiles(File gold) {
		List<File> result=new ArrayList<>(Arrays.asList(gold.listFiles()));
		Iterator<File> it=result.iterator();
		while(it.hasNext()) if(isHidden(it.next())) it.remove();
		return(result);
	}

	/** TRUE if this is an SVN file*/
	private static boolean isHidden(File language) {
		return language.getName().startsWith(".");
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
		List<File> files = listFiles(dataInput);
		if (files.size()!= 1) {
			Announce.error(
					"The 'wikipedia' folder for a WikipediaExtractor should contain exactly one file:",
					dataInput);
		}
		return (files.get(0));
	}

	/** Returns the file for the data input */
	private static File getDataInput(File testCase) {
		File dataInput = new File(testCase, "dataInput");
		if (!dataInput.exists() || !dataInput.isDirectory()) {
			Announce.error(
					"A test case for a DataExtractor should contain a folder 'dataInput':",
					testCase);
		}
		List<File> files = listFiles(dataInput);
		if (files.size()== 0) {
			Announce.error(
					"The 'dataInput' folder for a DataExtractor should contain one or more data input files:",
					dataInput);
		}
		if (files.size() == 1)
			return (files.get(0));
		else
			return (dataInput);
	}

	/** Points the input themes to the testCases */
	private static void setInputThemes(Extractor ex, File testCase) {
		Theme.forgetAllFiles();
		File inputThemeFolder = new File(testCase, "input");
		if (!inputThemeFolder.exists() || !inputThemeFolder.isDirectory()) {
			Announce.warning("A test case should contain a subfolder 'input':",
					testCase);
			return;
		}
		List<File> files = listFiles(inputThemeFolder);
		for (Theme t : ex.input()) {
			if (t.findFileInFolder(inputThemeFolder) == null)
				continue;
			try {
				t.assignToFolder(inputThemeFolder);
			} catch (IOException e) {
				// Should not happen
				e.printStackTrace();
			}
			files.remove(t.file());
		}
		if (!files.isEmpty()) {
			Announce.warning("Superfluous themes in input:", files);
		}
	}

	/** Runs the tester */
	public static void main(String[] args) throws Exception {
//		args = new String[] { "yagoTest.ini" };
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
			// Announce.setLevel(Announce.Level.DEBUG);
			FactCollection.maxMessages = 100;
			runTest(new File("testCases", singleTest), yagoFolder, outputFolder);
		} else {
			new RelationChecker().extract(yagoFolder, "Check relations");
			File testCases = new File("testCases");
			for (File testCase : listFiles(testCases)) {
				runTest(testCase, yagoFolder, outputFolder);
			}
		}
		Announce.done();
		Announce.message(succeeded, "/", total, "tests succeeded");
		Announce.message("Failed extractors:", failedExtractors);
		Announce.close();
	}

}
