package extractors;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.N4Reader;
import basics.Theme;

/**
 * HardExtractor - YAGO2s
 * 
 * Produces the hard-coded facts.
 * 
 * @author Fabian
 * 
 */
public class HardExtractor extends Extractor {

	/** Our output*/
	public static final Theme HARDWIREDFACTS=new Theme("hardWiredFacts");
	
	public Map<Theme,String> output() {
		return (new FinalMap<Theme,String>(HARDWIREDFACTS,"These are the hard-wired facts of YAGO"));
	}

	protected File inputFolder;

	/** Helper */
	public void extract(File input, FactWriter writer) throws Exception {
		if (!input.getName().endsWith(".ttl"))
			return;
		Announce.doing("Copying hard wired facts from", input.getName());
		for (Fact f : FactSource.from(input)) {
			writer.write(f);
		}
		Announce.done();
	}

	@Override
	public void extract(Map<Theme,FactWriter> writers, Map<Theme,FactSource> factCollections) throws Exception {
		Announce.doing("Copying hard wired facts");
		Announce.message("Input folder is", inputFolder);
		for (File f : inputFolder.listFiles())
			extract(f, writers.get(HARDWIREDFACTS));
		Announce.done();
	}

	public HardExtractor(File inputFolder) {
		if (!inputFolder.exists())
			throw new RuntimeException("Folder not found " + inputFolder);
		if (!inputFolder.isDirectory())
			throw new RuntimeException("Not a folder: " + inputFolder);
		this.inputFolder = inputFolder;
	}

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>();
	}
}
