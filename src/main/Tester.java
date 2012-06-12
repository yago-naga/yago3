package main;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.filehandlers.FileSet;
import basics.FactCollection;
import basics.Theme;
import extractors.Extractor;

/**
 * YAGO2s - Tester
 * 
 * Runs test cases for extractors in the folder "testCases"
 * 
 * @author Fabian
 *
 */
public class Tester {

  /** Finds the data in file*/
  public static File dataFile(File folder) {
    for (File f : folder.listFiles()) {
      if (!f.isDirectory() && !FileSet.extension(f).equals(".ttl")) return (f);
    }
    return (null);
  }

  private static int total = 0;

  private static int failed = 0;

  /** Runs the tester*/
  public static void main(String[] args) throws Exception {
    //Writer w=new FileWriter(new File("c:/fabian/temp/.t.txt"));
    //Announce.setWriter(w);
    Announce.doing("Testing YAGO extractors");
    String initFile = args.length == 0 ? "yago.ini" : args[0];
    Announce.doing("Initializing from", initFile);
    Parameters.init(initFile);
    Announce.done();
    //    int total = 0;
    //    int failed = 0;
    File yagoFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "Enter the folder where the real version of YAGO lives (for the inputs):");
    File outputFolder = Parameters.getOrRequestAndAddFile("testYagoFolder", "Enter the folder where the test version of YAGO should be created:");
    File singleTest = Parameters.getFile("singleTest", null);
    if (singleTest != null) {
      runTest(singleTest, yagoFolder, outputFolder);
    } else {
      File testCases = new File("testCases");
      for (File testCase : testCases.listFiles()) {
        runTest(testCase, yagoFolder, outputFolder);
      }
    }
    Announce.done();
    Announce.message((total - failed), "/", total, "tests succeeded");
    //w.close();
  }

  /** Runs a single test*/
  private static void runTest(File testCase, File yagoFolder, File outputFolder) throws Exception {
    if (!testCase.isDirectory() || testCase.getName().startsWith(".")) return;
    Announce.doing("Testing", testCase.getName());
    Extractor extractor = null;
    try {
      if (inputFolder(testCase) != null) {
        extractor = Extractor.forName(testCase.getName(), dataFile(testCase));
        extractor.extract(inputFolder(testCase), outputFolder, "Test of YAGO2s");
      } else {
        extractor = Extractor.forName(testCase.getName(), dataFile(testCase));
        extractor.extract(yagoFolder, outputFolder, "Test of YAGO2s");
      }
    } catch (Exception e) {
      Announce.message(e);
      Announce.failed();
      total++;
      failed++;
      return;
    }
    Announce.doing("Checking output");
    for (Theme theme : extractor.output()) {
      total++;
      Announce.doing("Checking", theme);
      FactCollection goldStandard = null;
      FactCollection result =null;
      try {
        goldStandard=new FactCollection(theme.file(testCase));
        result= new FactCollection(theme.file(outputFolder));
      } catch(Exception ex) {
        Announce.message(ex);
      }      
      if (result==null || goldStandard==null || !result.checkEqual(goldStandard)) {
        Announce.done("--------> " + theme + " failed");
        failed++;
      } else {
        Announce.done("--------> " + theme + " OK");
      }
    }
    Announce.done();
    Announce.done();
  }

  private static File inputFolder(File testCase) {
    for (File f : testCase.listFiles()) {
      if (f.isDirectory() && f.getName().equals("input")) {
        return f;
      }
    }
    return null;
  }
}
