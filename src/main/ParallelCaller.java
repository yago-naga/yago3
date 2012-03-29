package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
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
  protected static final int numThreads = 4;

  /** Starting time */
  protected static long time;

  /** Log file*/
  protected static Writer log = null;

  /** Calls next extractor*/
  public static void callNext(Extractor finished, boolean success) {
    synchronized (extractorsRunning) {
      if (finished != null) {
        extractorsRunning.remove(finished);
        if (success) {
          Announce.message("Finished", finished);
          themesWeHave.addAll(finished.output());
        } else {
          Announce.message("Failed", finished);
        }
      }
      if (extractorsRunning.size() >= numThreads) return;
      for (int i = 0; i < extractorsToDo.size(); i++) {
        if (extractorsRunning.size() >= numThreads) break;
        Extractor ex = extractorsToDo.get(i);
        if (ex.input().isEmpty() || themesWeHave.containsAll(ex.output())) {
          extractorsRunning.add(ex);
          extractorsToDo.remove(ex);
          i--;
        }
      }
      if (extractorsRunning.isEmpty()) {
        Announce.message("Can't run any more extractor");
        long now = System.currentTimeMillis();
        Announce.message("Finished at", NumberFormatter.ISOtime());
        Announce.message("Time needed:", NumberFormatter.formatMS(now - time));
        Announce.done();
        if (!extractorsToDo.isEmpty()) {
          Announce.doing("Warning: Could not call");
          for (Extractor e : extractorsToDo) {
            Set<Theme> weneed = new HashSet<>(e.input());
            weneed.removeAll(themesWeHave);
            Announce.message(e.name(), "because of missing", weneed);
          }
          Announce.done();
          if (log != null) try {
            log.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
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
        e.printStackTrace(new PrintWriter(Announce.getWriter()));
        success = false;
      }
      callNext(ex, success);
    }
  }

  /** Run */
  public static void main(String[] args) throws Exception {
    if (Arrays.asList(args).contains("log")) {
      File logFile = new File("yago_" + NumberFormatter.timeStamp() + ".log");
      Announce.message("Output written to", logFile);
      log = new FileWriter(logFile);
      Announce.setWriter(log);
    }
    Announce.doing("Running YAGO extractors in parallel");
    time = System.currentTimeMillis();
    Announce.message("Starting at", NumberFormatter.ISOtime());
    String initFile = args.length == 0 ? "yago.ini" : args[0];
    Announce.doing("Initializing from", initFile);
    Parameters.init(initFile);
    Announce.done();
    outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
    extractorsToDo = Caller.extractors(Parameters.getList("extractors"));
    callNext(null, true);
  }

}
