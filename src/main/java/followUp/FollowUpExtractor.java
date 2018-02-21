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

package followUp;

import java.util.Set;

import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import utils.Theme;

/** A Dummy class to indicate extractors that are called en suite 

*/

public abstract class FollowUpExtractor extends Extractor {

  /** This is the theme we want to check */
  protected final Theme checkMe;

  /** This is the theme we produce */
  protected final Theme checked;

  /** Points to whoever created us */
  protected final Extractor parent;

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(checked);
  }

  @Override
  public String name() {
    if (parent != null) {
      return String.format("%s:%s", super.name(), parent.name());
    } else {
      return super.name();
    }
  }

  protected FollowUpExtractor(Theme in, Theme out, Extractor parent) {
    checkMe = in;
    checked = out;
    this.parent = parent;
  }

  /** Creates an extractor given by name */
  public static FollowUpExtractor forName(Class<FollowUpExtractor> className, Theme in, Theme out) {
    Announce.doing("Creating extractor", className + "(" + in + ", " + out + ")");
    FollowUpExtractor extractor = null;
    try {
      extractor = className.getConstructor(Theme.class, Theme.class, Extractor.class).newInstance(in, out, null);

    } catch (Exception ex) {
      Announce.error(ex);
    }
    Announce.done();
    return (extractor);
  }

}
