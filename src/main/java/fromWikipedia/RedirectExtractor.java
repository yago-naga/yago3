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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * Extracts all redirects from Wikipedia
 * 
*/
public class RedirectExtractor extends MultilingualWikipediaExtractor {

  @Override
  public Set<Theme> input() {
    return new FinalSet<Theme>(PatternHardExtractor.LANGUAGECODEMAPPING);
  }

  private static final Pattern pattern = Pattern.compile("\\[\\[([^#\\]]*?)\\]\\]");

  public static final MultilingualTheme REDIRECT_FACTS_DIRTY = new MultilingualTheme("redirectLabelsNeedsTranslationTypeChecking",
      "Redirect facts from Wikipedia redirect pages (to be type checked and translated)");

  public static final MultilingualTheme TRANSLATEDREDIRECTFACTSDIRTY = new MultilingualTheme("redirectLabelsNeedsTypeChecking",
      "Redirect facts from Wikipedia redirect pages (to be type checked)");

  public static final MultilingualTheme REDIRECTFACTS = new MultilingualTheme("yagoRedirectLabels", "Labels from Wikipedia redirect pages");

  @Override
  public Set<FollowUpExtractor> followUp() {
    HashSet<FollowUpExtractor> s = new HashSet<>();
    if (isEnglish()) {
      s.add(new TypeChecker(REDIRECT_FACTS_DIRTY.inLanguage(language), REDIRECTFACTS.inLanguage(language)));
    } else {
      s.add(new EntityTranslator(REDIRECT_FACTS_DIRTY.inLanguage(language), TRANSLATEDREDIRECTFACTSDIRTY.inLanguage(language), this));
      s.add(new TypeChecker(TRANSLATEDREDIRECTFACTSDIRTY.inLanguage(language), REDIRECTFACTS.inLanguage(language)));
    }
    return s;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(REDIRECT_FACTS_DIRTY.inLanguage(this.language));
  }

  @Override
  public void extract() throws Exception {
    // Extract the information
    Announce.doing("Extracting Redirects");
    Map<String, String> redirects = new HashMap<>();

    Map<String, String> languagemap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");

    BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);

    String titleEntity = null;
    redirect: while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "<redirect")) {
        case -1:
          Announce.done();
          in.close();
          break redirect;
        case 0:
          titleEntity = Char17.decodeAmpersand(FileLines.readToBoundary(in, "</title>"));
          break;
        default:
          if (titleEntity == null) continue;
          FileLines.readTo(in, "<text");
          String redirectText = FileLines.readTo(in, "</text>").toString().trim();
          String redirectTarget = getRedirectTarget(redirectText);

          if (redirectTarget != null) {
            redirects.put(titleEntity, redirectTarget);
          }
      }
    }

    Theme out = REDIRECT_FACTS_DIRTY.inLanguage(this.language);

    for (Entry<String, String> redirect : redirects.entrySet()) {
      out.write(new Fact(FactComponent.forForeignYagoEntity(redirect.getValue(), this.language), "<redirectedFrom>",
          FactComponent.forStringWithLanguage(redirect.getKey(), this.language, languagemap)));
    }

    Announce.done();
  }

  private String getRedirectTarget(String redirect) {
    Matcher m = pattern.matcher(redirect);

    if (m.find()) {
      String entity = m.group(1);
      entity = entity.substring(0, 1).toUpperCase() + (entity.length() > 1 ? entity.substring(1, entity.length()):"");
      return entity;
    } else {
      return null;
    }
  }

  public RedirectExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new RedirectExtractor("en", new File("D:/en_wikitest.xml")).extract(new File("D:/data3/yago2s"), "Test on 1 wikipedia article");
  }
}
