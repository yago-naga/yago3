package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import basics.FactCollection;
import basics.N4Writer;
import extractors.Extractor;
import extractors.HardExtractor;

/**
 * Caller - YAGO2s
 * 
 * Calls the extractors
 * 
 * @author Fabian
 * 
 */
public class Caller {
	public static File outputFolder;

	public static String header = "This file is part of the ontology YAGO2s\nIt is licensed under a Creative-Commons Attribution License by the YAGO team\nat the Max Planck Institute for Informatics/Germany.\nSee http://yago-knowledge.org for all details.\n\n";

	public static void call(Extractor... extractors) throws Exception {
		Set<String> themesWeHave = new TreeSet<String>();
		Announce.doing("Creating YAGO");
		for (int i = 0; i < extractors.length; i++) {
			Extractor e = extractors[i];
			if (themesWeHave.containsAll(e.input) && !themesWeHave.containsAll(e.output.keySet())) {
				Announce.message("Current themes:",themesWeHave);
				Announce.doing("Calling", e.name());
				List<FactCollection> input = new ArrayList<FactCollection>();
				Announce.doing("Loading input");
				for (String theme : e.input) {
					input.add(new FactCollection(new File(outputFolder, theme + ".ttl")));
				}
				Announce.done();
				List<N4Writer> writers = new ArrayList<N4Writer>();
				Announce.doing("Creating output");
				for (String theme : e.output.keySet()) {
					File file = new File(outputFolder, theme + ".ttl");
					if (file.exists())
						Announce.error("File", file, "already exists");
					writers.add(new N4Writer(file, header + e.output.get(theme)));
				}
				Announce.done();
				e.extract(writers, input);
				Announce.done();
				i = 0; // Start again from the beginning
			}
		}
		Announce.done();
	}
	
	public static void main(String[] args) throws Exception {
		call(new HardExtractor(new File("/Users/Fabian/Fabian/Work/yago2/")));
	}
}
