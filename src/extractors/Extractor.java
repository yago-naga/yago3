package extractors;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import basics.FactCollection;
import basics.N4Writer;

/**
 * Extractor - Yago2s
 * 
 * Superclass of all extractors 
 * 
 * @author Fabian
 *
 */
public abstract class Extractor {

	public final List<String> input=Arrays.asList();
	
	public final List<String> output=Arrays.asList();
	
	public abstract void extract(List<N4Writer> writers, List<FactCollection> factCollections, Reader dataInput) throws Exception;
}
