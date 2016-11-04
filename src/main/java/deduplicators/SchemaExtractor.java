package deduplicators;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import fromOtherSources.HardExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * YAGO2s - SchemaExtractor
 * 
 * Deduplicates all schema facts (except for the multilingual ones). This
 * extractor is different from FactExtractor so that it can run in parallel.
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

public class SchemaExtractor extends SimpleDeduplicator {

  @Override
  public List<Theme> inputOrdered() {
    return Arrays.asList(HardExtractor.HARDWIREDFACTS);
  }

  /** All facts of YAGO */
  public static final Theme YAGOSCHEMA = new Theme("yagoSchema", "The domains, ranges and confidence values of YAGO relations", ThemeGroup.TAXONOMY);

  /** Relations that we care for */
  public static Set<String> relations = new FinalSet<>(RDFS.domain, RDFS.range, RDFS.subpropertyOf, YAGO.hasConfidence);

  @Override
  public Theme myOutput() {
    return YAGOSCHEMA;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    boolean isDesiredRelation = relations.contains(fact.getRelation());
    boolean isTypeRelation = fact.getRelation().equals(RDFS.type);
    boolean hasRightTypeArguments = fact.getArg(1).matches(".*Property.*|.*Relation.*") || fact.getArg(2).matches(".*Property.*|.*Relation.*");

    return isDesiredRelation || (isTypeRelation && hasRightTypeArguments);
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new SchemaExtractor().extract(new File("c:/fabian/data/yago3"), "test");
  }

}
