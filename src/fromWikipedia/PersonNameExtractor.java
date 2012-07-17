package fromWikipedia;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fromThemes.TransitiveTypeExtractor;

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
  public static final Theme PERSONNAMES = new Theme("personNames", "Names of people");

  /** Sources */
  public static final Theme PERSONNAMESOURCES = new Theme("personNameSources", "Sources for the names of people");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(PERSONNAMES,PERSONNAMESOURCES);
  }

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    Set<String> people = new TreeSet<>();
    for (Theme theme : input.keySet()) {
      Announce.doing("Reading", theme);
      for (Fact f : input.get(theme)) {
        if (!f.getRelation().equals(RDFS.type) || !f.getArg(2).equals(YAGO.person)) continue;
        if (people.contains(f.getArg(1))) continue;
        people.add(f.getArg(1));
        String n = FactComponent.stripBrackets(f.getArg(1));
        n = Char.decode(n);
        PersonName name = new PersonName(n);
        String given = name.givenName();
        if (given == null) continue;
        String family = name.familyName();
        if (family == null) continue;
        if (!given.endsWith(".") && given.length() > 1) {
          write(output, PERSONNAMES, new Fact(f.getArg(1), "<hasGivenName>", FactComponent.forStringWithLanguage(given,"en")), PERSONNAMESOURCES,
              FactComponent.forTheme(theme), "PersonNameExtractor");
        }
        write(output, PERSONNAMES, new Fact(f.getArg(1), "<hasFamilyName>", FactComponent.forStringWithLanguage(family,"en")), PERSONNAMESOURCES,
            FactComponent.forTheme(theme), "PersonNameExtractor");
      }
      Announce.done();
    }
  }

  public static void main(String[] args) throws Exception {
    new PersonNameExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
