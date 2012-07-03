package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fromWikipedia.Extractor;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.administrative.Parameters;
import javatools.parsers.NumberFormatter;
import basics.Theme;

/**
 * YAGO2s -- ParallelCaller
 * 
 * Calls the extractors in parallel
 * as given in the ini-file. The format in the ini-file
 * is: extractors = fromOtherSources.HardExtractor(./mydatafolder),
 * fromWikipedia.WikipediaExtractor(myWikipediaFile), ...
 * 
 * Optionally, the ini-file can contain the parameter
 * reuse=true
 * ... which will re-use themes that are already there.
 * 
 * @author Fabian M. Suchanek
 *
 */
public class ParallelCaller {

  /** Where the files shall go */
  protected static File outputFolder;

  /** Extractors still to do*/
  protected static List<Extractor> extractorsToDo;

  /** Extractors running */
  protected static List<Extractor> extractorsRunning = new ArrayList<>();

  /** Extractors running */
  protected static List<Extractor> extractorsFailed = new ArrayList<>();

  /** Themes we have */
  protected static Set<Theme> themesWeHave = new TreeSet<>();

  /** Number of threads we want*/
  protected static int numThreads = 16;

  /** Starting time */
  protected static long time;

  /** Calls next extractor*/
  public static synchronized void callNext(Extractor finished, boolean success) {
    D.p(NumberFormatter.ISOtime());
    if (finished != null) {
      extractorsRunning.remove(finished);
      if (success) {
        D.p("Finished", finished);
        themesWeHave.addAll(finished.output());
        extractorsToDo.addAll(finished.followUp());
      } else {
        D.p("Failed", finished);
        extractorsFailed.add(finished);
      }
    }
    //D.p("Themes:", themesWeHave);
    if (extractorsRunning.size() >= numThreads) return;
    for (int i = 0; i < extractorsToDo.size(); i++) {
      if (extractorsRunning.size() >= numThreads) break;
      Extractor ex = extractorsToDo.get(i);
      if (ex.input().isEmpty() || themesWeHave.containsAll(ex.input())) {
        if (!ex.output().isEmpty() && themesWeHave.containsAll(ex.output())) {
          D.p("Skipping", ex);
        } else {
          D.p("Starting", ex);
          extractorsRunning.add(ex);
          new ExtractionCaller(ex).start();
        }
        extractorsToDo.remove(ex);
        i--;
      } else {
        //Set<Theme> weneed = new HashSet<>(ex.input());
        //weneed.removeAll(themesWeHave);
        //D.p("In the queue:",ex,"because of missing",weneed);
      }
    }
    D.p("Themes:", themesWeHave);
    D.p("Extractors queuing:", extractorsToDo);
    D.p("Extractors running:", extractorsRunning);
    D.p("Extractors failed:", extractorsFailed);
    if (!extractorsRunning.isEmpty()) return;
    long now = System.currentTimeMillis();
    D.p("Finished at", NumberFormatter.ISOtime());
    D.p("Time needed:", NumberFormatter.formatMS(now - time));
    if (!extractorsToDo.isEmpty()) {
      D.p("Warning: Could not call");
      for (Extractor e : extractorsToDo) {
        Set<Theme> weneed = new HashSet<>(e.input());
        weneed.removeAll(themesWeHave);
        D.p("   ", e.name(), "because of missing", weneed);
      }
    }
  }

  /** Thread that runs the caller*/
  public static class ExtractionCaller extends Thread {

    Extractor ex;

    public ExtractionCaller(Extractor e) {
      this.setName("ExtractionCaller Thread :" + e.name());
      ex = e;
    }

    public void run() {
      boolean success;
      try {
        ex.extract(outputFolder, ParallelCaller.header+NumberFormatter.ISOtime()+".\n\n");
        success = true;
      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(System.out);
        success = false;
      }
      callNext(ex, success);
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
    outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
    extractorsToDo = ParallelCaller.extractors(Parameters.getList("extractors"));
    if (reuse) {
      D.p("Reusing existing themes");
      for (File f : outputFolder.listFiles()) {
        if (!f.getName().endsWith(".ttl") || f.length() < 100) continue;
        Theme t = Theme.forFile(f);
        if (t == null) {
          D.p("  No theme found for", f.getName());
        } else {
          themesWeHave.add(t);
        }
      }
    }
    time = System.currentTimeMillis();
    callNext(null, true);
  }

  /** Header for the YAGO files */
  public static String header = "This file is part of the ontology YAGO2s\n" +
  		"It is licensed under a Creative-Commons Attribution License by the YAGO team\n" +
  		"at the Max Planck Institute for Informatics/Germany.\n" +
  		"See http://yago-knowledge.org for all details.\n" +
  		"This file was generated on ";

  /** Creates extractors as given by the names */
  public static List<Extractor> extractors(List<String> extractorNames) {
    Announce.doing("Creating extractors");
    if (extractorNames == null) {
      Announce.error("No extractors given\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
    }
    if (extractorNames.isEmpty()) {
      Announce.error("Empty extractor list\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
    }
    List<Extractor> extractors = new ArrayList<Extractor>();
    for (String extractorName : extractorNames) {
      Extractor e = ParallelCaller.extractorForCall(extractorName);
      if (e != null) extractors.add(e);
    }
    Announce.done();
    return (extractors);
  }

  /** Creates an extractor for a call of the form "extractorName(File)" */
  public static Extractor extractorForCall(String extractorName) {
    if (extractorName == null || extractorName.isEmpty()) return (null);
    Announce.doing("Creating", extractorName);
    Matcher m = Pattern.compile("([A-Za-z0-9\\.]+)\\(([A-Za-z_0-9\\-:/\\.]*)\\)").matcher(extractorName);
    if (!m.matches()) {
      Announce.error("Cannot understand extractor call:", extractorName);
      Announce.failed();
      return (null);
    }
    Extractor extractor;
    if (m.group(2) != null && !m.group(2).isEmpty()) {
      String inputDataFileName = Parameters.get(m.group(2), m.group(2));
      File inputDataFile = new File(inputDataFileName);
      if (!inputDataFile.exists()) {
        Announce.warning("Input data file not found:", inputDataFile);
        return (null);
      }
      extractor = Extractor.forName(m.group(1), inputDataFile);
    } else {
      extractor = Extractor.forName(m.group(1), null);
    }
    if (extractor == null) {
      Announce.failed();
      return (null);
    }
    Announce.done();
    return (extractor);
  }

}
