package main;

import java.io.File;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import basics.FactCollection;
import basics.Theme;
import extractors.Extractor;

/**
 * Runs test cases for extractors
 * 
 * @author Fabian
 *
 */
public class Tester {

	
	public static void main(String[] args) throws Exception {
		Announce.doing("Testing YAGO extractors");
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		Announce.doing("Initializing from", initFile);
		Parameters.init(initFile);
		Announce.done();
		int total=0;
		int failed=0;
		File outputFolder=Parameters.getOrRequestAndAddFile("testYagoFolder", "the folder where the test version of YAGO should be created");
		File testCases=Parameters.getOrRequestAndAddFile("testCaseFolder", "the folder where the test cases live");
		for(File testCase : testCases.listFiles()) {
			if(!testCase.isDirectory()) continue;
			total++;
			Announce.doing("Testing",testCase.getName());
			File datainput=new File(testCase,"datainput.txt");
			if(!datainput.exists()) datainput=null;
			Extractor extractor=Extractor.forName(testCase.getName(),datainput);
			if(extractor==null) {
				Announce.failed();
				failed++;
				continue;
			}
			extractor.extract(testCase, outputFolder, "Test of YAGO2s");
			Announce.doing("Checking output");
			for(Theme theme : extractor.output()) {
				FactCollection goldStandard=new FactCollection(theme.file(testCase));
				FactCollection result=new FactCollection(theme.file(outputFolder));
				if(!result.checkEqual(goldStandard)) {
					Announce.failed();
					Announce.done();
					failed++;
					continue;
				}
			}
			Announce.done();
			Announce.done();
		}
		Announce.done();
		Announce.message((total-failed),"/",total,"tests succeeded");
	}
}
