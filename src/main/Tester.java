package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import basics.FactCollection;
import basics.N4Writer;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
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
		File outputFolder=Parameters.getOrRequestAndAddFile("testYagoFolder", "the folder where the test version of YAGO should be created");
		File testCases=Parameters.getOrRequestAndAddFile("testCaseFolder", "the folder where the test cases live");
		for(File testCase : testCases.listFiles()) {
			if(!testCase.isDirectory()) continue;
			Announce.doing("Testing",testCase.getName());
			Extractor extractor=createExtractor(testCase.getName(),testCase,outputFolder);
			Announce.done();
		}
		Announce.done();
	}
}
