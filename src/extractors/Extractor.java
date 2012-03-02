package extractors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.filehandlers.FileSet;
import basics.FactSource;
import basics.FactWriter;
import basics.N4Reader;
import basics.N4Writer;
import basics.Theme;

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
	public abstract Set<Theme> input();

	/** Themes produced and descriptions for the themes */
	public abstract Map<Theme, String> output();

	/** Returns the name */
	public final String name() {
		return (this.getClass().getSimpleName());
	}

	@Override
	public String toString() {
		return name();
	}

	/** Main method */
	public abstract void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception;

	/** Convenience method */
	public void extract(File inputFolder, String header) throws Exception {
		extract(inputFolder, inputFolder, header);
	}

	/** Convenience method */
	public void extract(File inputFolder, File outputFolder, String header) throws Exception {
		Announce.doing("Running", this.name());
		Map<Theme, FactSource> input = new HashMap<Theme, FactSource>();
		Announce.doing("Loading input");
		if (input().contains(Theme.ALL)) {
           for(File f : inputFolder.listFiles()) {
        	   if(!f.getName().endsWith(".ttl")) continue;
        	   Theme theme=new Theme(FileSet.noExtension(f.getName()));
        	   input.put(theme, FactSource.from(f));
           }
		} else {
			for (Theme theme : input()) {
				input.put(theme,FactSource.from(theme.file(inputFolder)));
			}
		}
		Announce.done();
		Map<Theme, FactWriter> writers = new HashMap<Theme, FactWriter>();
		Announce.doing("Creating output files");
		for (Entry<Theme, String> entry : output().entrySet()) {
			Announce.doing("Creating file", entry.getKey().name);
			File file = entry.getKey().file(outputFolder);
			// if (file.exists())
			// Announce.error("File", file, "already exists");
			writers.put(entry.getKey(), new N4Writer(file, header + "\n" + entry.getValue()));
			Announce.done();
		}
		Announce.done();
		extract(writers, input);
		for (FactWriter w : writers.values())
			w.close();
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
			Announce.message(ex);
			Announce.failed();
			return (null);
		}
		Announce.done();
		return (extractor);
	}

}
