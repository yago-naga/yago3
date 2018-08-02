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

import extractors.MultilingualExtractor;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromWikipedia.CategoryExtractor;
import utils.MultilingualTheme;
import utils.Theme;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Cleans the categories extracted by {@link CategoryExtractor} to only keep the relevant entities.
 * The output can be used as contextual evidence for the respective entities.
 * 
*/
public class CategoryConteXtExtractor extends MultilingualExtractor {

  public static final MultilingualTheme CATEGORY_CONTEXT = new MultilingualTheme("categoryConteXt",
          "Cleaned entity categories extracted from Wikipedia.");

  public static final MultilingualTheme CATEGORY_CONTEXT_ENTITIES_TRANSLATED = new MultilingualTheme("categoryConteXtEntitiesTranslated",
          "Cleaned entity categories extracted from Wikipedia with translated subjects");

  @Override
  public Set<Theme> input() {
    Set<Theme> result = new TreeSet<>();
    if (isEnglish()) {
      result.add(CategoryExtractor.CATEGORYMEMBERS.inLanguage(language));
    } else {
      result.add(CategoryExtractor.CATEGORYMEMBERS_ENTITIES_TRANSLATED.inLanguage(language));
    }
    return result;
  }
  @Override
  public Set<Theme> output() {
    Set<Theme> result = new TreeSet<>();
    if (isEnglish()) {
      result.add(CATEGORY_CONTEXT.inEnglish());
    } else {
      result.add(CATEGORY_CONTEXT_ENTITIES_TRANSLATED.inLanguage(this.language));
    }
    return result;
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    Set<FollowUpExtractor> followUps = new HashSet<>();
    if (isEnglish()) {
      followUps.add(new TypeChecker(CATEGORY_CONTEXT.inEnglish(), CATEGORY_CONTEXT.inEnglish()));
    } else {
      followUps.add(new TypeChecker(
              CategoryExtractor.CATEGORYMEMBERS_ENTITIES_TRANSLATED.inLanguage(this.language),
              CATEGORY_CONTEXT_ENTITIES_TRANSLATED.inLanguage(this.language)));
    }
    return followUps;
  }

  @Override
  public void extract() throws Exception {
    // Nothing to do here, the sole purpose of this class is to type-check.
  }

  public CategoryConteXtExtractor(String lang) {
    super(lang);
  }
}
