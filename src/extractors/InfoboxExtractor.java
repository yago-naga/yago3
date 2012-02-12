package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;
import basics.Fact;
import basics.FactCollection;
import basics.N4Writer;

public class InfoboxExtractor extends Extractor {

	/** Input file*/
	protected File wikipedia;
	
	@Override
	public List<Theme> input() {		
		return Arrays.asList(PatternHardExtractor.INFOBOXPATTERNS, PatternHardExtractor.TITLEPATTERNS,HardExtractor.HARDWIREDFACTS);
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

	/** reads an infobox*/
	public static Map<String,String> readInfobox(Reader in) {
		return(null);
	}
	
	@Override
	public void extract(List<N4Writer> writers,
			List<FactCollection> factCollections) throws Exception {
		Announce.doing("Compiling infobox patterns");
		Map<Pattern, String> patterns = new HashMap<Pattern, String>();
		for (Fact fact : factCollections.get(0).get("<_infoboxPattern>")) {
			patterns.put(Pattern.compile(fact.getArgNoQuotes(1)), fact.getArgNoQuotes(2));
		}
		if(patterns.isEmpty()) {
			Announce.failed();
			throw new Exception("No infobox patterns found");
		}
		Announce.done();
		
		// Extract the information
		Announce.doing("Extracting");
		Reader in = new BufferedReader(new FileReader(wikipedia));
		String titleEntity = null;
		while (true) {
			switch (FileLines.find(in, "<title>", "{{Infobox", "{{infobox", "{{ Infobox", "{{ infobox")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = CategoryExtractor.getTitleEntity(in, factCollections.get(1));
				break;
			default:
				if (titleEntity == null)
					continue;
				String cls=FileLines.readTo(in, '}','|').toString();
				Map<String,String> attributes=readInfobox(in);
			}
		}
	}

	/** Constructor from source file */
	public InfoboxExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

}
