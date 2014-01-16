package fromWikipedia;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
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
  
	protected static String[] languages = {"en" , "de" };
	static Map<String,String> langPostfixes = new HashMap<String, String>();
	static{
		for(String s:languages) {
			if(!s.equals("en"))
				langPostfixes.put(s, "_"+s);
			else
				langPostfixes.put(s, "");
		}
	}
  
	/* Finds the language from the name of the input file, 
	* assuming that the first part of the name before the
	*  underline is equal to the language */
	public static String decodeLang(String fileName) {
		if (!fileName.contains("_")) return "en";
		return fileName.split("_")[0];
	}

	/** The themes required */
	public abstract Set<Theme> input();

	/** Themes produced*/
	public abstract Set<Theme> output();

	/** A Dummy class to indicate extractors that are called en suite*/
	public static abstract class FollowUpExtractor extends Extractor {
		/** This is the theme we want to check*/
		protected Theme checkMe;

		/** This is the theme we produce*/
		protected Theme checked;
		
		/** This is the theme we produce*/
		protected Extractor parent;
		
		@Override
	  public Set<Theme> output() {
	    return new FinalSet<>(checked);
	  }
		
		@Override
		public String name() {
			if (parent != null ) {
				return String.format("%s:%s", super.name(), parent.name());
			}
			else {
				return super.name();
			}
		}
	}
	
	/** Returns other extractors to be called en suite*/
	@SuppressWarnings("unchecked")
	public Set<Extractor> followUp() {
		return(Collections.EMPTY_SET);
	}
	
	/** Returns the name */
	public String name() {
		return (this.getClass().getName());
	}

	/** Returns input data file name (if any)*/
	public File inputDataFile() {
	  return(null);
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
			writers.put(out, new N4Writer(file, header + "\n" + out.description+"\n"+out.themeGroup));
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
		if(datainput!=null) Announce.message("Data input:",datainput);
		if(datainput!=null && !datainput.exists()) {
		  Announce.message("File or folder not found:",datainput);
		  Announce.failed();
		  return(null);
		}
		Extractor extractor;
		try {
			if (datainput != null) {
				extractor = (Extractor) Class.forName(className).getConstructor(File.class).newInstance(datainput);
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

	 /** Creates provenance facts, writes fact and meta facts;source will be made a URI, technique will be made a string*/
	  public void write(Map<Theme,FactWriter> factWriters, Theme factTheme, Fact f, Theme metaFactTheme, String source, String technique) throws IOException {
		  write(factWriters.get(factTheme),f,factWriters.get(metaFactTheme),source,technique);
	  }
	  
	 /** Creates provenance facts, writes fact and meta facts; source will be made a URI, technique will be made a string*/
	  public void write(FactWriter factWriter, Fact f, FactWriter metaFactWriter, String source, String technique) throws IOException {
		  Fact sourceFact=f.metaFact(YAGO.extractionSource,FactComponent.forUri(source));
		  Fact techniqueFact=sourceFact.metaFact(YAGO.extractionTechnique, FactComponent.forString(technique));
		  factWriter.write(f);
		  metaFactWriter.write(sourceFact);
		  metaFactWriter.write(techniqueFact);
	  }
}
