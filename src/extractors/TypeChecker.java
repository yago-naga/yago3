package extractors;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import extractors.Extractor.FollowUpExtractor;
import finalExtractors.TransitiveTypeExtractor;

/**
 * YAGO2s - TypeChecker
 * 
 * Does a type check on infobox facts. 
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class TypeChecker extends FollowUpExtractor {

	@Override
	public Set<Theme> input() {
		return new TreeSet<Theme>(Arrays.asList(checkMe, TransitiveTypeExtractor.TRANSITIVETYPE, HardExtractor.HARDWIREDFACTS));
	}

	/** Constructor, takes theme to be checked and theme to output*/
	public TypeChecker(Theme in, Theme out) {
		checkMe=in;
		checked=out;
	}
	
	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
	  Map<String,Set<String>> types=TransitiveTypeExtractor.yagoTaxonomy(input);
	  FactCollection domRan=new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
		FactWriter out = output.get(checked);
		Announce.doing("Type checking facts");
		for (Fact fact : input.get(checkMe)) {			
			String domain = domRan.getArg2(fact.getRelation(), RDFS.domain);
			if (!check(fact.getArg(1), domain, types)) {
				Announce.debug("Domain check failed", fact);
				continue;
			}
			if (FactComponent.isLiteral(fact.getArg(2))) {
        out.write(fact);
        continue;
      }
			String range = domRan.getArg2(fact.getRelation(), RDFS.range);
			if (check(fact.getArg(2), range, types))
				out.write(fact);
			else
				Announce.debug("Range check failed", fact);
		}
		Announce.done();
	}

	/** Checks whether an entity is of a type */
	protected boolean check(String entity, String type, Map<String,Set<String>> types) {
		if (type == null) {
		  // Type is entity, just check it's not a literal
		  return(!FactComponent.isLiteral(entity));
		}
		Set<String> myTypes=types.get(entity);
		return (myTypes!=null && myTypes.contains(type));
	}

}
