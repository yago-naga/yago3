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
import basics.FactCollection;
import basics.N4Writer;
import extractors.Extractor;

/**
 * Caller - YAGO2s
 * 
 * Calls the extractors, as given in the ini-file. The format in the ini-file is:
 *    extractors = extractors.HardExtractor(./mydatafolder), extractors.WikipediaExtractor(myWikipediaFile), ...
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
		Set<String> themesWeHave = new TreeSet<String>();
		Announce.doing("Calling extractors");
		for (int i = 0; i < extractors.size(); i++) {
			Extractor e = extractors.get(i);
			if (e.input().isEmpty() || themesWeHave.containsAll(e.input())) {
				Announce.message("Current themes:", themesWeHave);
				Announce.doing("Calling", e.name());
				List<FactCollection> input = new ArrayList<FactCollection>();
				Announce.doing("Loading input");
				for (String theme : e.input()) {
					input.add(new FactCollection(new File(outputFolder, theme + ".ttl")));
				}
				Announce.done();
				List<N4Writer> writers = new ArrayList<N4Writer>();
				Announce.doing("Creating output files");
				for (int j=0;j<e.output().size();j++) {
					Announce.doing("Creating file",e.output().get(j));
					File file = new File(outputFolder, e.output().get(j)+ ".ttl");
					if (file.exists())
						Announce.error("File", file, "already exists");
					writers.add(new N4Writer(file, header + e.outputDescriptions().get(j)));
					Announce.done();
				}
				Announce.done();
				try {
					e.extract(writers, input);
					Announce.done();
				} catch (Exception ex) {
					Announce.message(ex);
					ex.printStackTrace();
					Announce.failed();
				}
				for (N4Writer w : writers)
					w.close();
				extractors.remove(i);
				i = 0; // Start again from the beginning
			}
		}
		Announce.done();
	}
	
	/** Creates extractors as given by the names*/
	public static List<Extractor> extractors(List<String> extractorNames) {
		Announce.doing("Creating extractors");
		if(extractorNames==null) {
			Announce.error("No extractors given\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
		}
		if(extractorNames.isEmpty()) {
			Announce.error("Empty extractor list\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
		}
		List<Extractor> extractors = new ArrayList<Extractor>();
		for(String extractorName : extractorNames) {
			Announce.doing("Creating",extractorName);
			Matcher m=Pattern.compile("([A-Za-z0-9\\.]+)\\(([A-Za-z0-9/\\.]*)\\)").matcher(extractorName);
			if(!m.matches()) {
				Announce.warning("Cannot understand extractor call:",extractorName);
				Announce.failed();
				continue;
			}
			try{
				Extractor extractor;
				if(m.group(1).length()>0) {
					extractor=(Extractor) Class.forName(m.group(1)).getConstructor(File.class).newInstance(new File(m.group(2)));
				} else {
					extractor=(Extractor) Class.forName(m.group(1)).newInstance();
				}
				extractors.add(extractor);
			} catch(Exception ex) {
				Announce.warning(ex);
				Announce.failed();
                continue;
			}
			Announce.done();
		}
		Announce.done();
		return(extractors);
	}

	/** Run */
	public static void main(String[] args) throws Exception {
		Announce.doing("Creating YAGO");
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		Announce.doing("Initializing from", initFile);
		Parameters.init(initFile);
		Announce.done();
		outputFolder=Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
		call(extractors(Parameters.getList("extractors")));		
		Announce.done();
	}
}
