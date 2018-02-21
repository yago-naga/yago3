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

package fromThemes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.MultilingualExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import fromOtherSources.HardExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.InfoboxExtractor;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.PatternList;
import utils.Theme;
import utils.termParsers.TermParser;

/**
 * Extracts the terms from the Infobox templates.
 *
*/
public class InfoboxTermExtractor extends MultilingualExtractor {

  public static final MultilingualTheme INFOBOXTERMS = new MultilingualTheme("infoboxTerms",
      "The attribute facts of the Wikipedia infoboxes, split into terms");

  public static final MultilingualTheme INFOBOXTERMS_TOREDIRECT = new MultilingualTheme("infoboxTermsToBeRedirected",
      "The attribute facts of the Wikipedia infoboxes, split into terms, still to be redirected.");

  public static final MultilingualTheme INFOBOXTERMSTRANSLATED = new MultilingualTheme("infoboxTermsTranslated",
      "The attribute facts of the Wikipedia infoboxes, split into terms, redirected, subject translated");

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.INFOBOXPATTERNS, PatternHardExtractor.INFOBOXREPLACEMENTS,
        WordnetExtractor.PREFMEANINGS, HardExtractor.HARDWIREDFACTS, InfoboxExtractor.INFOBOX_ATTRIBUTES.inLanguage(this.language),
        PatternHardExtractor.DATEPARSER, PatternHardExtractor.STRINGPARSER, PatternHardExtractor.NUMBERPARSER));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(INFOBOXTERMS_TOREDIRECT.inLanguage(this.language));
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<>(PatternHardExtractor.INFOBOXPATTERNS, PatternHardExtractor.INFOBOXREPLACEMENTS, WordnetExtractor.PREFMEANINGS,
        PatternHardExtractor.DATEPARSER, PatternHardExtractor.STRINGPARSER, PatternHardExtractor.NUMBERPARSER);
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    Set<FollowUpExtractor> result = new HashSet<FollowUpExtractor>();
    result.add(new Redirector(INFOBOXTERMS_TOREDIRECT.inLanguage(this.language), INFOBOXTERMS.inLanguage(this.language), this));
    if (!isEnglish()) result.add(new EntityTranslator(INFOBOXTERMS.inLanguage(language), INFOBOXTERMSTRANSLATED.inLanguage(this.language), this));
    return (result);
  }

  @Override
  public void extract() throws Exception {
    PatternList replacements = new PatternList(PatternHardExtractor.INFOBOXREPLACEMENTS, "<_infoboxReplace>");
    Map<String, String> unitDictionary = PatternHardExtractor.INFOBOXPATTERNS.factCollection().getMap("<_hasPredefinedUnit>");
    Map<String, String> preferredMeanings = WordnetExtractor.PREFMEANINGS.factCollection().getPreferredMeanings();

    List<TermParser> parsers = TermParser.allParsers(preferredMeanings, language);

    for (Fact f : InfoboxExtractor.INFOBOX_ATTRIBUTES.inLanguage(this.language)) {
      String val = f.getObjectAsJavaString();
      val = Char17.decodeAmpersand(val);
      // Sometimes we get empty values here
      if (val == null || val.isEmpty()) continue;
      val = replacements.transform(val);
      val = val.replace("$0", FactComponent.stripBrackets(f.getSubject()));
      val = val.trim();
      if (val.length() == 0) continue;
      @Fact.ImplementationNote("This has to be a list, because the first date mentioned is usually the right one")
      List<String> objects = new ArrayList<>();
      for (TermParser termParser : parsers) {
        for (String s : termParser.extractList(val)) {

          // Add predefined units
          if (unitDictionary.containsKey(f.getRelation())) {
            String datatype = FactComponent.getDatatype(s);
            if (datatype != null && (datatype.equals(YAGO.decimal) || datatype.equals(YAGO.integer))) {
              String value = FactComponent.stripQuotes(FactComponent.getString(s));
              s = FactComponent.forStringWithDatatype(value, unitDictionary.get(f.getRelation()));
            }
          }

          if (!objects.contains(s)) objects.add(s);
        }
      }
      for (String object : objects) {
        INFOBOXTERMS_TOREDIRECT.inLanguage(language).write(new Fact(f.getSubject(), f.getRelation(), object));
      }
    }
  }

  public InfoboxTermExtractor(String lang) {
    super(lang);
  }

  public static void main(String[] args) throws Exception {
    InfoboxTermExtractor extractor = new InfoboxTermExtractor("en");
    extractor.extract(new File("c:/fabian/data/yago3"), "mapping infobox attributes into infobox facts");
  }

}
