/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Johannes Hoffart.

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

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import extractors.Extractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;
import javatools.parsers.Char17;
import utils.FactCollection;
import utils.FactTemplateExtractor;
import utils.MultilingualTheme;
import utils.PatternList;
import utils.Theme;
import utils.TitleExtractor;

/**
 * Extracts context keyphrases (the X in SPOTLX) facts from Wikipedia.
 *
 * For now, the provenance generation (yagoConteXtFacts) is disabled.

*/
public class ConteXtExtractor extends MultilingualWikipediaExtractor {

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new TreeSet<>();
    input.add(PatternHardExtractor.TITLEPATTERNS);
    input.add(PatternHardExtractor.CONTEXTPATTERNS);
    input.add(PatternHardExtractor.AIDACLEANINGPATTERNS);
    input.add(PatternHardExtractor.LANGUAGECODEMAPPING);
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;
  }

  @Override
  public Set<Theme> inputCached() {
    Set<Theme> input = new TreeSet<>();
    input.add(PatternHardExtractor.TITLEPATTERNS);
    input.add(PatternHardExtractor.CONTEXTPATTERNS);
    input.add(PatternHardExtractor.AIDACLEANINGPATTERNS);
    input.add(PatternHardExtractor.LANGUAGECODEMAPPING);
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;  }

  public static final MultilingualTheme CONTEXTFACTSNEEDSTRANSLATION = new MultilingualTheme("conteXtFactsNeedsTranslation",
      "Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names");

  /** Context for entities */
  public static final MultilingualTheme CONTEXTFACTS = new MultilingualTheme("yagoConteXtFacts",
      "Keyphrases for the X in SPOTLX - gathered from (internal and external) link anchors, citations and category names");

  @Override
  public Set<Theme> output() {
    // return new FinalSet<Theme>(DIRTYCONTEXTFACTS, CONTEXTSOURCES);
    if (isEnglish()) {
      return new FinalSet<Theme>(CONTEXTFACTS.inLanguage(language));
    } else {
      return new FinalSet<Theme>(CONTEXTFACTSNEEDSTRANSLATION.inLanguage(language));
    }
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    Set<FollowUpExtractor> result = new HashSet<FollowUpExtractor>();

    if (!isEnglish()) {
      result.add(new EntityTranslator(CONTEXTFACTSNEEDSTRANSLATION.inLanguage(language), CONTEXTFACTS.inLanguage(this.language), this));
    }
    return result;
  }

  @Override
  public void extract() throws Exception {
    // Extract the information
    Announce.doing("Extracting context facts");

    BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    TitleExtractor titleExtractor = new TitleExtractor(language);

    FactCollection contextPatternCollection = PatternHardExtractor.CONTEXTPATTERNS.factCollection();
    FactTemplateExtractor contextPatterns = new FactTemplateExtractor(contextPatternCollection, "<_extendedContextWikiPattern>");
    PatternList replacements = new PatternList(PatternHardExtractor.AIDACLEANINGPATTERNS, "<_aidaCleaning>");

    // FactWriter outSources = output.get(CONTEXTSOURCES);

    String titleEntity = null;
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>")) {
        case -1:
          Announce.done();
          in.close();
          return;
        case 0:
          titleEntity = titleExtractor.getTitleEntity(in);
          if (titleEntity == null) continue;

          String page = FileLines.readBetween(in, "<text", "</text>");
          String normalizedPage = Char17.decodeAmpersand(Char17.decodeAmpersand(page.replaceAll("[\\s\\x00-\\x1F]+", " ")));
          String transformedPage = replacements.transform(normalizedPage);

          // for (Pair<Fact, String> fact :
          // contextPatterns.extractWithProvenance(normalizedPage,
          // titleEntity)) {
          // if (fact.first != null)
          // write(out, fact.first, outSources,
          // FactComponent.wikipediaURL(titleEntity),
          // "ConteXtExtractor from: " + fact.second);
          // }
          for (Fact fact : contextPatterns.extract(transformedPage, titleEntity, language)) {
            if (fact != null) {
              if (isEnglish()) {
                CONTEXTFACTS.inLanguage(language).write(fact);
              } else {
                CONTEXTFACTSNEEDSTRANSLATION.inLanguage(language).write(fact);
              }
            }
          }
      }
    }
  }

  /**
   * Needs Wikipedia as input
   *
   * @param wikipedia
   *            Wikipedia XML dump
   */
  public ConteXtExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

}
