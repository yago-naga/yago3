package fromThemes;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import javatools.parsers.Name.PersonName;
import utils.Theme;

/**
 * YAGO2s - PersonNameExtractor
 * 
 * Extracts given name and family name for people.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class GenderNameExtractor extends Extractor {

  /** Given names associated with a gender, deduced from categories*/
  public static final Theme GENDERNAMES = new Theme("genderNames", "Gender of first names derived from the categories");

  /** Gender, derived from the first names*/
  public static final Theme GENDERSBYNAME = new Theme("genderByName", "Gender of people, derived from the given name");

  /** Sources */
  public static final Theme GENDERSOURCES = new Theme("genderByNameSources", "Sources for the genders determined by given name.");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new HashSet<>();
    result.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    result.addAll(CategoryGenderExtractor.CATEGORYGENDER.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return (result);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(GENDERNAMES, GENDERSBYNAME, GENDERSOURCES);
  }

  /** Returns the given name of a person (or null)*/
  protected static String givenName(String yagoEntity) {
    return (new PersonName(Char17.decode(FactComponent.stripQualifier(FactComponent.stripBracketsAndLanguage(yagoEntity)))).givenName());
  }

  /** Returns the gender from a map of names to gender counts (or NULL)*/
  protected static String gender(Map<String, int[]> givenName2gender, String givenName) {
    int[] gender = givenName2gender.get(givenName);
    if (gender == null) return (null);
    if (gender[0] + gender[1] < 100) return (null);
    if (gender[0] > 0.95 * (gender[0] + gender[1])) return ("<male>");
    else if (gender[1] > 0.95 * (gender[0] + gender[1])) return ("<female>");
    return (null);
  }

  /** Registers a gender count*/
  private void registerGender(Map<String, int[]> firstName2gender, String subject, String gender) {
    String givenName = givenName(subject);
    if (givenName == null) return;
    int[] counters = firstName2gender.get(givenName);
    if (counters == null) firstName2gender.put(givenName, counters = new int[2]);
    counters[gender.equals("<male>") ? 0 : 1]++;
  }

  @Override
  public void extract() throws Exception {
    // Run through all people of whom we know the gender
    Map<String, int[]> givenName2gender = new HashMap<>();
    for (Theme categoryGenders : CategoryGenderExtractor.CATEGORYGENDER.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      for (Fact f : categoryGenders) {
        if (!f.getRelation().equals("<hasGender>")) continue;
        registerGender(givenName2gender, f.getSubject(), f.getObject());
      }
    }

    // Write out the given names with their determined gender
    for (String givenName : givenName2gender.keySet()) {
      String gender = gender(givenName2gender, givenName);
      if (gender != null) GENDERNAMES.write(new Fact(FactComponent.forString(givenName), "<hasGender>", gender));
    }

    // Write out the gender of all other people
    String source = TransitiveTypeExtractor.TRANSITIVETYPE.asYagoEntity();
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (f.getRelation().equals(RDFS.type) && f.getObject().equals(YAGO.person)) {
        String givenName = givenName(f.getSubject());
        if (givenName != null) {
          String gender = gender(givenName2gender, givenName);
          if (gender != null) {
            write(GENDERSBYNAME, new Fact(f.getSubject(), "<hasGender>", gender), GENDERSOURCES, source, "Gender derived from given name.");
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    new GenderNameExtractor().extract(new File("c:/fabian/data/yago3"), "test");
  }
}
