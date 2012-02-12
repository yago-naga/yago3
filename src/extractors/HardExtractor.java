package extractors;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javatools.administrative.Announce;
import basics.Fact;
import basics.FactCollection;
import basics.N4Reader;
import basics.N4Writer;

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
	
	public List<Theme> output() {
		return (Arrays.asList(HARDWIREDFACTS));
	}

	public List<String> outputDescriptions() {
		return (Arrays.asList("These are the hard-wired facts of YAGO"));
	}

	protected File inputFolder;

	/** Helper */
	public void extract(File input, N4Writer writer) throws Exception {
		if (!input.getName().endsWith(".ttl"))
			return;
		Announce.doing("Copying hard wired facts from", input.getName());
		for (Fact f : new N4Reader(input)) {
			writer.write(f);
		}
		Announce.done();
	}

	@Override
	public void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception {
		Announce.doing("Copying hard wired facts");
		Announce.message("Input folder is", inputFolder);
		for (File f : inputFolder.listFiles())
			extract(f, writers.get(0));
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
	public List<Theme> input() {
		return Arrays.asList();
	}
}
