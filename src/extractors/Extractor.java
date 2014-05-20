package extractors;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javatools.administrative.Announce;
import basics.Fact;
import basics.FactComponent;
import basics.Theme;
import basics.YAGO;
import followUp.FollowUpExtractor;

/**
 * Extractor - Yago2s
 * 
 * Superclass of all extractors. It is suggested that the constructor takes as
 * argument the input file.
 * 
 * @author Fabian
 * 
 */
public abstract class Extractor implements Comparable<Extractor> {

	/** The themes required */
	public abstract Set<Theme> input();

	/** The cached themes required */
	public Set<Theme> inputCached() {
		return (Collections.emptySet());
	}

	/** Themes produced */
	public abstract Set<Theme> output();

	/** Returns other extractors to be called en suite */
	public Set<FollowUpExtractor> followUp() {
		return (Collections.emptySet());
	}

	/** Returns the name */
	public String name() {
		return (this.getClass().getName());
	}

	@Override
	public String toString() {
		return name();
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
			if (!theme.isAvailableForReading()) {
				// If you want to run extractors even if the input is absent, use this
				//if(theme.findFileInFolder(inputFolder)!=null)  
				theme.assignToFolder(inputFolder);
			}
		}
		Announce.done();
		for (Theme out : output()) {
			out.forgetFile();
			out.openForWritingInFolder(outputFolder, header + "\n"
					+ out.description + "\n" + out.themeGroup);
		}
		Announce.doing("Extracting");
		extract();
		Announce.done();
		for (Theme out : output())
			out.close();
		Announce.done();
	}

	/** Creates an extractor given by name */
	public static Extractor forName(Class<Extractor> className) {
		Announce.doing("Creating extractor", className);
		Extractor extractor = null;
		try {
			extractor = className.newInstance();
		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}

	@Override
	public int compareTo(Extractor o) {
		return this.name().compareTo(o.name());
	}

	/**
	 * Creates provenance facts, writes fact and meta facts;source will be made
	 * a URI, technique will be made a string
	 */
	public void write(Theme factTheme, Fact f, Theme metaFactTheme,
			String source, String technique) throws IOException {
		Fact sourceFact = f.metaFact(YAGO.extractionSource,
				FactComponent.forUri(source));
		Fact techniqueFact = sourceFact.metaFact(YAGO.extractionTechnique,
				FactComponent.forString(technique));
		factTheme.write(f);
		metaFactTheme.write(sourceFact);
		metaFactTheme.write(techniqueFact);
	}
}
