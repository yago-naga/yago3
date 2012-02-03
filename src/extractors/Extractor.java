package extractors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import basics.FactCollection;
import basics.N4Writer;

/**
 * Extractor - Yago2s
 * 
 * Superclass of all extractors. It is suggested that the constructor takes as argument the input file. 
 * 
 * @author Fabian
 *
 */
public abstract class Extractor {

	/** The themes required*/
	public final List<String> input=Arrays.asList();
	
	/** The themes produced with descriptions*/
	public final Map<String,String> output=new TreeMap<String,String>();
	
	/** Returns the name*/
	public final String name() {
		return(this.getClass().getName());
	}
	
	/** Main method*/
	public abstract void extract(List<N4Writer> writers, List<FactCollection> factCollections) throws Exception;
}
