package fromThemes;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Set;
import java.util.TreeSet;

import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import javatools.parsers.Name.PersonName;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
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
	
	 /** Sources */
  public static final Theme PERSONNAMEHEURISTICS = new Theme(
      "personNameHeuristics", "Generates rdfs:label in the form of 'FirstName FamilyName'");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(PERSONNAMES, PERSONNAMESOURCES, PERSONNAMEHEURISTICS);
	}

	@Override
	public void extract() throws Exception {
		Set<String> people = new TreeSet<>();
		String source = TransitiveTypeExtractor.TRANSITIVETYPE.asYagoEntity();
		for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
			if (!f.getRelation().equals(RDFS.type)
					|| !f.getArg(2).equals(YAGO.person))
				continue;
			if (people.contains(f.getArg(1)))
				continue;
			people.add(f.getArg(1));
			String n = FactComponent.stripBrackets(f.getArg(1));
			n = Char17.decode(n);
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
				
				writeNormalized(f.getArg(1), given, source);
				
        write(PERSONNAMEHEURISTICS, new Fact(f.getArg(1), RDFS.label,
            FactComponent.forStringWithLanguage(given + " " + family, "eng")),
            PERSONNAMESOURCES, source, "PersonNameExtractor");
        
        writeNormalized(f.getArg(1), given + " " + family, source);

			}
			write(PERSONNAMES, new Fact(f.getArg(1), "<hasFamilyName>",
					FactComponent.forStringWithLanguage(family, "eng")),
					PERSONNAMESOURCES, source, "PersonNameExtractor");
			
      writeNormalized(f.getArg(1), family, source);
		}
	}

	private void writeNormalized(String entity, String name, String source) throws IOException {
	  String normalizedName =  Normalizer.normalize(name, Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	  if (!normalizedName.equals(name)) {
	    write(PERSONNAMEHEURISTICS, 
	        new Fact(entity, RDFS.label, FactComponent.forStringWithLanguage(normalizedName, "eng")), 
	        PERSONNAMESOURCES, source, "PersonNameExtractor_normalized");
	  }
  }

  public static void main(String[] args) throws Exception {
		new PersonNameExtractor().extract(new File("c:/fabian/data/yago2s"),
				"test");
	}
}
