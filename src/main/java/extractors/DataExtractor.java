package extractors;

import java.io.File;

import javatools.administrative.Announce;

/**
 * Superclass of all extractors that take a data file as input (except
 * multilingual Wikipedia, see there).
 *
 * By convention, all subclasses have a constructor that takes as argument the
 * data file.
 *
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
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
        throw new RuntimeException("No data input, and no default constructor for " + className + ". Exception: " + ex);
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
