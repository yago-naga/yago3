package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import basics.Fact;
import basics.RDFS;
import fromOtherSources.HardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.CategoryClassExtractor;
import fromThemes.CategoryClassHierarchyExtractor;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - ClassExtractor
 * 
 * Deduplicates all type subclass facts and puts them into the right themes.
 * 
 * This is different from the FactExtractor, because its output is useful for
 * many extractors that deliver input for the FactExtractor.
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
public class ClassExtractor extends SimpleDeduplicator {

  @Override
  public List<Theme> inputOrdered() {
    return (Arrays.asList(SchemaExtractor.YAGOSCHEMA, HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETCLASSES,
        CategoryClassExtractor.CATEGORYCLASSES, CategoryClassHierarchyExtractor.CATEGORYCLASSHIERARCHY.inEnglish()
    // GeoNamesClassMapper.GEONAMESCLASSES
    ));
  }

  /** The YAGO taxonomy */
  public static final Theme YAGOTAXONOMY = new Theme("yagoTaxonomy",
      "The entire YAGO taxonomy. These are all rdfs:subClassOf facts derived from multilingual Wikipedia and from WordNet", ThemeGroup.TAXONOMY);

  @Override
  public Theme myOutput() {
    return YAGOTAXONOMY;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    return fact.getRelation().equals(RDFS.subclassOf);
  }

  public static void main(String[] args) throws Exception {
    new ClassExtractor().extract(new File("c:/fabian/data/yago2s"), "test");
  }
}
