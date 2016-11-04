package fromThemes;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import extractors.MultilingualExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.MultilingualTheme;
import utils.Theme;

/**
 * Extracts types from category membership facts.
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
public class CategoryTypeExtractor extends MultilingualExtractor {

  /** Sources for category facts */
  public static final MultilingualTheme CATEGORYTYPESOURCES = new MultilingualTheme("categoryTypeSources",
      "Sources for the classes derived from the Wikipedia categories, with their connection to the WordNet class hierarchy leaves");

  /** Types deduced from categories */
  public static final MultilingualTheme CATEGORYTYPES = new MultilingualTheme("categoryTypes",
      "The rdf:type facts of YAGO derived from the categories");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<Theme>(Arrays.asList(CategoryClassExtractor.CATEGORYCLASSES));
    if (isEnglish()) result.add(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language));
    else result.add(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language));
    return result;
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(CategoryClassExtractor.CATEGORYCLASSES);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CATEGORYTYPESOURCES.inLanguage(language), CATEGORYTYPES.inLanguage(language));
  }

  @Override
  public void extract() throws Exception {
    Set<String> validClasses = CategoryClassExtractor.CATEGORYCLASSES.factCollection().getSubjects();

    FactSource categoryMembs;
    if (isEnglish()) categoryMembs = CategoryExtractor.CATEGORYMEMBERS.inLanguage(language);
    else categoryMembs = CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(language);

    // Extract the information
    for (Fact f : categoryMembs) {
      if (!f.getRelation().equals("<hasWikipediaCategory>")) continue;
      String category = f.getObject();
      if (!validClasses.contains(category)) continue;
      write(CATEGORYTYPES.inLanguage(language), new Fact(f.getSubject(), "rdf:type", category), CATEGORYTYPESOURCES.inLanguage(language),
          FactComponent.wikipediaSourceURL(f.getSubject(), language), "By membership in conceptual category");
    }
    Announce.done();
  }

  public CategoryTypeExtractor(String lang) {
    super(lang);
  }

  public static void main(String[] args) throws Exception {
    new CategoryTypeExtractor("en").extract(new File("c:/fabian/data/yago3"), "Test");
  }

}
