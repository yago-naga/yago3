package main;

import java.io.File;

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
		for(File f : folder.listFiles()) {
			if(!f.isDirectory() && !FileSet.extension(f).equals(".ttl")) return(f);
		}
		return(null);
	}
	
	/** Runs the tester*/
	public static void main(String[] args) throws Exception {
		Announce.doing("Testing YAGO extractors");
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		Announce.doing("Initializing from", initFile);
		Parameters.init(initFile);
		Announce.done();
		int total=0;
		int failed=0;
		File yagoFolder=Parameters.getOrRequestAndAddFile("yagoFolder", "Enter the folder where the real version of YAGO lives (for the inputs):");
		File outputFolder=Parameters.getOrRequestAndAddFile("testYagoFolder", "Enter the folder where the test version of YAGO should be created:");
		File testCases=new File("testCases");
		for(File testCase : testCases.listFiles()) {
			if(!testCase.isDirectory() || testCase.getName().startsWith(".")) continue;
			total++;
			Announce.doing("Testing",testCase.getName());
			Extractor extractor = null;
			if (inputFolder(testCase) != null) {
			  extractor=Extractor.forName(testCase.getName(),dataFile(testCase));
			  extractor.extract(inputFolder(testCase), outputFolder, "Test of YAGO2s");
			} else {
			  extractor=Extractor.forName(testCase.getName(),dataFile(testCase));
			  if(extractor==null) {
			    Announce.failed();
			    failed++;
			    continue;
			  }
		     extractor.extract(yagoFolder, outputFolder, "Test of YAGO2s");
			}
			Announce.doing("Checking output");
			for(Theme theme : extractor.output()) {
				Announce.doing("Checking",theme);
				FactCollection goldStandard=new FactCollection(theme.file(testCase));
				FactCollection result=new FactCollection(theme.file(outputFolder));
				if(!result.checkEqual(goldStandard)) {
					Announce.done("--------> "+theme+" failed");
					failed++;
				} else {
					Announce.done("--------> "+theme+" OK");
				}
			}
			Announce.done();
			Announce.done();
		}
		Announce.done();
		Announce.message((total-failed),"/",total,"tests succeeded");
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
