package extractors;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import javatools.parsers.Name.PersonName;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import finalExtractors.TransitiveTypeExtractor;

/**
 * YAGO2s - PersonNameExtractor
 * 
 * Extracts given name and family name for people.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class PersonNameExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(CategoryExtractor.CATEGORYTYPES, InfoboxExtractor.INFOBOXTYPES,
				TransitiveTypeExtractor.TRANSITIVETYPE);
	}

	/** Names of people */
	public static final Theme PERSONNAMES = new Theme("personNames", "Names of people");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(PERSONNAMES);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		Set<String> people = new TreeSet<>();
		for (Theme theme : input.keySet()) {
			Announce.doing("Reading", theme);
			for (Fact f : input.get(theme)) {
				if (!f.getRelation().equals(RDFS.type) || !f.getArg(2).equals(YAGO.person))
					continue;
				if (people.contains(f.getArg(1)))
					continue;
				people.add(f.getArg(1));
				String n = FactComponent.stripBrackets(f.getArg(1));
				n = Char.decode(n);
				PersonName name = new PersonName(n);
				String given = name.givenName();
				if (given == null)
					continue;
				String family = name.familyName();
				if (family == null)
					continue;
				output.get(PERSONNAMES).write(new Fact(f.getArg(1), "<hasGivenName>", FactComponent.forString(given)));
				output.get(PERSONNAMES)
						.write(new Fact(f.getArg(1), "<hasFamilyName>", FactComponent.forString(family)));
			}
			Announce.done();
		}
	}

	public static void main(String[] args) throws Exception {
		new PersonNameExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
	}
}
