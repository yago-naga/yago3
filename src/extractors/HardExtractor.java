package extractors;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import basics.Fact;
import basics.FactCollection;
import basics.N4Reader;
import basics.N4Writer;

public class HardExtractor  extends Extractor {

	public final List<String> output=Arrays.asList("hardWiredFacts");
	
	@Override
	public void extract(List<N4Writer> writers, List<FactCollection> factCollections, Reader dataInput) throws Exception {
		for(Fact f : new N4Reader(dataInput)) {
			writers.get(0).write(f);
		}
	}

}
