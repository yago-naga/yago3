package fromThemes;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import javatools.parsers.Char;
import javatools.parsers.Name.PersonName;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import extractors.Extractor;

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
		return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
	}

	/** Names of people */
	public static final Theme PERSONNAMES = new Theme("personNames",
			"Names of people");

	/** Sources */
	public static final Theme PERSONNAMESOURCES = new Theme(
			"personNameSources", "Sources for the names of people");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(PERSONNAMES, PERSONNAMESOURCES);
	}

	@Override
	public void extract() throws Exception {
		Set<String> people = new TreeSet<>();
		String source = FactComponent
				.forTheme(TransitiveTypeExtractor.TRANSITIVETYPE);
		for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
			if (!f.getRelation().equals(RDFS.type)
					|| !f.getArg(2).equals(YAGO.person))
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
			if (!given.endsWith(".") && given.length() > 1) {
				write(PERSONNAMES, new Fact(f.getArg(1), "<hasGivenName>",
						FactComponent.forStringWithLanguage(given, "eng")),
						PERSONNAMESOURCES, source, "PersonNameExtractor");
			}
			write(PERSONNAMES, new Fact(f.getArg(1), "<hasFamilyName>",
					FactComponent.forStringWithLanguage(family, "eng")),
					PERSONNAMESOURCES, source, "PersonNameExtractor");
		}
	}

	public static void main(String[] args) throws Exception {
		new PersonNameExtractor().extract(new File("c:/fabian/data/yago2s"),
				"test");
	}
}
