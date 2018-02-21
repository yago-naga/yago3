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

package extractors;

import java.io.File;

/**
 * Superclass of all extractors that extract from the English Wikipedia
 * 
*/

public abstract class EnglishWikipediaExtractor extends DataExtractor {

  protected File wikipedia() {
    return (inputData);
  }

  public EnglishWikipediaExtractor(File wikipedia) {
    super(wikipedia);
  }
}
