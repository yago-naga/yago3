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

	public final Map<String,String> output=new FinalMap<String,String>("hardWiredFacts","These are the hard-wired facts of YAGO");

	protected File inputFile;
	
	@Override
	public void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception {
		Announce.doing("Copying hard wired facts from",inputFile.getName());
		for(Fact f : new N4Reader(inputFile)) {
			writers.get(0).write(f);
		}
		Announce.doing();
	}

	public HardExtractor(File input) {
		inputFile=input;
	}
}
