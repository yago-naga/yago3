package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.administrative.Parameters;
import javatools.parsers.NumberFormatter;
import basics.Theme;
import extractors.Extractor;

/**
 * YAGO2s -- ParallelCaller
 * 
 * Calls the extractors in parallel
 * as given in the ini-file. The format in the ini-file
 * is: extractors = extractors.HardExtractor(./mydatafolder),
 * extractors.WikipediaExtractor(myWikipediaFile), ...
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

  /** Themes we have */
  protected static Set<Theme> themesWeHave = new TreeSet<>();

  /** Number of threads we want*/
  protected static int numThreads = 4;

  /** Starting time */
  protected static long time;

  /** Calls next extractor*/
  public static synchronized void callNext(Extractor finished, boolean success) {
    if (finished != null) {
      extractorsRunning.remove(finished);
      if (success) {
        D.p("Finished", finished);
        themesWeHave.addAll(finished.output());
        extractorsToDo.addAll(finished.followUp());
      } else {
        D.p("Failed", finished);
      }
    }
    D.p("Themes:", themesWeHave);
    if (extractorsRunning.size() >= numThreads) return;
    for (int i = 0; i < extractorsToDo.size(); i++) {
      if (extractorsRunning.size() >= numThreads) break;
      Extractor ex = extractorsToDo.get(i);
      if (ex.input().isEmpty() || themesWeHave.containsAll(ex.input())) {
        D.p("Starting", ex);
        extractorsRunning.add(ex);
        new ExtractionCaller(ex).start();
        extractorsToDo.remove(ex);
        i--;
      }
    }
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
      ex = e;
    }

    public void run() {
      boolean success;
      try {
        ex.extract(outputFolder, Caller.header);
        success = true;
      } catch (Exception e) {
        e.printStackTrace();
        success = false;
      }
      callNext(ex, success);
    }
  }

  /** Run */
  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.WARNING);
    D.p("Running YAGO extractors in parallel");
    time = System.currentTimeMillis();
    D.p("Starting at", NumberFormatter.ISOtime());
    String initFile = args.length == 0 ? "yago.ini" : args[0];
    D.p("Initializing from", initFile);
    Parameters.init(initFile);
    numThreads = Parameters.getInt("numThreads", numThreads);
    outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
    extractorsToDo = Caller.extractors(Parameters.getList("extractors"));
    callNext(null, true);
  }

}
