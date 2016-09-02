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
import javatools.datatypes.FinalSet;
import javatools.datatypes.FrequencyVector;
import javatools.parsers.Char17;
import javatools.parsers.Name.PersonName;
import utils.Theme;

/**
 * YAGO2s - GenderNameExtractor
 * 
 * Extracts the gender from categories and first names.
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
  public static final Theme GENDERSOURCES = new Theme("genderSources", "Sources for the genders determined by given name.");

  /** Sources for category facts */
  public static final Theme GENDERSBYCATEGORY = new Theme("genderByCategory", "Genders derived from the Wikipedia categories");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new HashSet<>();
    result.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    return (result);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(GENDERNAMES, GENDERSBYNAME, GENDERSBYCATEGORY, GENDERSOURCES);
  }

  /** Returns the given name of a person (or null)*/
  protected static String givenName(String yagoEntity) {
    return (new PersonName(Char17.decode(FactComponent.stripQualifier(FactComponent.stripBracketsAndLanguage(yagoEntity)))).givenName());
  }

  /** Returns the gender from a map of names to gender counts (or NULL)*/
  protected static String gender(Map<String, int[]> givenName2gender, String givenName) {
    int[] gender = givenName2gender.get(givenName);
    if (gender == null) return (null);
    if (FrequencyVector.wilson(gender[0] + gender[1], gender[0])[0] > 0.95) return ("<male>");
    if (FrequencyVector.wilson(gender[0] + gender[1], gender[1])[0] > 0.95) return ("<female>");
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
    String source = TransitiveTypeExtractor.TRANSITIVETYPE.asYagoEntity();
    Map<String, int[]> givenName2gender = new HashMap<>();

    // Run through all people, guess gender from the category, register their first name
    String lastEntity = "";
    boolean isPerson = false;
    String gender = null;
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      if (!lastEntity.equals(f.getSubject())) {
        if (isPerson && gender != null) {
          registerGender(givenName2gender, lastEntity, gender);
          write(GENDERSBYCATEGORY, new Fact(lastEntity, "<hasGender>", gender), GENDERSOURCES, source, "Gender from category");
        }
        lastEntity = f.getSubject();
        isPerson = false;
        gender = null;
      }
      String category = f.getObject();
      if (category.equals(YAGO.person)) isPerson = true;
      else if (category.startsWith("<wikicat_Male_")) gender = "<male>";
      else if (category.startsWith("<wikicat_Female_")) gender = "<female>";
    }

    // Write out the given names with their determined gender
    for (String givenName : givenName2gender.keySet()) {
      gender = gender(givenName2gender, givenName);
      if (gender != null) GENDERNAMES.write(new Fact(FactComponent.forString(givenName), "<hasGender>", gender));
    }

    // Deduce the gender from the first name
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (f.getRelation().equals(RDFS.type) && f.getObject().equals(YAGO.person)) {
        String givenName = givenName(f.getSubject());
        if (givenName != null) {
          gender = gender(givenName2gender, givenName);
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
