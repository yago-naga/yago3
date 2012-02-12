package extractors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javatools.administrative.Announce;

import basics.Fact;
import basics.FactCollection;
import basics.N4Writer;

public class InfoboxExtractor extends Extractor {

	@Override
	public List<Theme> input() {		
		return Arrays.asList(CategoryExtractor.CATEGORTYPES, PatternHardExtractor.INFOBOXPATTERNS);
	}

	/** Infobox facts, non-checked*/
	public static final Theme DIRTYINFOBOXFACTS=new Theme("dirtyInfoxboxFacts");
	/** Types derived from infoboxes*/
	public static final Theme INFOBOXTYPES=new Theme("infoboxTypes");

	@Override
	public List<Theme> output() {
		return Arrays.asList(DIRTYINFOBOXFACTS, INFOBOXTYPES);
	}

	@Override
	public List<String> outputDescriptions() {		
		return Arrays.asList("Facts extracted from the Wikipedia infoboxes - still to be type-checked","Types extracted from Wikipedia infoboxes");
	}

	@Override
	public void extract(List<N4Writer> writers,
			List<FactCollection> factCollections) throws Exception {
		Announce.doing("Compiling infobox patterns");
		Map<Pattern, String> patterns = new HashMap<Pattern, String>();
		for (Fact fact : factCollections.get(1).get("<_infoboxPattern>")) {
			patterns.put(Pattern.compile(fact.getArgNoQuotes(1)), fact.getArgNoQuotes(2));
		}
		if(patterns.isEmpty()) {
			Announce.failed();
			throw new Exception("No category patterns found");
		}
	}

}
