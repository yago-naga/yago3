/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Fabian M. Suchanek, with contributions from Farzaneh Mahdisoltani.

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.Fact.ImplementationNote;
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
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

/**
 * Extracts facts from Wikipedia categories.
 * 
*/

public class CategoryExtractor extends MultilingualWikipediaExtractor {

  public static final MultilingualTheme CATEGORYMEMBERS = new MultilingualTheme("categoryMembers",
      "Facts about Wikipedia instances, derived from the Wikipedia categories, still to be translated");

  public static final MultilingualTheme CATEGORYMEMBERS_TRANSLATED = new MultilingualTheme("categoryMembersTranslated",
      "Category Members facts with translated subjects and objects");

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new TreeSet<>();
    input.add(PatternHardExtractor.TITLEPATTERNS);
    input.add(DictionaryExtractor.CATEGORYWORDS);
    input.add(RedirectExtractor.REDIRECT_FACTS_DIRTY.inLanguage(language));
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;
  }

  @Override
  public Set<Theme> inputCached() {
    Set<Theme> input = new TreeSet<>();
    input.add(DictionaryExtractor.CATEGORYWORDS);
    if (!Extractor.includeConcepts) {
      input.add(WordnetExtractor.PREFMEANINGS);
    }
    return input;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYMEMBERS.inLanguage(language));
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    if (language.equals("en")) return (Collections.emptySet());
    return (new FinalSet<FollowUpExtractor>(
        new CategoryTranslator(CATEGORYMEMBERS.inLanguage(this.language), CATEGORYMEMBERS_TRANSLATED.inLanguage(this.language), this)));
  }

  @Override
  public void extract() throws Exception {
    TitleExtractor titleExtractor = new TitleExtractor(language);

    // Extract the information
    // Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(wikipedia);
    String titleEntity = null;
    
    // Create a set from all objects of relation "<redirectedFrom>", which are the redirect pages.
    // Since we do not want to add redirect entities to Yago entities, we need them to check against extracted entities.
    // The reason for using REDIRECTFACTSDIRTY instead of REDIRECTFACTS is that the former is created in a follow up 
    // extractor that is depending on the output of this class to remove non named entity words.
    Set<String>  redirects = new HashSet<>();
    Set<Fact> redirectFacts = RedirectExtractor.REDIRECT_FACTS_DIRTY.inLanguage(language).factCollection().getFactsWithRelation("<redirectedFrom>");
    for(Fact f:redirectFacts) {
      String entity = titleExtractor.createTitleEntity(FactComponent.stripQuotesAndLanguage(f.getObject()));
      redirects.add(entity);
    }
    
    /**
     * categoryWord holds the synonym of the word "Category" in different
     * languages. It is needed to distinguish the category part in Wiki
     * pages.
     */
    String categoryWord = DictionaryExtractor.CATEGORYWORDS.factCollection().getObject(FactComponent.forString(language), "<_hasCategoryWord>");
    if (categoryWord == null) throw new Exception("Category word undefined in language " + language);
    categoryWord = FactComponent.asJavaString(categoryWord);
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:", "[[" + categoryWord + ":", "<!--")) {
        case -1:
          // Announce.progressDone();
          in.close();
          return;
        case 0:
          // Announce.progressStep();
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1:
        case 2:
          if (titleEntity == null || redirects.contains(titleEntity)) {
            continue;
          }
          @ImplementationNote("All of these weird characters can erroneously appear in category names. We cut them away.")
          String category = FileLines.readTo(in, ']', '|', '{', '}').toString();
          category = category.trim();
          // There are sometimes categories of length 0
          // This causes problems, so avoid them
          if (category.length() >= 4 && !category.contains(":")) {
            CATEGORYMEMBERS.inLanguage(language)
                .write(new Fact(titleEntity, "<hasWikipediaCategory>", FactComponent.forForeignWikiCategory(category, language)));
          }
          break;
        case 3:
          FileLines.findIgnoreCase(in, "-->");
          break;
      }
    }
  }

  public CategoryExtractor(String lang, File wikipedia) {
    super(lang, wikipedia);
  }

  public static void main(String[] args) throws Exception {

    new CategoryExtractor("de", new File("C:/Fabian/data/wikipedia/wikitest_de.xml")).extract(new File("c:/fabian/data/yago3"),
        "Test on 1 wikipedia article");

  }

}
