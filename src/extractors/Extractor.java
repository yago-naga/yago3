package extractors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import basics.FactSource;
import basics.FactWriter;
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

	/** Themes produced*/
	public abstract Set<Theme> output();

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
		for (Theme theme : input()) {
			input.put(theme, FactSource.from(theme.file(inputFolder)));
		}
		Announce.done();
		Map<Theme, FactWriter> writers = new HashMap<Theme, FactWriter>();
		Announce.doing("Creating output files");
		for (Theme out : output()) {
			Announce.doing("Creating file", out.name);
			File file = out.file(outputFolder);
			writers.put(out, new N4Writer(file, header + "\n" + out.description));
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
