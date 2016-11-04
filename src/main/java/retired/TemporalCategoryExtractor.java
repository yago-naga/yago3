package retired;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import basics.Fact;
import extractors.EnglishWikipediaExtractor;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.FactTemplateExtractor;
import utils.Theme;
import utils.TitleExtractor;

/**
 * Extract temporal facts from categories. It uses the patterns
 * /data/_categoryTemporalPatterns.ttl for the extraction.
 * 
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Erdal Kuzey.

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
public class TemporalCategoryExtractor extends EnglishWikipediaExtractor {

  @Override
  public Set<FollowUpExtractor> followUp() {
    return new FinalSet<FollowUpExtractor>(new TypeChecker(DIRTYCATEGORYFACTS, TEMPORALCATEGORYFACTS, this));
  }

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(
        //PatternHardExtractor.TEMPORALCATEGORYPATTERNS,
        PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS, HardExtractor.HARDWIREDFACTS));
  }

  /** Facts deduced from categories */
  public static final Theme DIRTYCATEGORYFACTS = new Theme("categoryTemporalFactsDirty",
      "Temporal facts derived from the categories - still to be type checked");

  /** Facts deduced from categories */
  public static final Theme TEMPORALCATEGORYFACTS = new Theme("categoryTemporalFacts", "Temporal facts derived from the categories");

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(DIRTYCATEGORYFACTS);
  }

  @Override
  public void extract() throws IOException {
    FactCollection categoryPatternCollection = null;
    //				PatternHardExtractor.TEMPORALCATEGORYPATTERNS	.factCollection();
    FactTemplateExtractor categoryPatterns = new FactTemplateExtractor(categoryPatternCollection, "<_categoryPattern>");
    TitleExtractor titleExtractor = new TitleExtractor("en");
    // Announce.progressStart("Extracting", 3_900_000);
    Reader in = FileUtils.getBufferedUTF8Reader(inputData);
    String titleEntity = null;
    FactCollection facts = new FactCollection();
    while (true) {
      switch (FileLines.findIgnoreCase(in, "<title>", "[[Category:")) {
        case -1:
          flush(titleEntity, facts);
          // Announce.progressDone();
          in.close();
          return;
        case 0:
          // Announce.progressStep();
          flush(titleEntity, facts);
          titleEntity = titleExtractor.getTitleEntity(in);
          break;
        case 1:
          if (titleEntity == null) continue;
          String category = FileLines.readTo(in, "]]").toString();
          if (!category.endsWith("]]")) continue;
          category = category.substring(0, category.length() - 2);
          for (Fact fact : categoryPatterns.extract(category, titleEntity)) {
            if (fact != null) facts.add(fact);
          }
      }
    }

  }

  /** Writes the facts */
  public static void flush(String entity, FactCollection facts) throws IOException {
    if (entity == null) return;

    for (Fact fact : facts) {
      DIRTYCATEGORYFACTS.write(fact);
    }
    facts.clear();
  }

  public TemporalCategoryExtractor(File wikipedia) {
    super(wikipedia);
  }
}
