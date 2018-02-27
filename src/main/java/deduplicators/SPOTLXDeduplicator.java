/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Joanna Asia Biega.

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

package deduplicators;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import basics.Fact;
import fromThemes.SPOTLXDeductiveExtractor;
import javatools.administrative.Announce;
import utils.Theme;

/**
 * YAGO2s - SPOTLXDeduplicator
 * 
 * An clean-up extractor for the SPOTLX deduction process. It produces the final
 * results of SPOTLX deduction, filtering only occurs[In, Since, Until]
 * relations and removing the duplicates.
 * 
*/

public class SPOTLXDeduplicator extends SimpleDeduplicator {

  @Override
  public List<Theme> inputOrdered() {
    return Arrays.asList(SchemaExtractor.YAGOSCHEMA, SPOTLXDeductiveExtractor.RULERESULTS);
  }

  public static final Theme SPOTLXFACTS = new Theme("spotlxFacts", "SPOTLX deduced facts");

  public static final List<String> SPOTLX_FINAL_RELATIONS = new ArrayList<String>(Arrays.asList("<occursIn>", "<occursSince>", "<occursUntil>"));

  @Override
  public Theme myOutput() {
    return SPOTLXFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    return SPOTLX_FINAL_RELATIONS.contains(fact.getRelation());
  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new SPOTLXDeduplicator().extract(new File("/home/jbiega/data/yago2s"), "test");
    // new SPOTLXDeduplicator().extract(new File("/local/jbiega/yagofacts"),
    // "test");
  }
}
