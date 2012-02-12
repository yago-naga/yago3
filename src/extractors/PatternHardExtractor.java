package extractors;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import basics.Fact;
import basics.FactCollection;
import basics.N4Reader;
import basics.N4Writer;

/**
 * PatternHardExtractor - YAGO2s
 * 
 * Produces the hard-coded facts that are YAGO-internal.
 * 
 * @author Fabian
 *
 */
public class PatternHardExtractor extends HardExtractor {

	/** Patterns of infoboxes*/
	public static final Theme INFOBOXPATTERNS=new Theme("_infoboxPatterns");
	/** Patterns of titles*/
	public static final Theme TITLEPATTERNS=new Theme("_titlePatterns");
	/** Patterns of categories*/
	public static final Theme CATEGORYPATTERNS=new Theme("_categoryPatterns");
	
	public List<Theme> output() {
		return (Arrays.asList(INFOBOXPATTERNS,TITLEPATTERNS,CATEGORYPATTERNS));
	}

	public List<String> outputDescriptions() {
		return (Arrays.asList("These are the Wikipedia infobox patterns used in YAGO",
				"These are the replacement patterns for Wikipedia titles used in YAGO",
				"These are the Wikipedia category patterns used in YAGO"));
	}

	@Override
	public void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception {
		Announce.doing("Copying patterns");
		Announce.message("Input folder is",inputFolder);
		for(int i=0;i<output().size();i++) {
		  extract(new File(inputFolder,output().get(i)+".ttl"),writers.get(i));
		}
		Announce.done();
	}

	public PatternHardExtractor(File inputFolder) {
		super(inputFolder);
	}
}
