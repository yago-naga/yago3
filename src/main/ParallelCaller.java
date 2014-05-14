package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
import extractors.MultilingualExtractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.FollowUpExtractor;
import fromThemes.AttributeMatcher;

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

	/** Themes we produced and that were not used */
	protected static Set<Theme> themesWeProducedAndNobodyConsumed = new TreeSet<>();

	/** Caches we killed */
	protected static Set<Theme> cachesWeKilled = new TreeSet<>();

	/** Number of threads we want */
	protected static int numThreads = 16;

	/** Starting time */
	protected static long startTime;

	/** TRUE if we are just simulating a run */
	protected static boolean simulate = true;

	/** Finds the themes that nobody produced */
	protected static Set<Theme> culprits() {
		Set<Theme> themesWeNeed = new HashSet<>();
		for (Extractor e : extractorsToDo) {
			for (Theme t : e.input()) {
				if (themesWeHave.contains(t))
					continue;
				themesWeNeed.add(t);
			}
		}
		for (Extractor e : extractorsToDo) {
			themesWeNeed.removeAll(e.output());
		}
		return (themesWeNeed);
	}

	/** Calls next extractor */
	public static synchronized void callNext(Extractor finished, boolean success) {
		D.p(NumberFormatter.ISOtime());
		if (finished != null) {
			extractorsRunning.remove(finished);
			if (success) {
				D.p("Finished", finished);
				themesWeHave.addAll(finished.output());
				themesWeProducedAndNobodyConsumed.addAll(finished.output());
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
			if (!requiredCaches.contains(theme)
					&& !cachesWeKilled.contains(theme)) {
				D.p("Killing cache", theme);
				theme.killCache();
				cachesWeKilled.add(theme);
			}
		}

		// Start other extractors that can run now
		for (int i = 0; i < extractorsToDo.size(); i++) {
			if (extractorsRunning.size() >= numThreads)
				break;
			Extractor ex = extractorsToDo.get(i);
			if (ex.input().isEmpty() || themesWeHave.containsAll(ex.input())) {
				themesWeProducedAndNobodyConsumed.removeAll(ex.input());
				if (!ex.output().isEmpty()
						&& themesWeHave.containsAll(ex.output())) {
					D.p("Skipping", ex);
				} else {
					D.p("Starting", ex);
					StringBuilder caches = new StringBuilder();
					for (Theme t : ex.inputCached())
						caches.append(t)
								.append(t.isCached() ? " (cached)" : "")
								.append(", ");
					if (caches.length() != 0)
						D.p("Required caches: ", caches);
					if (AttributeMatcher.containsAny(ex.inputCached(),
							cachesWeKilled)) {
						Announce.warning("Resurrecting cache");
					}
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
		// D.p("Themes:", themesWeHave);
		// D.p("Extractors queuing:", extractorsToDo);
		D.p("Extractors running:", extractorsRunning);
		D.p("Extractors failed:", extractorsFailed);
		if (!extractorsRunning.isEmpty())
			return;

		// In case we finished print summary
		long now = System.currentTimeMillis();
		D.p("Finished at", NumberFormatter.ISOtime());
		D.p("Time needed:", NumberFormatter.formatMS(now - startTime));
		if (!extractorsToDo.isEmpty()) {
			for (Extractor e : extractorsToDo) {
				Set<Theme> weneed = new HashSet<>(e.input());
				weneed.removeAll(themesWeHave);
				Announce.warning("Could not call", e.name(),
						"because of missing", weneed);
			}
			Announce.warning("Nobody produced or will produce", culprits());
		}
		for (Theme t : themesWeProducedAndNobodyConsumed) {
			if (!t.isFinal())
				Announce.warning("Nobody consumed", t);
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
			boolean success = false;
			try {
				if (!simulate)
					ex.extract(outputFolder, ParallelCaller.header
							+ NumberFormatter.ISOtime() + ".\n\n");
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
				e.printStackTrace(System.out);
			}
			callNext(ex, success);
		}
	}

	/** Adds all follow up extractors to the list */
	public static void addFollowUps(List<Extractor> extractors) {
		for (int i = 0; i < extractors.size(); i++) {
			Set<FollowUpExtractor> followUps = extractors.get(i).followUp();
			extractors.addAll(followUps);
		}
	}

	/** Create the list of Wikipedias */
	protected static void createWikipediaList(List<String> languages,
			List<String> wikis) {
		if (wikis == null || wikis.isEmpty() || languages == null
				|| languages.isEmpty() || wikis.size() > languages.size()
				|| !languages.get(0).equals("en")) {
			Announce.help(
					"Error: No wikipedias given. The ini file should contain:",
					"   wikipedias = wiki_en.xml, wiki_de.xml, ...",
					"   languages = en, de, ...",
					"with a 1:1 correspondence between languages and Wikipedias.",
					"The languages have to start with English, followed by the 'most English' other languages.",
					"Found: " + languages + ", " + wikis);
		}
		MultilingualExtractor.wikipediaLanguages = new ArrayList<>();
		for (String l : languages) {
			if (!l.matches("[a-z]{2,3}"))
				Announce.error(
						"Languages have to be 2 or 3-digit language codes, not",
						l);
			MultilingualExtractor.wikipediaLanguages.add(l);
		}
		wikipedias = new HashMap<String, File>();
		for (int i = 0; i < languages.size(); i++) {
			File wiki = new File(wikis.get(i));
			if (!wiki.exists())
				Announce.error("Wikipedia not found:", wiki);
			wikipedias.put(languages.get(i), wiki);
		}
	}

	/** Run */
	public static void main(String[] args) throws Exception {
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		D.p("Initializing from", initFile);
		Parameters.init(initFile);
		simulate = Parameters.getBoolean("simulate", false);
		if (simulate)
			D.p("Simulating a YAGO run");
		else
			D.p("Running YAGO extractors in parallel");
		numThreads = Parameters.getInt("numThreads", numThreads);
		createWikipediaList(Parameters.getList("languages"),
				Parameters.getList("wikipedias"));
		boolean reuse = Parameters.getBoolean("reuse", false);
		outputFolder = simulate ? Parameters.getFile("yagoSimulationFolder")
				: Parameters.getFile("yagoFolder");
		extractorsToDo = extractors(Parameters.getList("extractors"));
		addFollowUps(extractorsToDo);

		extractorsRunning.clear();
		extractorsFailed.clear();
		themesWeHave.clear();
		if (reuse) {
			D.p("Reusing existing themes");
			for (Theme t : Theme.all()) {
				File f = t.findFileInFolder(outputFolder);
				if (f == null || f.length() < 100)
					continue;
				t.assignToFolder(outputFolder);
				themesWeHave.add(t);
			}
		}
		startTime = System.currentTimeMillis();
		if (!simulate)
			Announce.setLevel(Announce.Level.WARNING);
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
		if (extractorNames == null || extractorNames.isEmpty()) {
			Announce.help(
					"Error: No extractors given. The ini file should contain",
					"   extractors = package.extractorClass[(fileName)], ...",
					"The filename arguments are required only for DataExtractors",
					"if you want them to run on other files than the default files.");

			/*
			 * // In the future: Collect extractor names automatically File
			 * source = new File("./src"); if (!source.exists() ||
			 * !source.isDirectory()) { Announce.help(
			 * "Error: No extractors given. The ini file should contain",
			 * "   extractors = package.extractorClass[(fileName)], ...",
			 * "The filename arguments are required only for DataExtractors",
			 * "if you want them to run on other files than the default files.",
			 * "",
			 * "Alternatively, if there is a folder './src', the extractor names"
			 * , "will be collected from there."); } extractorNames = new
			 * ArrayList<>(); List<String> exclude =
			 * Arrays.asList("deduplicators", "extractors", "followUp", "main",
			 * "utils"); for (File dir : source.listFiles()) { if
			 * (dir.isDirectory() && !exclude.contains(dir.getName())) { for
			 * (String e : dir.list()) { if (e.endsWith(".java")) {
			 * extractorNames.add(dir.getName() + "." + e.substring(0,
			 * e.length() - 5)); } } } }
			 */
		}

		List<Extractor> extractors = new ArrayList<Extractor>();
		for (String extractorName : extractorNames) {
			extractors.addAll(extractorsForCall(extractorName));
		}
		Announce.done();
		return (extractors);
	}

	/** Creates an extractor for a call of the form "extractorName(File)" */
	@SuppressWarnings("unchecked")
	public static List<Extractor> extractorsForCall(String extractorCall) {
		if (extractorCall == null || extractorCall.isEmpty())
			return (null);
		Announce.doing("Creating", extractorCall);
		Matcher m = Pattern.compile(
				"([A-Za-z0-9\\.]+)(?:\\(([A-Za-z_0-9\\-:/\\.\\\\]*)\\))?")
				.matcher(extractorCall);
		if (!m.matches()) {
			Announce.error("Cannot parse extractor call:", extractorCall);
		}
		List<Extractor> extractors = new ArrayList<Extractor>();
		Class<? extends Extractor> clss;
		try {
			clss = (Class<? extends Extractor>) Class.forName(m.group(1));
		} catch (Exception e) {
			Announce.error(e);
			return (null);
		}
		Set<Class<?>> superclasses = new HashSet<>();
		Class<?> s = clss;
		while (s != Object.class) {
			superclasses.add(s);
			s = s.getSuperclass();
		}
		if (superclasses.contains(MultilingualWikipediaExtractor.class)) {
			for (String language : wikipedias.keySet()) {
				extractors.add(MultilingualWikipediaExtractor.forName(
						(Class<MultilingualWikipediaExtractor>) clss, language,
						wikipedias.get(language)));
			}
		} else if (superclasses.contains(MultilingualExtractor.class)) {
			for (String language : wikipedias.keySet()) {
				extractors.add(MultilingualExtractor.forName(
						(Class<MultilingualExtractor>) clss, language));
			}
		} else if (superclasses.contains(EnglishWikipediaExtractor.class)) {
			extractors.add(EnglishWikipediaExtractor.forName(
					(Class<DataExtractor>) clss, wikipedias.get("en")));
		} else if (superclasses.contains(DataExtractor.class)) {
			File input = null;
			if (m.group(2) != null && !m.group(2).isEmpty())
				input = new File(m.group(2));
			extractors.add(DataExtractor.forName((Class<DataExtractor>) clss,
					input));
		} else {
			extractors.add(Extractor.forName((Class<Extractor>) clss));
		}
		Announce.done();
		return (extractors);
	}
}
