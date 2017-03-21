package fromThemes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.datatypes.FinalSet;
import javatools.datatypes.FrequencyVector;
import javatools.parsers.Char17;
import javatools.parsers.Name.PersonName;
import utils.Theme;

/**
 * Extracts the gender from categories and first names.
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

    // run through category members, guess gender from the category, save in personToGender
    List<Theme> categoryMembers = new ArrayList<>();
    categoryMembers.add(CategoryExtractor.CATEGORYMEMBERS.inEnglish());
    categoryMembers.addAll(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));

    Map<String, String> entityToGender = new HashMap<>();
    Matcher male = Pattern.compile("<wikicat.*_male_.*>", Pattern.CASE_INSENSITIVE).matcher("");
    Matcher female = Pattern.compile("<wikicat.*_female_.*>", Pattern.CASE_INSENSITIVE).matcher("");
    for (Theme t : categoryMembers) {
      for (Fact f : t) {
        String category = f.getObject();
        String gender = null;
        if (male.reset(category).matches()) gender = "<male>";
        else if (female.reset(category).matches()) gender = "<female>";
        if (gender != null) {
          entityToGender.put(f.getSubject(), gender);
        }
      }
    }

    // Run through all people, guess gender from the category, register their first name
    String lastEntity = "";
    boolean isPerson = false;
    String gender = null;
    for (Fact f : TransitiveTypeExtractor.TRANSITIVETYPE) {
      if (!f.getRelation().equals(RDFS.type)) continue;
      if (!lastEntity.equals(f.getSubject())) {
        if (isPerson) {
          if (gender == null) gender = entityToGender.get(lastEntity);
          if (gender != null) {
            registerGender(givenName2gender, lastEntity, gender);
            write(GENDERSBYCATEGORY, new Fact(lastEntity, "<hasGender>", gender), GENDERSOURCES, source, "Gender from category");
          }
        }
        lastEntity = f.getSubject();
        isPerson = false;
        gender = null;
      }
      String category = f.getObject();
      if (category.equals(YAGO.person)) isPerson = true;
      else if (male.reset(category).matches()) gender = "<male>";
      else if (female.reset(category).matches()) gender = "<female>";
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
