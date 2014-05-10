package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.administrative.Parameters;
import javatools.parsers.NumberFormatter;
import basics.Theme;
import extractors.DataExtractor;
import extractors.EnglishWikipediaExtractor;
import extractors.Extractor;
import extractors.MultilingualDataExtractor;
import extractors.MultilingualExtractor;
import extractors.MultilingualWikipediaExtractor;

/**
 * YAGO2s -- ParallelCaller
 * 
 * Calls the extractors in parallel as given in the ini-file. The format in the
 * ini-file is: extractors = fromOtherSources.HardExtractor(./mydatafolder),
 * fromWikipedia.WikipediaExtractor(myWikipediaFile), ...
 * 
 * Optionally, the ini-file can contain the parameter reuse=true ... which will
 * re-use themes that are already there.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class ParallelCaller {

	/** Where the files shall go */
	protected static File outputFolder;

	/** Wikipedias in different languages */
	protected static Map<String, File> wikipedias;

	/** Extractors still to do */
	protected static List<Extractor> extractorsToDo;

	/** Extractors running */
	protected static List<Extractor> extractorsRunning = new ArrayList<>();

	/** Extractors running */
	protected static List<Extractor> extractorsFailed = new ArrayList<>();

	/** Themes we have */
	protected static Set<Theme> themesWeHave = new TreeSet<>();

	/** Number of threads we want */
	protected static int numThreads = 16;

	/** Starting time */
	protected static long startTime;

	/** Calls next extractor */
	public static synchronized void callNext(Extractor finished, boolean success) {
		D.p(NumberFormatter.ISOtime());
		if (finished != null) {
			extractorsRunning.remove(finished);
			if (success) {
				D.p("Finished", finished);
				themesWeHave.addAll(finished.output());
			} else {
				D.p("Failed", finished);
				extractorsFailed.add(finished);
			}
		}
		// Kill unused caches
		Set<Theme> requiredCaches = new HashSet<>();
		for (Extractor ex : extractorsRunning) {
			requiredCaches.addAll(ex.inputCached());
		}
		for (Extractor ex : extractorsToDo) {
			requiredCaches.addAll(ex.inputCached());
		}
		for (Theme theme : themesWeHave) {
			if (requiredCaches.contains(theme))
				theme.killCache();
		}

		// Start other extractors that can run now
		for (int i = 0; i < extractorsToDo.size(); i++) {
			if (extractorsRunning.size() >= numThreads)
				break;
			Extractor ex = extractorsToDo.get(i);
			if (ex.input().isEmpty() || themesWeHave.containsAll(ex.input())) {
				if (!ex.output().isEmpty()
						&& themesWeHave.containsAll(ex.output())) {
					D.p("Skipping", ex);
				} else {
					D.p("Starting", ex);
					extractorsRunning.add(ex);
					new ExtractionCaller(ex).start();
				}
				extractorsToDo.remove(ex);
				i--;
			} else {
				// Set<Theme> weneed = new HashSet<>(ex.input());
				// weneed.removeAll(themesWeHave);
				// D.p("In the queue:",ex,"because of missing",weneed);
			}
		}

		// Print new state
		D.p("Themes:", themesWeHave);
		D.p("Extractors queuing:", extractorsToDo);
		D.p("Extractors running:", extractorsRunning);
		D.p("Extractors failed:", extractorsFailed);
		if (!extractorsRunning.isEmpty())
			return;

		// In case we finished print summary
		long now = System.currentTimeMillis();
		D.p("Finished at", NumberFormatter.ISOtime());
		D.p("Time needed:", NumberFormatter.formatMS(now - startTime));
		if (!extractorsToDo.isEmpty()) {
			D.p("Warning: Could not call");
			for (Extractor e : extractorsToDo) {
				Set<Theme> weneed = new HashSet<>(e.input());
				weneed.removeAll(themesWeHave);
				D.p("   ", e.name(), "because of missing", weneed);
			}
		}
	}

	/** Thread that runs the caller */
	public static class ExtractionCaller extends Thread {

		protected Extractor ex;

		public ExtractionCaller(Extractor e) {
			this.setName("ExtractionCaller Thread :" + e.name());
			ex = e;
		}

		public void run() {
			boolean success;
			try {
				ex.extract(outputFolder, ParallelCaller.header
						+ NumberFormatter.ISOtime() + ".\n\n");
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
				e.printStackTrace(System.out);
				success = false;
			}
			callNext(ex, success);
		}
	}

	/** Adds all follow up extractors to the list */
	public static void addFollowUps(List<Extractor> extractors) {
		for (int i = 0; i < extractors.size(); i++) {
			Set<Extractor> followUps = extractors.get(i).followUp();
			extractors.addAll(followUps);
		}
	}

	/** Create the list of Wikipedias */
	protected static void createWikipediaList(List<String> languages,
			List<String> wikis) {
		if (wikipedias == null) {
			Announce.error("No wikipedias given\nThe ini file should contain:\nwikipedias = wiki_en.xml, wiki_de.xml, ...");
		}
		if (wikipedias.isEmpty()) {
			Announce.error("Empty wikipedia list\nThe ini file should contain:\nwikipedias = wiki_en.xml, wiki_de.xml, ...");
		}
		if (languages == null || languages.isEmpty())
			languages = Arrays.asList("en");
		if (wikis.size() > languages.size()) {
			Announce.error("The wikipedia list and the language list must correspond 1:1 in the ini file");
		}
		MultilingualExtractor.wikipediaLanguages = languages;
		for (int i = 0; i < languages.size(); i++) {
			wikipedias.put(languages.get(i), wikipedias.get(i));
		}
	}

	/** Run */
	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.WARNING);
		D.p("Running YAGO extractors in parallel");
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		D.p("Initializing from", initFile);
		Parameters.init(initFile);
		numThreads = Parameters.getInt("numThreads", numThreads);
		boolean reuse = Parameters.getBoolean("reuse", false);
		outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder",
				"the folder where YAGO should be created");
		createWikipediaList(Parameters.getList("languages"),
				Parameters.getList("wikipedias"));
		extractorsToDo = ParallelCaller.extractors(Parameters
				.getList("extractors"));
		addFollowUps(extractorsToDo);

		extractorsRunning.clear();
		extractorsFailed.clear();
		themesWeHave.clear();
		if (reuse) {
			D.p("Reusing existing themes");
			for (File f : outputFolder.listFiles()) {
				if (!f.getName().endsWith(".ttl") || f.length() < 100)
					continue;
				Theme t = Theme.forFile(f);
				if (t == null) {
					D.p("  No theme found for", f.getName());
				} else {
					themesWeHave.add(t);
				}
			}
		}
		startTime = System.currentTimeMillis();
		callNext(null, true);
	}

	/** Header for the YAGO files */
	public static String header = "This file is part of the ontology YAGO2s\n"
			+ "It is licensed under a Creative-Commons Attribution License by the YAGO team\n"
			+ "at the Max Planck Institute for Informatics/Germany.\n"
			+ "See http://yago-knowledge.org for all details.\n"
			+ "This file was generated on ";

	/** Creates extractors as given by the names */
	public static List<Extractor> extractors(List<String> extractorNames) {
		Announce.doing("Creating extractors");
		if (extractorNames == null) {
			Announce.error("No extractors given\nThe ini file should contain:\nextractors = package.extractorClass(fileName), ...");
		}
		if (extractorNames.isEmpty()) {
			Announce.error("Empty extractor list\nThe ini file should contain:\nextractors = package.extractorClass(fileName), ...");
		}

		List<Extractor> extractors = new ArrayList<Extractor>();
		for (String extractorName : extractorNames) {
			extractors.addAll(ParallelCaller.extractorsForCall(extractorName));
		}
		Announce.done();
		return (extractors);
	}

	/** Creates an extractor for a call of the form "extractorName(File)" */
	@SuppressWarnings("unchecked")
	public static List<Extractor> extractorsForCall(String extractorName) {
		if (extractorName == null || extractorName.isEmpty())
			return (null);
		Announce.doing("Creating", extractorName);
		Matcher m = Pattern.compile(
				"([A-Za-z0-9\\.]+)\\(([A-Za-z_0-9\\-:/\\.]*[\\*]{0,1})\\)")
				.matcher(extractorName);
		if (!m.matches()) {
			Announce.error("Cannot parse extractor call:", extractorName);
		}
		List<Extractor> extractors = new ArrayList<Extractor>();
		Class<? extends Extractor> clss;
		try {
			clss = (Class<? extends Extractor>) Class.forName(extractorName);
		} catch (Exception e) {
			Announce.error(e);
			Announce.failed();
			return (null);
		}
		if (clss.getSuperclass() == MultilingualWikipediaExtractor.class) {
			for (String language : wikipedias.keySet()) {
				extractors.add(MultilingualDataExtractor.forName(
						(Class<MultilingualDataExtractor>) clss, language,
						wikipedias.get(language)));
			}
		} else if (clss.getSuperclass() == MultilingualDataExtractor.class) {
			File input = new File(m.group(2));
			for (String language : wikipedias.keySet()) {
				extractors.add(MultilingualDataExtractor.forName(
						(Class<MultilingualDataExtractor>) clss, language,
						input));
			}
		} else if (clss.getSuperclass() == MultilingualExtractor.class) {
			for (String language : wikipedias.keySet()) {
				extractors.add(MultilingualExtractor.forName(
						(Class<MultilingualExtractor>) clss, language));
			}
		} else if (clss.getSuperclass() == DataExtractor.class) {
			File input = new File(m.group(2));
			extractors.add(DataExtractor.forName((Class<DataExtractor>) clss,
					input));
		} else if (clss.getSuperclass() == EnglishWikipediaExtractor.class) {
			extractors.add(EnglishWikipediaExtractor.forName(
					(Class<DataExtractor>) clss, wikipedias.get("en")));
		} else {
			extractors.add(Extractor.forName((Class<Extractor>) clss));
		}
		Announce.done();
		return (extractors);
	}
}
