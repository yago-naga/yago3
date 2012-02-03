package extractors;

import java.io.File;
import java.util.List;
import java.util.Map;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
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
public class HardExtractor  extends Extractor {

	public Map<String,String> output() {
		return(new FinalMap<String,String>("hardWiredFacts","These are the hard-wired facts of YAGO"));
	}

	protected File inputFolder;

	/** Helper*/
	public void extract(File input, N4Writer writer) throws Exception {
		if(!input.getName().endsWith(".ttl")) return;
		Announce.doing("Copying hard wired facts from",input.getName());		
		for(Fact f : new N4Reader(input)) {
			writer.write(f);
		}
		Announce.done();
	}

	@Override
	public void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception {
		Announce.doing("Copying hard wired facts");
		Announce.message("Folder is",inputFolder);
		for(File f : inputFolder.listFiles()) extract(f,writers.get(0));
		Announce.done();
	}

	public HardExtractor(File inputFolder) {
		if(!inputFolder.exists()) throw new RuntimeException("Folder not found "+inputFolder);
		if(!inputFolder.isDirectory()) throw new RuntimeException("Not a folder: "+inputFolder);
		this.inputFolder=inputFolder;
	}
}
