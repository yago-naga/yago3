package extractors;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import basics.Fact;
import basics.FactCollection;
import basics.N4Reader;
import basics.N4Writer;
import basics.RDFS;
import basics.Theme;

public class TypeChecker extends Extractor {

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(InfoboxExtractor.DIRTYINFOBOXFACTS, HardExtractor.HARDWIREDFACTS,WordnetExtractor.WORDNETCLASSES, CategoryExtractor.CATEGORTYPES));
	}

	/** The output of this extractor*/
	public static final Theme CHECKEDINFOBOXFACTS=new Theme("checkedInfoboxFacts");
	
	@Override
	public Map<Theme, String> output() {		
		return new FinalMap<>(CHECKEDINFOBOXFACTS,"The facts extracted from the infoboxes, checked for types");
	}

	@Override
	public void extract(Map<Theme, N4Writer> output, Map<Theme, N4Reader> input) throws Exception {
		FactCollection types=new FactCollection(input.get(WordnetExtractor.WORDNETCLASSES));
		types.load(input.get(CategoryExtractor.CATEGORTYPES));
		FactCollection domRan=new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
		N4Writer out=output.get(CHECKEDINFOBOXFACTS);
		Announce.doing("Type checking facts");
		for(Fact fact : input.get(InfoboxExtractor.DIRTYINFOBOXFACTS)) {
        	String domain=domRan.getArg2(fact.relation, RDFS.domain);
        	if(check(fact.arg2,domain,types)) out.write(fact); 
        }
		Announce.done();
	}

	protected boolean check(String arg2, String domain, FactCollection types) {
		if(domain==null) domain=RDFS.resource;
		Set<String> classes=types.classesOf(arg2);
		return classes!=null && classes.contains(domain);
	}

}
