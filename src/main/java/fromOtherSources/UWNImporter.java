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

package fromOtherSources;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import extractors.DataExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import utils.Theme;
import utils.Theme.ThemeGroup;

/**
 * Imports the multi-lingual class labels from Gerard de Melo's Universal
 * WordNet (UWN)
 * 
 * Needs the uwn-nouns.tsv as input, with the format
 * three_letter_language_code\tnew_label
 * \trel:means\twordnet_synset_id\tconfidence
 * 
*/
public class UWNImporter extends DataExtractor {

  /** multi-lingual class names */
  public static final Theme UWNDATA = new Theme("yagoMultilingualClassLabels", "Multi-lingual labels for classes from Universal WordNet",
      ThemeGroup.MULTILINGUAL);

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETIDS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(UWNDATA);
  }

  @Override
  public Set<Theme> inputCached() {
    return new HashSet<Theme>(Arrays.asList(HardExtractor.HARDWIREDFACTS, WordnetExtractor.WORDNETIDS));
  }

  @Override
  public void extract() throws Exception {
    // get wordnet synset id mapping
    Map<String, String> wnssm = WordnetExtractor.WORDNETIDS.factCollection().getReverseMap("<hasSynsetId>");
    Map<String, String> tlc2language = HardExtractor.HARDWIREDFACTS.factCollection().getReverseMap("<hasThreeLetterLanguageCode>");

    for (String line : new FileLines(inputData, "UTF-8", "Importing UWN mappings")) {
      String data[] = line.split("\t");

      String lang = tlc2language.get(FactComponent.forString(data[0]));
      String name = FactComponent.forStringWithLanguage(data[1], data[0]);
      String wordnetSynset = wnssm.get(FactComponent.forString(data[3]));

      if (wordnetSynset == null) {
        Announce.debug("No WordNet Synset for id " + data[3]);
        continue;
      }

      if (lang == null) {
        Announce.debug("No Wikipedia language for " + data[3]);
        continue;
      }

      UWNDATA.write(new Fact(wordnetSynset, RDFS.label, name));
    }
  }

  public UWNImporter(File uwnNouns) {
    super(uwnNouns);
  }

  public UWNImporter() {
    this(new File("./data/uwn4yago/uwn-nouns.tsv"));
  }
}
