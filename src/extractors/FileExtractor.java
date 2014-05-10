package extractors;

import java.io.File;

import javatools.administrative.Announce;

/**
 * DataFileExtractor - Yago2s
 * 
 * Superclass of all extractors that take a data file as input (except
 * multilingual Wikipedia, see there).
 * 
 * By convention, all subclasses have a constructor that takes as argument the
 * data file.
 * 
 * @author Fabian
 * 
 */

public abstract class FileExtractor extends Extractor {

	/** The file or folder from which we read */
	protected final File inputData;

	public FileExtractor(File input) {
		inputData = input;
	}

	/** Creates an extractor given by name */
	public static Extractor forName(Class<FileExtractor> className,
			File datainput) {
		Announce.doing("Creating extractor", className + "(" + datainput + ")");
		if (datainput == null) {
			Announce.error("No data input");
		}
		if (!datainput.exists()) {
			Announce.error("File or folder not found:", datainput);
		}
		Extractor extractor=null;
		try {
			extractor = className.getConstructor(File.class).newInstance(
					datainput);

		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}

}
