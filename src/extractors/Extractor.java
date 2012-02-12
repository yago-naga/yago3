package extractors;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javatools.administrative.Announce;
import javatools.datatypes.Pair;
import basics.FactCollection;
import basics.N4Writer;

/**
 * Extractor - Yago2s
 * 
 * Superclass of all extractors. It is suggested that the constructor takes as
 * argument the input file.
 * 
 * @author Fabian
 * 
 */
public abstract class Extractor {

	/** The themes required */
	public abstract List<Theme> input();

	/** The themes produced */
	public abstract List<Theme> output();

	/** Descriptions for the themes */
	public abstract List<String> outputDescriptions();

	/** Returns the name */
	public final String name() {
		return (this.getClass().getSimpleName());
	}

	@Override
	public String toString() {
		return name();
	}
	/** Main method */
	public abstract void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception;

	/** Convenience method */
	public void extract(File inputFolder, String header) throws Exception {
		extract(inputFolder, inputFolder, header);
	}
	
	/** Convenience method */
	public void extract(File inputFolder, File outputFolder, String header) throws Exception {
		Announce.doing("Running",this.name());
		List<FactCollection> input = new ArrayList<FactCollection>();
		Announce.doing("Loading input");
		for (Theme theme : input()) {
			input.add(new FactCollection(theme.file(inputFolder)));
		}
		Announce.done();
		List<N4Writer> writers = new ArrayList<N4Writer>();
		Announce.doing("Creating output files");
		for (int j = 0; j < output().size(); j++) {
			Announce.doing("Creating file", output().get(j));
			File file = output().get(j).file(outputFolder);
			//if (file.exists())
			//	Announce.error("File", file, "already exists");
			writers.add(new N4Writer(file, header + outputDescriptions().get(j)));
			Announce.done();
		}
		Announce.done();
		extract(writers,input);
		for(N4Writer w : writers) w.close();
		Announce.done();
	}

	/** Creates an extractor given by name */
	public static Extractor forName(String className, File datainput) {
		Announce.doing("Creating extractor", className);
		Extractor extractor;
		try {
			if (datainput != null) {
				extractor = (Extractor) Class.forName(className).getConstructor(File.class).newInstance(datainput);
			} else {
				extractor = (Extractor) Class.forName(className).newInstance();
			}
		} catch (Exception ex) {
			Announce.message(ex.getMessage());
			Announce.failed();
			return (null);
		}
		Announce.done();
		return (extractor);
	}

}
