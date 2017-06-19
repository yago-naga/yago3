package fromThemes;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import fromOtherSources.PatternHardExtractor;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import javatools.parsers.Name.PersonName;
import utils.Theme;

/**
 * Extracts given name and family name for people.
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek.

YAGO is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

YAGO is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with YAGO.  If not, see <http://www.gnu.org/licenses/>.
*/
public class PersonNameExtractor extends Extractor {

  /** 2-letter language codes for language which the extractor should consider
   * when applying the first name/family name heuristics*/
  private final Set<String> supportedLanguages = new HashSet<String>(Arrays.asList("en", "de", "es", "it", "pl", "nl", "ro", "fr"));

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE, PatternHardExtractor.LANGUAGECODEMAPPING);
  }

  /** Names of people */
  public static final Theme PERSONNAMES = new Theme("personNames", "Names of people");

  /** Sources */
  public static final Theme PERSONNAMESOURCES = new Theme("personNameSources", "Sources for the names of people");

  /** Sources */
  public static final Theme PERSONNAMEHEURISTICS = new Theme("personNameHeuristics", "Generates rdfs:label in the form of 'FirstName FamilyName'");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(PERSONNAMES, PERSONNAMESOURCES, PERSONNAMEHEURISTICS);
  }

  @Override
  public void extract() throws Exception {
    Map<String, String> languagemap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");

    Set<String> people = new TreeSet<>();
    String source = TransitiveTypeExtractor.TRANSITIVETYPE.asYagoEntity();
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (!f.getRelation().equals(RDFS.type) || !f.getArg(2).equals(YAGO.person)) continue;
      if (people.contains(f.getArg(1))) continue;
      people.add(f.getArg(1));
      String entity = f.getArg(1);
      String lang = FactComponent.getLanguageOfEntity(entity);
      if (lang == null) {
        // If the entity does not have any language, the default language is English.
        lang = "en";
      }
      if (lang != null && !supportedLanguages.contains(lang)) {
        continue;
      }
      String n = FactComponent.stripBracketsAndLanguage(entity);
      n = FactComponent.stripQualifier(n);
      n = Char17.decode(n);
      PersonName name = new PersonName(n);
      String given = name.givenName();
      if (given == null) continue;
      String family = name.familyName();
      if (family == null) continue;
      lang = languagemap.getOrDefault(lang, lang);
      if (!given.endsWith(".") && given.length() > 1) {
        write(PERSONNAMES, new Fact(f.getArg(1), "<hasGivenName>", FactComponent.forStringWithLanguage(given, lang)), PERSONNAMESOURCES, source,
            "PersonNameExtractor");

        writeNormalized(f.getArg(1), given, source);

        write(PERSONNAMEHEURISTICS, new Fact(f.getArg(1), RDFS.label, FactComponent.forStringWithLanguage(given + " " + family, lang)),
            PERSONNAMESOURCES, source, "PersonNameExtractor");

        writeNormalized(f.getArg(1), given + " " + family, source);

      }
      write(PERSONNAMES, new Fact(f.getArg(1), "<hasFamilyName>", FactComponent.forStringWithLanguage(family, lang)), PERSONNAMESOURCES, source,
          "PersonNameExtractor");

      writeNormalized(f.getArg(1), family, source);
    }
  }

  private void writeNormalized(String entity, String name, String source) throws IOException {
    String normalizedName = Normalizer.normalize(name, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    if (!normalizedName.equals(name)) {
      write(PERSONNAMEHEURISTICS, new Fact(entity, RDFS.label, FactComponent.forStringWithLanguage(normalizedName, "eng")), PERSONNAMESOURCES, source,
          "PersonNameExtractor_normalized");
    }
  }

  public static void main(String[] args) throws Exception {
    new PersonNameExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
