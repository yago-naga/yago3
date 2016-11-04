package retired.export;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromThemes.InfoboxMapper;
import fromThemes.TransitiveTypeExtractor;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * Extracts children for Berkeley project
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
public class BerkeleyChildExtractor extends Extractor {

  public static final Theme CHILDREN = new Theme("children", "Children of people, not type checked on the object");

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<>();
    input.add(TransitiveTypeExtractor.TRANSITIVETYPE);
    input.addAll(InfoboxMapper.INFOBOXFACTS_TOTYPECHECK.inLanguages(MultilingualExtractor.wikipediaLanguages));
    return input;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(CHILDREN);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(TransitiveTypeExtractor.TRANSITIVETYPE);
  }

  @Override
  public void extract() throws Exception {
    Map<String, Set<String>> children = new HashMap<>();
    for (Theme inputTheme : InfoboxMapper.INFOBOXFACTS_TOTYPECHECK.inLanguages(MultilingualExtractor.wikipediaLanguages)) {
      for (Fact f : inputTheme) {
        if (!f.getRelation().equals("<hasChild>")) continue;
        String parent = f.getSubject();
        if (FactComponent.isLiteral(parent)) {
          if (!parent.matches("[\\p{IsAlphabetic}\\. ]{4,50}")) continue;
          parent = FactComponent.forWikipediaTitle(FactComponent.asJavaString(parent));
        }
        String child = f.getObject();
        if (FactComponent.isLiteral(child)) {
          if (!child.matches("[\\p{IsAlphabetic}\\. ]{4,50}")) continue;
          child = FactComponent.forWikipediaTitle(FactComponent.asJavaString(child));
        }
        D.addKeyValue(children, parent, child, HashSet.class);
      }
    }
    for (String parent : children.keySet()) {
      for (String child : children.get(parent)) {
        CHILDREN.write(new Fact(parent, "<hasChild>", child));
      }
    }
  }

  public static void main(String[] args) throws Exception {
    new BerkeleyChildExtractor().extract(new File("c:/fabian/data/yago3"), "extracting children");
  }

}
