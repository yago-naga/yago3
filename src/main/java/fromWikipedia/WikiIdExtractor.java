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
import basics.FactComponent;
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
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

/**
 * Extracts all Wikipedia IDs. It writes facts of the kind
 *
 * entity <hasWikipediaId> "en/123"
 * entity <hasWikipediaId> "de/234"
 * 
*/
public class WikiIdExtractor extends MultilingualWikipediaExtractor {

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new TreeSet<Theme>(Arrays.asList(
        PatternHardExtractor.TITLEPATTERNS, 
        PatternHardExtractor.LANGUAGECODEMAPPING));
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;
  }

  public static final MultilingualTheme WIKIPEDIAIDFACTSNEEDSTRANSLATION = new MultilingualTheme("wikipediaIdNeedsTranslation",
      "Extracted IDs from Wikipedia articles pages (to be translated)");

  public static final MultilingualTheme WIKIPEDIAIDFACTS = new MultilingualTheme("yagoWikipediaIds", "Extracted IDs from Wikipedia articles pages",
      Theme.ThemeGroup.OTHER);

  @Override
  public Set<FollowUpExtractor> followUp() {
    HashSet<FollowUpExtractor> s = new HashSet<>();
    if (!isEnglish()) {
      s.add(new EntityTranslator(WIKIPEDIAIDFACTSNEEDSTRANSLATION.inLanguage(language), WIKIPEDIAIDFACTS.inLanguage(language), this));
    }
    return s;
  }

  @Override
  public Set<Theme> output() {
    if (isEnglish()) {
      return new FinalSet<Theme>(WIKIPEDIAIDFACTS.inLanguage(language));
    } else {
      return new FinalSet<Theme>(WIKIPEDIAIDFACTSNEEDSTRANSLATION.inLanguage(language));
    }
  }

  @Override
  public void extract() throws Exception {
    Announce.doing("Extracting Wikipedia IDs");

    BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    TitleExtractor titleExtractor = new TitleExtractor(language);

    String titleEntity = null;
    redirect: while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "<id>")) {
        case -1:
          Announce.done();
          in.close();
          break redirect;
        case 0:
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1:
          if (titleEntity == null) continue;
          Integer id = Integer.parseInt(FileLines.readToBoundary(in, "</id>").toString());
          String langId = language + "/" + id;
          Fact f = new Fact(titleEntity, "<hasWikipediaId>", FactComponent.forString(langId));
          if (isEnglish()) {
            WIKIPEDIAIDFACTS.inLanguage(language).write(f);
          } else {
            WIKIPEDIAIDFACTSNEEDSTRANSLATION.inLanguage(language).write(f);
          }

          // Make sure to take only the first id encountered after the title. Other ids might be revisions etc.
          titleEntity = null;
      }
    }

    Announce.done();
  }

  public WikiIdExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new WikiIdExtractor("en", new File("D:/en_wikitest.xml")).extract(new File("D:/data3/yago2s"), "Test on 1 wikipedia article");
  }
}
