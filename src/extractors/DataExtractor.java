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

public abstract class DataExtractor extends Extractor {

  /** The file or folder from which we read */
  public final File inputData;

  public DataExtractor(File input) {
    inputData = input;
    if (!inputData.exists()) throw new RuntimeException("Input file does not exist for " + this.getClass() + ": " + inputData);
  }

  /** Creates an extractor given by name */
  public static Extractor forName(Class<DataExtractor> className, File datainput) {
    Extractor extractor = null;
    if (datainput == null) {
      Announce.doing("Creating extractor", className + "(default)");
      try {
        extractor = className.getConstructor().newInstance();
        Announce.done();
        return (extractor);
      } catch (Exception ex) {
        throw new RuntimeException("No data input, and no default constructor for " + className);
      }
    }
    Announce.doing("Creating extractor", className + "(" + datainput + ")");
    if (!datainput.exists()) {
      throw new RuntimeException("File or folder not found: " + datainput);
    }
    try {
      extractor = className.getConstructor(File.class).newInstance(datainput);
    } catch (Exception ex) {
      Announce.error(ex);
    }
    Announce.done();
    return (extractor);
  }

}
