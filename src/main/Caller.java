package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import extractors.Extractor;
import extractors.Theme;

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

	/** Calls all extractors in the right order */
	public static void call(List<Extractor> extractors) throws Exception {
		Set<Theme> themesWeHave = new TreeSet<Theme>();
		Announce.doing("Calling extractors");
		Announce.message("Extractors",extractors);
		for (int i = 0; i < extractors.size(); i++) {
			Extractor e = extractors.get(i);
			if (e.input().isEmpty() || themesWeHave.containsAll(e.input())) {
				e.extract(outputFolder, header);
				themesWeHave.addAll(e.output().keySet());
				extractors.remove(i);
				Announce.message("Current themes:", themesWeHave);
				Announce.message("Current extractors:",extractors);
				i = -1; // Start again from the beginning
			}
		}
		if(!extractors.isEmpty()) Announce.warning("Could not call",extractors);
		Announce.done();
	}

	/** Creates extractors as given by the names */
	public static List<Extractor> extractors(List<String> extractorNames) {
		Announce.doing("Creating extractors");
		if (extractorNames == null) {
			Announce
					.error("No extractors given\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
		}
		if (extractorNames.isEmpty()) {
			Announce
					.error("Empty extractor list\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
		}
		List<Extractor> extractors = new ArrayList<Extractor>();
		for (String extractorName : extractorNames) {
			Announce.doing("Creating", extractorName);
			Matcher m = Pattern.compile("([A-Za-z0-9\\.]+)\\(([A-Za-z0-9:/\\.]*)\\)").matcher(extractorName);
			if (!m.matches()) {
				Announce.warning("Cannot understand extractor call:", extractorName);
				Announce.failed();
				continue;
			}
			Extractor extractor = Extractor.forName(m.group(1), m.group(2) == null ? null : new File(m.group(2)));
			if (extractor == null) {
				Announce.failed();
				continue;
			}
			extractors.add(extractor);
			Announce.done();
		}
		Announce.done();
		return (extractors);
	}

	/** Run */
	public static void main(String[] args) throws Exception {
		Announce.doing("Creating YAGO");
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		Announce.doing("Initializing from", initFile);
		Parameters.init(initFile);
		Announce.done();
		outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
		call(extractors(Parameters.getList("extractors")));
		Announce.done();
	}
}
