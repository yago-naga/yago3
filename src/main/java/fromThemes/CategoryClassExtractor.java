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

package fromThemes;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.parsers.Name;
import javatools.parsers.NounGroup;
import javatools.parsers.PlingStemmer;
import utils.Theme;

/**
 * Extracts classes from the English category membership facts.
 *
*/
public class CategoryClassExtractor extends Extractor {

  /** Classes deduced from categories with their connection to WordNet */
  public static final Theme CATEGORYCLASSES = new Theme("categoryClasses",
      "Classes derived from the Wikipedia categories, with their connection to the WordNet class hierarchy leaves");

  @Override
  public Set<Theme> input() {
    HashSet<Theme> input = new HashSet<>();
    input.add(PatternHardExtractor.CATEGORYPATTERNS);
    input.add(WordnetExtractor.PREFMEANINGS);
    input.add(CategoryExtractor.CATEGORYMEMBERS.inEnglish());
    input.addAll(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    return input;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYCLASSES);
  }

  /** Holds the nonconceptual categories */
  protected Set<String> nonConceptualCategories = new HashSet<>();

  /** Holds the preferred meanings */
  protected Map<String, String> preferredMeanings;

  /** Maps a category to a wordnet class */
  public String category2class(String categoryName) {
    categoryName = FactComponent.stripCat(categoryName);
    // Check out whether the new category is worth being added
    NounGroup category = new NounGroup(categoryName);
    if (category.head() == null) {
      Announce.debug("Could not find type in", categoryName, "(has empty head)");
      return (null);
    }

    // If the category is an acronym, drop it
    if (Name.isAbbreviation(category.head())) {
      Announce.debug("Could not find type in", categoryName, "(is abbreviation)");
      return (null);
    }
    category = new NounGroup(categoryName.toLowerCase());

    // Only plural words are good hypernyms
    if (PlingStemmer.isSingular(category.head()) && !category.head().equals("people")) {
      Announce.debug("Could not find type in", categoryName, "(is singular)");
      return (null);
    }

    String stemmedHead = PlingStemmer.stem(category.head());

    // Exclude the bad guys
    if (nonConceptualCategories.contains(stemmedHead)) {
      Announce.debug("Could not find type in", categoryName, "(is non-conceptual)");
      return (null);
    }

    // Try all premodifiers (reducing the length in each step) + head
    if (category.preModifier() != null) {
      String wordnet = null;
      String preModifier = category.preModifier().replace('_', ' ');

      for (int start = 0; start != -1 && start < preModifier.length() - 2; start = preModifier.indexOf(' ', start + 1)) {
        wordnet = preferredMeanings.get((start == 0 ? preModifier : preModifier.substring(start + 1)) + " " + stemmedHead);
        // take the longest matching sequence
        if (wordnet != null) return (wordnet);
      }
    }

    // Try postmodifiers to catch "head of state"
    if (category.postModifier() != null && category.preposition() != null && category.preposition().equals("of")) {
      String wordnet = preferredMeanings.get(stemmedHead + " of " + category.postModifier().head());
      if (wordnet != null) return (wordnet);
    }

    // Try head
    String wordnet = preferredMeanings.get(stemmedHead);
    if (wordnet != null) return (wordnet);
    Announce.debug("Could not find type in", categoryName, "(" + stemmedHead + ") (no wordnet match)");
    return (null);
  }

  /**
   * Extracts the statement subclassOf(category name, wordnetclass)
   *
   * @param classWriter
   */
  protected void extractClassStatement(String categoryEntity) throws IOException {
    String concept = category2class(categoryEntity);
    if (concept == null) return;
    CATEGORYCLASSES.write(new Fact(categoryEntity, RDFS.subclassOf, concept));
    String name = new NounGroup(FactComponent.stripCat(categoryEntity)).stemmed().replace('_', ' ');
    if (!name.isEmpty()) CATEGORYCLASSES.write(new Fact(null, categoryEntity, RDFS.label, FactComponent.forStringWithLanguage(name, "eng")));

    if (categoryEntity.startsWith("<wikicat_Fictional_")) { // added for wikidata project
      CATEGORYCLASSES.write(new Fact(categoryEntity, RDFS.subclassOf, "<wordnet_imaginary_being_109483738>"));
    }
  }

  @Override
  public void extract() throws Exception {
    nonConceptualCategories = PatternHardExtractor.CATEGORYPATTERNS.factCollection().seekStringsOfType("<_yagoNonConceptualWord>");
    preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection().getPreferredMeanings();

    // Holds the categories we already did
    Set<String> categoriesDone = new HashSet<>();

    // Extract the information
    for (Theme t : input()) {
      for (Fact f : t) {
        if (!f.getRelation().equals("<hasWikipediaCategory>")) continue;
        String category = f.getObject();
        if (categoriesDone.contains(category)) continue;
        categoriesDone.add(category);
        extractClassStatement(category);
      }
    }
    this.nonConceptualCategories = null;
    this.preferredMeanings = null;
  }

  public static void main(String[] args) throws Exception {
    new CategoryClassExtractor().extract(new File("c:/fabian/data/yago3"), "Test");
  }

}
