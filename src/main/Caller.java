package main;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fromWikipedia.Extractor;


import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.parsers.NumberFormatter;
import basics.Theme;

/**
 * Caller - YAGO2s
 * 
 * Calls the extractors, as given in the ini-file. The format in the ini-file
 * is: extractors = extractors.HardExtractor(./mydatafolder),
 * extractors.WikipediaExtractor(myWikipediaFile), ...
 * 
 * @author Fabian
 * 
 */
public class Caller {

  /** Where the files shall go */
  public static File outputFolder;

  /** Header for the YAGO files */
  public static String header = "This file is part of the ontology YAGO2s\nIt is licensed under a Creative-Commons Attribution License by the YAGO team\nat the Max Planck Institute for Informatics/Germany.\nSee http://yago-knowledge.org for all details.\n\n";

  /** Calls specific extractors in the given order */
  public static void callNow(List<Extractor> extractors) throws Exception {
    Announce.doing("Calling manually specified extractors");
    for (Extractor e : extractors) {
      e.extract(outputFolder, header);
    }
    Announce.done();
  }

  /** Calls all extractors in the right order */
  public static void call(List<Extractor> extractors) throws Exception {
    Set<Theme> themesWeHave = new TreeSet<Theme>();
    Announce.doing("Calling extractors in scheduled order");
    Announce.message("Extractors", extractors);
    for (int i = 0; i < extractors.size(); i++) {
      Extractor e = extractors.get(i);
      if (e.input().isEmpty() || themesWeHave.containsAll(e.input())) {
        Announce.startTimer();
        try {
          e.extract(outputFolder, header);
          themesWeHave.addAll(e.output());
          extractors.addAll(e.followUp());
        } catch (Exception ex) {
          Announce.warning(ex);
          ex.printStackTrace(new PrintWriter(Announce.getWriter()));
        }
        Announce.printTime();
        extractors.remove(i);
        Announce.message("----------------------------");
        Announce.message("Current themes:", themesWeHave);
        Announce.message("Current extractors:", extractors);
        i = -1; // Start again from the beginning
      }
    }
    if (!extractors.isEmpty()) {
      Announce.doing("Warning: Could not call");
      for (Extractor e : extractors) {
        Set<Theme> weneed = new HashSet<>(e.input());
        weneed.removeAll(themesWeHave);
        Announce.message(e.name(), "because of missing", weneed);
      }
      Announce.done();
    }
    Announce.done();
  }

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
      Extractor e = extractorForCall(extractorName);
      if (e != null) extractors.add(e);
    }
    Announce.done();
    return (extractors);
  }

  /** Creates an extractor for a call of the form "extractorName(File)" */
  public static Extractor extractorForCall(String extractorName) {
    if(extractorName==null || extractorName.isEmpty()) return(null);
    Announce.doing("Creating", extractorName);
    Matcher m = Pattern.compile("([A-Za-z0-9\\.]+)\\(([A-Za-z_0-9\\-:/\\.]*)\\)").matcher(extractorName);
    if (!m.matches()) {
      Announce.error("Cannot understand extractor call:", extractorName);
      Announce.failed();
      return (null);
    }
    Extractor extractor;
    if(m.group(2)!=null && !m.group(2).isEmpty()) {
      String inputDataFileName=Parameters.get(m.group(2),m.group(2));
      File inputDataFile=new File(inputDataFileName);
      if(!inputDataFile.exists()) {
        Announce.warning("Input data file not found:",inputDataFile);
        return(null);
      }
      extractor=Extractor.forName(m.group(1),inputDataFile);
    } else {
     extractor= Extractor.forName(m.group(1),null);
    }
    if (extractor == null) {
      Announce.failed();
      return (null);
    }
    Announce.done();
    return (extractor);
  }

  /** Run */
  public static void main(String[] args) throws Exception {
    Writer log = null;
    if (Arrays.asList(args).contains("log")) {
      File logFile = new File("yago_" + NumberFormatter.timeStamp() + ".log");
      Announce.message("Output written to", logFile);
      log = new FileWriter(logFile);
      Announce.setWriter(log);
    }
    try {
      Announce.doing("Running YAGO extractors");
      long time = System.currentTimeMillis();
      Announce.message("Starting at", NumberFormatter.ISOtime());
      String initFile = args.length == 0 ? "yago.ini" : args[0];
      Announce.doing("Initializing from", initFile);
      Parameters.init(initFile);
      Announce.done();
      outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
      if (Parameters.isDefined("callNow")) {
        callNow(extractors(Parameters.getList("callNow")));
      } else {
        call(extractors(Parameters.getList("extractors")));
      }
      long now = System.currentTimeMillis();
      Announce.message("Finished at", NumberFormatter.ISOtime());
      Announce.message("Time needed:", NumberFormatter.formatMS(now - time));
      Announce.done();
    } catch (Exception e) {
      Announce.failed();
      e.printStackTrace();
    }
    if (log != null) log.close();
  }
}
