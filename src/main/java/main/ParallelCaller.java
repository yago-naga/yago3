package main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extractors.DataExtractor;
import extractors.EnglishWikipediaExtractor;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.FollowUpExtractor;
import fromThemes.AttributeMatcher;
import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.administrative.Parameters;
import javatools.parsers.NumberFormatter;
import javatools.util.FileUtils;
import utils.Theme;

/**
 * Calls the extractors in parallel as given in the ini-file. The format in the
 * ini-file is: extractors = fromOtherSources.HardExtractor(./mydatafolder),
 * fromWikipedia.WikipediaExtractor(myWikipediaFile), ...
 *
 * Optionally, the ini-file can contain the parameter reuse=true ... which will
 * re-use themes that are already there.
 *
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek, with contributions
from Mohamed Amir Yosef and Johannes Hoffart.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
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

  /** TRUE if we run extractors which take a theme as an input, which was regenerated */
  protected static boolean rerunDependentExtractors = false;

  /** Maps from an extractor to those that need to run before it */
  protected static Map<Extractor, Set<Extractor>> extractorDependencies = new HashMap<>();

  /** Maps from a theme to the extractor which produces it */
  protected static Map<Theme, Extractor> theme2extractor = new HashMap<>();

  protected static Map<String, List<Extractor>> call2extractor = new HashMap<>();

  /** Finds the themes that nobody produced */
  protected static Set<Theme> culprits() {
    Set<Theme> themesWeNeed = new HashSet<>();
    for (Extractor e : extractorsToDo) {
      for (Theme t : e.input()) {
        if (themesWeHave.contains(t)) continue;
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
      if (!requiredCaches.contains(theme) && !cachesWeKilled.contains(theme)) {
        theme.killCache();
        cachesWeKilled.add(theme);
      }
    }

    // Start other extractors that can run now
    for (int i = 0; i < extractorsToDo.size(); i++) {
      if (extractorsRunning.size() >= numThreads) break;
      Extractor ex = extractorsToDo.get(i);
      if (ex.input().isEmpty() || themesWeHave.containsAll(ex.input())) {
        themesWeProducedAndNobodyConsumed.removeAll(ex.input());
        if (!ex.output().isEmpty() && themesWeHave.containsAll(ex.output())) {
          D.p("Skipping", ex);
        } else {
          D.p("Starting", ex);
          StringBuilder caches = new StringBuilder();
          for (Theme t : ex.inputCached())
            caches.append(t).append(t.isCached() ? " (cached)" : "").append(", ");
          if (caches.length() != 0) D.p("Required caches: ", caches);
          if (AttributeMatcher.containsAny(ex.inputCached(), cachesWeKilled)) {
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
    if (!extractorsRunning.isEmpty()) return;

    // In case we finished print summary
    long now = System.currentTimeMillis();
    D.p("Finished at", NumberFormatter.ISOtime());
    D.p("Time needed:", NumberFormatter.formatMS(now - startTime));
    if (!extractorsToDo.isEmpty()) {
      for (Extractor e : extractorsToDo) {
        Set<Theme> weneed = new HashSet<>(e.input());
        weneed.removeAll(themesWeHave);
        Announce.warning("Could not call", e.name(), "because of missing", weneed);
        ParallelCaller.printNeededExtractorsForThemes(weneed);
      }
      Announce.warning("Nobody produced or will produce", culprits());
    }
    for (Theme t : themesWeProducedAndNobodyConsumed) {
      if (!t.isFinal()) Announce.warning("Nobody consumed", t);
    }
  }

  /** Thread that runs the caller */
  public static class ExtractionCaller extends Thread {

    protected Extractor ex;

    public ExtractionCaller(Extractor e) {
      this.setName("ExtractionCaller Thread :" + e.name());
      ex = e;
    }

    @Override
    public void run() {
      boolean success = false;
      try {
        if (!simulate) ex.extract(outputFolder, ParallelCaller.header + NumberFormatter.ISOtime() + ".\n\n");
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
  public static void createWikipediaList(List<String> languages, List<String> wikis) {
    if (wikis == null || wikis.isEmpty() || languages == null || languages.isEmpty() || wikis.size() > languages.size()
        || !languages.get(0).startsWith("en")) {
      Announce.help("Error: No wikipedias given. The ini file should contain:", "   wikipedias = wiki_en.xml, wiki_de.xml, ...",
          "   languages = en, de, ...", "with a 1:1 correspondence between languages and Wikipedias.",
          "The languages have to start with English, followed by the 'most English' other languages.", "Found: " + languages + ", " + wikis);
    }
    MultilingualExtractor.wikipediaLanguages = new ArrayList<>();
    for (String l : languages) {
      if (!l.matches("[a-z]{2,3}")) Announce.error("Languages have to be 2 or 3-digit language codes, not", l);
      MultilingualExtractor.wikipediaLanguages.add(l);
    }
    wikipedias = new HashMap<>();
    for (int i = 0; i < languages.size(); i++) {
      File wiki = new File(wikis.get(i));
      if (!wiki.exists()) Announce.error("Wikipedia not found:", wiki);
      wikipedias.put(languages.get(i), wiki);
    }
  }

  public static void fillTheme2Extractor() {
    Enumeration<URL> roots = null;
    try {
      roots = ParallelCaller.class.getClassLoader().getResources("");
    } catch (IOException e) {
      e.printStackTrace();
    }
    while (roots.hasMoreElements()) {
      URL rootURL = roots.nextElement();
      File root = new File(rootURL.getPath());
      for (File file : FileUtils.getAllFiles(root)) {
        if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
          String fullClassName = file.getAbsolutePath().replace(root.getAbsolutePath() + "/", "").replace("/", ".");
          fullClassName = fullClassName.substring(0, fullClassName.lastIndexOf("."));
          List<Extractor> es = null;
          try {
            es = getExtractorForClassIfPossible(fullClassName);
          } catch (Exception e) {
          }
          if (es != null) {
            for (Extractor e : es) {
              for (Theme t : e.output()) {
                theme2extractor.put(t, e);
              }
              for (FollowUpExtractor fe : e.followUp()) {
                for (Theme t : fe.output()) {
                  theme2extractor.put(t, e);
                }
              }
            }
          }
        }
      }
    }
  }

  /** Returns a topological sort. If map contains a -> [b, c], b -> [c, d], it may return c, d, b, a */
  public static <T> List<T> topologicalSort(Map<T, Set<T>> itemToAncestors) {
    Map<T, HashSet<T>> map = new HashMap<>();
    for (T e : itemToAncestors.keySet()) {
      map.put(e, new HashSet<>(itemToAncestors.get(e)));
      for (T a : itemToAncestors.get(e)) {
        map.computeIfAbsent(a, k -> new HashSet<>());
      }
    }

    List<T> result = new ArrayList<>();
    Set<T> done = new HashSet<>();
    while (map.size() > 0) {
      boolean changed = false;
      for (T e : new ArrayList<>(map.keySet())) {
        changed |= map.get(e).removeIf(d -> done.contains(d));
        if (map.get(e).size() == 0) {
          done.add(e);
          map.remove(e);
          result.add(e);
          changed = true;
        }
      }
      if (!changed) break;
    }

    if (map.size() > 0) {
      Announce.warning("Circular dependencies within following extractors:");
      Announce.warning(result);

      // print path
      List<T> path = new ArrayList<>();
      Map<T, Integer> toDistance = new HashMap<>();
      Map<T, T> prev = new HashMap<>();
      T start = map.keySet().iterator().next(), act = start;
      int actDist = 0;
      while (!done.contains(act)) {
        done.add(act);
        path.add(act);
        toDistance.remove(act);
        for (T neighbor : map.get(act)) {
          if (!done.contains(neighbor) && map.containsKey(neighbor)) {
            if (toDistance.containsKey(neighbor)) {
              T fAct = act;
              toDistance.merge(neighbor, actDist + 1, (a, b) -> {
                prev.put(neighbor, fAct);
                return Math.min(a, b);
              });
            } else {
              toDistance.put(neighbor, actDist + 1);
              prev.put(neighbor, act);
            }
          }
        }
        actDist = Integer.MAX_VALUE;
        for (Entry<T, Integer> e : toDistance.entrySet()) {
          if (actDist > e.getValue()) {
            actDist = e.getValue();
            act = e.getKey();
          }
        }
      }

      do {
        Announce.warning("path: ", act, " --> ", map.get(act));
      } while ((act = prev.get(act)) != null);
      Announce.warning("path: ", start, " --> ", map.get(start));

    }
    return result;
  }

  /** Get extractors that produce themes. */
  public static void printNeededExtractorsForThemes(Set<Theme> themes) {
    Set<Theme> stillMissing = new HashSet<>(themes);
    Set<Theme> unresolvable = new HashSet<>();
    Set<Extractor> extractorsToRun = new HashSet<>();

    // This produces a large map in the log file:
    // System.out.println(theme2extractor);

    while (!stillMissing.isEmpty()) {
      Theme t = stillMissing.iterator().next();
      stillMissing.remove(t);
      Extractor toRun = theme2extractor.get(t);
      if (toRun == null) {
        unresolvable.add(t);
      } else if (!extractorsToRun.contains(toRun)) {
        stillMissing.addAll(toRun.input());
        extractorsToRun.add(toRun);
      }
    }
    Announce.warning("Try adding the following extractors to resolve missing inputs:");
    Set<String> extractorsToRunSet = new HashSet<>();
    for (Extractor e : extractorsToRun) {
      extractorsToRunSet.add(e.getClass().getCanonicalName());
    }
    List<String> sortedExtractorsToRun = new ArrayList<>(extractorsToRunSet);
    Collections.sort(sortedExtractorsToRun);
    for (String e : sortedExtractorsToRun) {
      System.out.println(e + ",");
    }
    Announce.warning("Themes that are still missing: " + unresolvable);
  }

  private static List<Extractor> getExtractorForClassIfPossible(String fullClassName) {
    List<Extractor> es = null;
    try {
      @SuppressWarnings("rawtypes")
      Class cls = Class.forName(fullClassName);
      if (Extractor.class.isAssignableFrom(cls) && !FollowUpExtractor.class.isAssignableFrom(cls) && !Modifier.isAbstract(cls.getModifiers())) {
        return extractorsForCall(fullClassName);
      }
    } catch (ClassNotFoundException cnfe) {
    } catch (SecurityException e1) {
    }
    return es;
  }

  /** Run */
  public static void main(String[] args) throws Exception {
    String initFile = args.length == 0 ? "yago.ini" : args[0];
    D.p("Initializing from", initFile);
    Parameters.init(initFile);
    simulate = Parameters.getBoolean("simulate", false);
    rerunDependentExtractors = Parameters.getBoolean("rerunDependent", false);
    List<Pattern> rerunDependentOn = new ArrayList<>();
    List<String> rerunDependentOnRegex = Parameters.getList("rerunDependentOn");
    if (rerunDependentOnRegex != null) {
      for (String regex : rerunDependentOnRegex) {
        rerunDependentOn.add(Pattern.compile(regex.trim()));
      }
    }
    if (simulate) D.p("Simulating a YAGO run");
    else D.p("Running YAGO extractors in parallel");
    numThreads = Parameters.getInt("numThreads", numThreads);
    createWikipediaList(Parameters.getList("languages"), Parameters.getList("wikipedias"));
    boolean reuse = Parameters.getBoolean("reuse", false);
    outputFolder = simulate ? Parameters.getFile("yagoSimulationFolder") : Parameters.getFile("yagoFolder");
    extractorsToDo = new ArrayList<>(extractors(Parameters.getList("extractors")));

    String lockFile = outputFolder.getAbsolutePath() + "/yago.lock";
    if (!lock(lockFile)) {
      System.out.println("another instance of YAGO seems to be running. (Otherwise delete " + lockFile + ")");
      System.exit(-1);
    }

    addFollowUps(extractorsToDo);
    fillTheme2Extractor();
    // Make sure all themes are generated.
    // This is to make sure that Theme.all() works,
    // which is important for the 'reuse'-flag further down to work.
    for (Extractor e : extractorsToDo) {
      extractorDependencies.computeIfAbsent(e, k -> new HashSet<>());
      for (Theme t : e.input()) {
        Extractor eForTheme = theme2extractor.get(t);
        extractorDependencies.get(e).add(eForTheme);
      }
      e.output();
    }
    extractorsToDo = topologicalSort(extractorDependencies);

    extractorsRunning.clear();
    extractorsFailed.clear();
    themesWeHave.clear();
    if (reuse) {
      // check which themes exist, and which we could reuse
      Set<Theme> available = new HashSet<>(), reusable = new HashSet<>();
      for (Theme t : Theme.all()) {
        File f = t.findFileInFolder(outputFolder);
        if (f == null || f.length() < 200) {
          continue;
        }
        available.add(t);
        reusable.add(t);
      }
      if (rerunDependentExtractors) {
        for (Extractor e : extractorsToDo) {
          if (!reusable.containsAll(e.input())) {
            reusable.removeAll(e.output());
          } else pattern_loop: for (Pattern p : rerunDependentOn) {
            for (Theme t : e.input()) {
              Matcher m = p.matcher(t.name);
              if (m.matches()) {
                reusable.removeAll(e.output());
                break pattern_loop;
              }
            }
          }
        }
      }

      D.p("Reusing existing themes");
      for (Theme t : Theme.all()) {
        if (!available.contains(t)) {
          D.p(" Not found:", t);
        } else {
          if (reusable.contains(t)) {
            t.assignToFolder(outputFolder);
            D.p(" Reusing:", t);
            themesWeHave.add(t);
          } else {
            D.p(" Overwriting:", t);
          }
        }
      }
    }
    startTime = System.currentTimeMillis();
    if (!simulate) Announce.setLevel(Announce.Level.WARNING);
    callNext(null, true);
  }

  private static boolean lock(String lockFile) {
    try {
      final File file = new File(lockFile);
      if (file.createNewFile()) {
        file.deleteOnExit();
        return true;
      }
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  /** Header for the YAGO files */
  public static String header = "This file is part of the ontology YAGO3.\n"
      + "It is licensed under a Creative-Commons Attribution License by the YAGO team\n" + "at the Max Planck Institute for Informatics/Germany.\n"
      + "See http://yago-knowledge.org for all details.\n" + "This file was generated on ";

  /** Creates extractors as given by the names */
  public static List<Extractor> extractors(List<String> extractorNames) {
    Announce.doing("Creating extractors");
    if (extractorNames == null || extractorNames.isEmpty()) {
      Announce.help("Error: No extractors given. The ini file should contain", "   extractors = package.extractorClass[(fileName)], ...",
          "The filename arguments are required only for DataExtractors", "if you want them to run on other files than the default files.");

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

    List<Extractor> extractors = new ArrayList<>();
    for (String extractorName : extractorNames) {
      if (extractorName.trim().startsWith("#")) continue;
      extractors.addAll(extractorsForCall(extractorName));
    }
    Announce.done();
    return (extractors);
  }

  /** Creates an extractor for a call of the form "extractorName(File)" */
  @SuppressWarnings("unchecked")
  public static List<Extractor> extractorsForCall(String extractorCall) {
    return call2extractor.computeIfAbsent(extractorCall, k -> {
      if (extractorCall == null || extractorCall.isEmpty()) return (null);
      Announce.doing("Creating", extractorCall);
      Matcher m = Pattern.compile("([A-Za-z0-9\\.]+)(?:\\(([A-Za-z_0-9\\-:/\\.\\\\]*)\\))?").matcher(extractorCall);
      if (!m.matches()) {
        Announce.error("Cannot parse extractor call:", extractorCall);
      }
      List<Extractor> extractors = new ArrayList<>();
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
          extractors.add(MultilingualWikipediaExtractor.forName((Class<MultilingualWikipediaExtractor>) clss, language, wikipedias.get(language)));
        }
      } else if (superclasses.contains(MultilingualExtractor.class)) {
        for (String language : wikipedias.keySet()) {
          extractors.add(MultilingualExtractor.forName((Class<MultilingualExtractor>) clss, language));
        }
      } else if (superclasses.contains(EnglishWikipediaExtractor.class)) {
        extractors.add(EnglishWikipediaExtractor.forName((Class<DataExtractor>) clss, wikipedias.get("en")));
      } else if (superclasses.contains(DataExtractor.class)) {
        File input = null;
        if (m.group(2) != null && !m.group(2).isEmpty()) input = new File(m.group(2));
        extractors.add(DataExtractor.forName((Class<DataExtractor>) clss, input));
      } else {
        extractors.add(Extractor.forName((Class<Extractor>) clss));
      }
      Announce.done();
      return (extractors);
    });
  }
}
