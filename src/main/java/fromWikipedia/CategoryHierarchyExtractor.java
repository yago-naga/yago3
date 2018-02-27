/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Felix Keller, with contributions from Johannes Hoffart.

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
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import extractors.MultilingualWikipediaExtractor;
import followUp.CategoryTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.DictionaryExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileUtils;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * CategoryExtractor - YAGO2s
 * 
 * Extracts the Wikipedia category hierarchy.
 * 
*/

public class CategoryHierarchyExtractor extends MultilingualWikipediaExtractor {

  public static final MultilingualTheme CATEGORYHIERARCHY = new MultilingualTheme("categoryHierarchy",
      "Wikipedia category hierarchy, still to be translated");

  public static final MultilingualTheme CATEGORYHIERARCHY_TRANSLATED = new MultilingualTheme("categoryHierarchyTranslated",
      "Wikipedia category hierarchy translated subjects and objects");

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new TreeSet<>();
    input.add(PatternHardExtractor.TITLEPATTERNS);
    input.add(DictionaryExtractor.CATEGORYWORDS);
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(DictionaryExtractor.CATEGORYWORDS);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYHIERARCHY.inLanguage(language));
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    if (language.equals("en")) return (Collections.emptySet());
    return (new FinalSet<FollowUpExtractor>(
        new CategoryTranslator(CATEGORYHIERARCHY.inLanguage(this.language), CATEGORYHIERARCHY_TRANSLATED.inLanguage(this.language), this, true, false)));
  }

  @Override
  public void extract() throws Exception {
    // Extract the information
    // Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String pageCategory = null;
    /**
     * categoryWord holds the synonym of the word "Category" in different
     * languages. It is needed to distinguish the category part in Wiki
     * pages.
     */
    String categoryWord = DictionaryExtractor.CATEGORYWORDS.factCollection().getObject(FactComponent.forString(language), "<_hasCategoryWord>");
    if (categoryWord == null) throw new Exception("Category word undefined in language " + language);
    categoryWord = FactComponent.asJavaString(categoryWord);
    String categoryTitlePrefix = categoryWord + ":";
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:", "[[" + categoryWord + ":")) {
        case -1:
          // Announce.progressDone();
          in.close();
          return;
        case 0:
          // Announce.progressStep();
          String title = FileLines.readToBoundary(in, "</title>");
          title = Char17.decodeAmpersand(title);
          if (title.startsWith("Category:")) {
            pageCategory = title.substring("Category:".length());
          } else if (title.startsWith(categoryTitlePrefix)) {
            pageCategory = title.substring(categoryTitlePrefix.length());
          } else {
            pageCategory = null;
          }
          break;
        case 1:
        case 2:
          if (pageCategory == null) {
            continue;
          }
          String category = FileLines.readTo(in, ']', '|').toString();
          category = category.trim();
          // There are sometimes categories of length 0
          // This causes problems, so avoid them
          if (category.length() >= 4 && !category.contains(":")) {
            CATEGORYHIERARCHY.inLanguage(language).write(new Fact(FactComponent.forForeignWikiCategory(pageCategory, language),
                "<wikipediaSubCategoryOf>", FactComponent.forForeignWikiCategory(category, language)));
          }
          break;
      }
    }
  }

  public CategoryHierarchyExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

  public static void main(String[] args) throws Exception {

    new CategoryHierarchyExtractor("en", new File("c:/fabian/data/wikipedia/roxana.xml")).extract(new File("data"), "Test on 1 wikipedia article");

  }

}
