package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;
import extractors.EnglishWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.Theme;
import utils.TitleExtractor;

/**
 * Extracts the gender for persons in wikipedia
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
public class GenderExtractor extends EnglishWikipediaExtractor {

  /** gender facts, checked if the entity is a person */
  public static final Theme GENDERBYPRONOUN = new Theme("genderByPronoun", "Gender of a person, guessed from the frequency of pronouns.");

  /** sources */
  public static final Theme GENDERBYPRONOUNSOURCES = new Theme("genderByPronounSources", "Sources for the gender of a person");

  /** Constructor from source file */
  public GenderExtractor(File wikipedia) {
    super(wikipedia);
  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(TransitiveTypeExtractor.TRANSITIVETYPE, PatternHardExtractor.TITLEPATTERNS));
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<Theme>(GENDERBYPRONOUN, GENDERBYPRONOUNSOURCES));
  }

  /** Pattern for "she" */
  private static final Pattern she = Pattern.compile("\\b(she|her)\\b", Pattern.CASE_INSENSITIVE);

  /** Pattern for "he" */
  private static final Pattern he = Pattern.compile("\\b(he|his)\\b", Pattern.CASE_INSENSITIVE);

  @Override
  public void extract() throws Exception {
    FactCollection types = TransitiveTypeExtractor.TRANSITIVETYPE.factCollection();
    TitleExtractor titleExtractor = new TitleExtractor("en");
    Reader in = FileUtils.getBufferedUTF8Reader(inputData);
    String titleEntity = null;
    // Announce.progressStart("Extracting Genders", 4_500_000);
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>")) {
        case -1:
          // Announce.progressDone();
          in.close();
          return;
        case 0:
          // Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          if (titleEntity != null) {
            if (!types.contains(titleEntity, RDFS.type, YAGO.person)) continue;
            String page = FileLines.readBetween(in, "<text", "</text>");
            String normalizedPage = page.replaceAll("[\\s\\x00-\\x1F]+", " ");
            // New heuristics: First pronoun
            // Scroll to beginning of the article
            int startPos = normalizedPage.indexOf("'''");
            if (startPos == -1) startPos = 0;
            Matcher heMatcher = he.matcher(normalizedPage);
            int hePos = heMatcher.find(startPos) ? heMatcher.start() : Integer.MAX_VALUE;
            Matcher sheMatcher = she.matcher(normalizedPage);
            int shePos = sheMatcher.find(startPos) ? sheMatcher.start() : Integer.MAX_VALUE;
            //System.out.printf("He: %d %s", hePos, normalizedPage.substring(hePos - 30, hePos + 30));
            //System.out.printf("She: %d %s", shePos, normalizedPage.substring(shePos - 30, shePos + 30));
            if (hePos < shePos) {
              write(GENDERBYPRONOUN, new Fact(titleEntity, "<hasGender>", "<male>"), GENDERBYPRONOUNSOURCES, FactComponent.wikipediaURL(titleEntity),
                  "GenderExtractor by first pronoun");
              continue;
            }
            if (shePos < hePos) {
              write(GENDERBYPRONOUN, new Fact(titleEntity, "<hasGender>", "<female>"), GENDERBYPRONOUNSOURCES,
                  FactComponent.wikipediaURL(titleEntity), "GenderExtractor by first pronoun");
              continue;
            }
            // Otherwise: give up

            /* Old heuristics
            int male = 0;
            Matcher gm = he.matcher(normalizedPage);
            while (gm.find())
              male++;
            int female = 0;
            gm = she.matcher(normalizedPage);
            while (gm.find())
              female++;
            if (male > female * 2 || (male > 10 && male > female)) {
              write(GENDERBYPRONOUN, new Fact(titleEntity, "<hasGender>", "<male>"), GENDERBYPRONOUNSOURCES, FactComponent.wikipediaURL(titleEntity),
                  "GenderExtractor");
            } else if (female > male * 2 || (female > 10 && female > male)) {
              write(GENDERBYPRONOUN, new Fact(titleEntity, "<hasGender>", "<female>"), GENDERBYPRONOUNSOURCES,
                  FactComponent.wikipediaURL(titleEntity), "GenderExtractor");
            }
            */
          }
          break;
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new GenderExtractor(new File("c:/fabian/data/wikipedia/wikitest_en.xml")).extract(new File("c:/fabian/data/yago3"), "test");
  }
}
