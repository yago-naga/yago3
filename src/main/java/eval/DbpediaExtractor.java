package eval;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import extractors.DataExtractor;
import javatools.datatypes.FinalSet;
import utils.Theme;

/** Produces YAGO facts from DBpedia, without mapping the predicates

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

public class DbpediaExtractor extends DataExtractor {

  public DbpediaExtractor(File input) {
    super(input);
  }

  @Override
  public Set<Theme> input() {
    return (Collections.emptySet());
  }

  public static final Theme DBPEDIAFACTS = new Theme("dbpediaFacts", "Facts of http://dbpedia.org, in YAGO format");

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(DBPEDIAFACTS);
  }

  /**
   * Translates instances 1:1 into the YAGO namespace, cleans literal
   * datatypes
   */
  public static String makeYago(String dbpedia) {
    if (FactComponent.isLiteral(dbpedia)) return (dbpedia);
    return ("<" + FactComponent.stripPrefix(dbpedia) + ">");
  }

  @Override
  public void extract() throws Exception {
    for (Fact f : FactSource.from(inputData)) {
      DBPEDIAFACTS.write(new Fact(makeYago(f.getSubject()), f.getRelation(), makeYago(f.getObject())));
    }
  }

  public static void main(String[] args) throws Exception {
    new DbpediaExtractor(new File("c:/fabian/data/dbpedia/mappingbased_properties_cleaned_en.ttl")).extract(new File("c:/fabian/data/dbpedia/"),
        "blah");
  }
}
