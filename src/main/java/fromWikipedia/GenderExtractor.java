/*
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

package fromWikipedia;

import java.io.File;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.Fact.ImplementationNote;
import basics.FactComponent;
import basics.YAGO;
import extractors.MultilingualWikipediaExtractor;
import fromOtherSources.PatternHardExtractor;
import fromThemes.TransitiveTypeExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalMap;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

/**
 * Extracts the gender for persons in wikipedia
 * 
*/
public class GenderExtractor extends MultilingualWikipediaExtractor {

  /** gender facts, checked if the entity is a person */
  public static final MultilingualTheme GENDERBYPRONOUN = new MultilingualTheme("genderByPronoun",
      "Gender of a person, guessed from the frequency of pronouns.");

  /** sources */
  public static final MultilingualTheme GENDERBYPRONOUNSOURCES = new MultilingualTheme("genderByPronounSources",
      "Sources for the gender of a person");

  /** Constructor from source file */
  public GenderExtractor(String language, File wikipedia) {
    super(language, wikipedia);
  }

  @Override
  public Set<Theme> input() {
    return new TreeSet<Theme>(Arrays.asList(TransitiveTypeExtractor.TRANSITIVETYPE, PatternHardExtractor.TITLEPATTERNS));
  }

  @Override
  public Set<Theme> output() {
    return (new FinalSet<Theme>(GENDERBYPRONOUN.inLanguage(language), GENDERBYPRONOUNSOURCES.inLanguage(language)));
  }

  /** Map from the language code to the Pattern for "she" */
  private static final Map<String, Pattern> lang2she = new FinalMap<>("en", Pattern.compile("\\b(she|her)\\b", Pattern.CASE_INSENSITIVE), "de",
      Pattern.compile("\\b(sie|ihre|ihr|ihren)\\b", Pattern.CASE_INSENSITIVE));

  /** Map from the language code to the Pattern for "he" */
  private static final Map<String, Pattern> lang2he = new FinalMap<>("en", Pattern.compile("\\b(he|his)\\b", Pattern.CASE_INSENSITIVE), "de",
      Pattern.compile("\\b(er|seinen|seine|ihm)\\b", Pattern.CASE_INSENSITIVE));

  @Override
  public void extract() throws Exception {
    @ImplementationNote("If we don't have the language, output some bogus facts just to make sure the oputput file is large enough so that the ParralelCaller thinks the work is done.")
    Pattern she = lang2she.get(language);
    if (she == null) {
      for (int i = 0; i < 20; i++) {
        write(GENDERBYPRONOUN.inLanguage(language), new Fact("<Elvis_Presley>", "<hasGender>", "<male>"), GENDERBYPRONOUNSOURCES.inLanguage(language),
            FactComponent.wikipediaURL("<Elvis_Presley>"), "GenderExtractor by first pronoun fillup fact");
      }
      return;
    }
    Pattern he = lang2he.get(language);
    Map<String, Set<String>> subjToTypes = TransitiveTypeExtractor.getSubjectToTypes();
    TitleExtractor titleExtractor = new TitleExtractor("en");
    Reader in = FileUtils.getBufferedUTF8Reader(this.wikipedia);
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
            Set<String> types = subjToTypes.get(titleEntity);
            if (types == null || !types.contains(YAGO.person)) continue;
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
              write(GENDERBYPRONOUN.inLanguage(language), new Fact(titleEntity, "<hasGender>", "<male>"), GENDERBYPRONOUNSOURCES.inLanguage(language),
                  FactComponent.wikipediaURL(titleEntity), "GenderExtractor by first pronoun");
              continue;
            }
            if (shePos < hePos) {
              write(GENDERBYPRONOUN.inLanguage(language), new Fact(titleEntity, "<hasGender>", "<female>"),
                  GENDERBYPRONOUNSOURCES.inLanguage(language), FactComponent.wikipediaURL(titleEntity), "GenderExtractor by first pronoun");
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
    new GenderExtractor("de", new File("c:/fabian/data/wikipedia/wikitest_de.xml")).extract(new File("c:/fabian/data/yago3"), "test");
  }
}
