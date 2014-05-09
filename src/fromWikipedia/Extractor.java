package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javatools.administrative.Announce;
import basics.Fact;
import basics.FactComponent;
import basics.N4Writer;
import basics.Theme;
import basics.YAGO;

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

	/** The cached themes required */
	public Set<Theme> inputCached() {
		return (Collections.emptySet());
	}

	/** Themes produced */
	public abstract Set<Theme> output();

	/** Returns other extractors to be called en suite */
	public Set<Extractor> followUp() {
		return (Collections.emptySet());
	}

	/** Returns the name */
	public String name() {	
		return (this.getClass().getName());
	}

	/** Returns input data file name (if any) */
	public File inputDataFile() {
		return (null);
	}

	@Override
	public String toString() {
		return name();
	}

	/**
	 * Finds the language from the name of the input file, assuming that the
	 * first part of the name before the underline is equal to the language
	 */
	public static String decodeLang(String fileName) {
		if (!fileName.contains("_"))
			return "en";
		return fileName.split("_")[0];
	}

	/** Main method */
	public abstract void extract() throws Exception;

	/** Convenience method */
	public void extract(File inputFolder, String header) throws Exception {
		extract(inputFolder, inputFolder, header);
	}

	/** Convenience method */
	public void extract(File inputFolder, File outputFolder, String header)
			throws Exception {
		Announce.doing("Running", this.name());
		Announce.doing("Loading input");
		for (Theme theme : input()) {
				theme.setFile(theme.file(inputFolder));
		}
		Announce.done();
		Announce.doing("Creating output files");
		for (Theme out : output()) {
			Announce.doing("Creating file", out.name);
			out.open(new N4Writer(out.file(outputFolder), header + "\n" + out.description + "\n"
					+ out.themeGroup));
			Announce.done();
		}
		Announce.done();
		extract();
		for (Theme out : output())
			out.close();
		Announce.done();
	}

	/** Creates an extractor given by name */
	public static Extractor forName(String className, File datainput) {
		Announce.doing("Creating extractor", className);
		if (datainput != null)
			Announce.message("Data input:", datainput);
		if (datainput != null && !datainput.exists()) {
			Announce.message("File or folder not found:", datainput);
			Announce.failed();
			return (null);
		}
		Extractor extractor;
		try {
			if (datainput != null) {
				extractor = (Extractor) Class.forName(className)
						.getConstructor(File.class).newInstance(datainput);
			} else {
				extractor = (Extractor) Class.forName(className).newInstance();
			}
		} catch (Exception ex) {
			Announce.warning(ex);
			Announce.warning(ex.getMessage());
			Announce.failed();
			return (null);
		}
		Announce.done();
		return (extractor);
	}

	/**
	 * Creates provenance facts, writes fact and meta facts;source will be made
	 * a URI, technique will be made a string
	 */
	public void write(Theme factTheme,
			Fact f, Theme metaFactTheme, String source, String technique)
			throws IOException {
		Fact sourceFact = f.metaFact(YAGO.extractionSource,
				FactComponent.forUri(source));
		Fact techniqueFact = sourceFact.metaFact(YAGO.extractionTechnique,
				FactComponent.forString(technique));
		factTheme.write(f);
		metaFactTheme.write(sourceFact);
		metaFactTheme.write(techniqueFact);
	}
}
