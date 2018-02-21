/*
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Johannes Hoffart.

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

package followUp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import extractors.Extractor;
import fromWikipedia.RedirectExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * Takes the input Themes and checks if any of the entities are actually a
 * redirect and resolves them
 * 
*/

public class Redirector extends FollowUpExtractor {

  protected String language;

  @Override
  public Set<Theme> input() {
    return new FinalSet<Theme>(checkMe, RedirectExtractor.REDIRECT_FACTS_DIRTY.inLanguage(this.language));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(checked);
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(RedirectExtractor.REDIRECT_FACTS_DIRTY.inLanguage(this.language));
  }

  @Override
  public void extract() throws Exception {
    // Extract the information
    Map<String, String> redirects = new HashMap<>();
    Announce.doing("Loading redirects");
    for (Fact f : RedirectExtractor.REDIRECT_FACTS_DIRTY.inLanguage(this.language)) {
      redirects.put(FactComponent.forYagoEntity(FactComponent.asJavaString(f.getArg(2)).replace(' ', '_')), f.getArg(1));
    }
    Announce.done();

    Announce.doing("Applying redirects to facts");
    for (Fact dirtyFact : checkMe) {
      Fact redirectedDirtyFact = redirectArguments(dirtyFact, redirects);
      checked.write(redirectedDirtyFact);
    }
    Announce.done();
  }

  protected Fact redirectArguments(Fact dirtyFact, Map<String, String> redirects) {
    String redirectedArg1 = dirtyFact.getArg(1);
    if (redirects.containsKey(dirtyFact.getArg(1))) {
      redirectedArg1 = redirects.get(dirtyFact.getArg(1));
    }

    String redirectedArg2 = dirtyFact.getArg(2);
    if (redirects.containsKey(dirtyFact.getArg(2))) {
      redirectedArg2 = redirects.get(dirtyFact.getArg(2));
    }

    Fact redirectedFact = new Fact(redirectedArg1, dirtyFact.getRelation(), redirectedArg2);
    redirectedFact.makeId();

    return redirectedFact;
  }

  public Redirector(Theme in, Theme out, Extractor parent) {
    super(in, out, parent);
    this.language = in.language();
    if (this.language == null) this.language = "en";
  }

}
