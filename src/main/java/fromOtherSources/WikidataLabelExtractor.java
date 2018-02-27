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

package fromOtherSources;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.RDFS;
import basics.YAGO;
import extractors.DataExtractor;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import followUp.FollowUpExtractor;
import followUp.TypeChecker;
import fromThemes.CoherentTypeExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.Theme;

/**
 * Extracts labels from wikidata
 *
*/
public class WikidataLabelExtractor extends DataExtractor {

  private static final String WIKIDATA = "wikidata";

  public WikidataLabelExtractor(File wikidata) {
    super(wikidata);
  }

  public WikidataLabelExtractor() {
    this(Parameters.getFile(WIKIDATA));
  }

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new TreeSet<>();
    input.add(PatternHardExtractor.LANGUAGECODEMAPPING);
    if (Extractor.includeConcepts) {
      input.add(CategoryExtractor.CATEGORYMEMBERS.inEnglish());
      input.addAll(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    }
    else {
      input.add(CoherentTypeExtractor.YAGOTYPES);
    }
    return input;
  }

  @Override
  public Set<Theme> inputCached() {
    return new FinalSet<Theme>(CoherentTypeExtractor.YAGOTYPES, PatternHardExtractor.LANGUAGECODEMAPPING);
  }

  /** Facts deduced from categories */
  public static final Theme WIKIPEDIALABELS = new Theme("wikipediaLabels", "Labels derived from the name of the instance in Wikipedia");

  /** Sources */
  public static final Theme WIKIPEDIALABELSOURCES = new Theme("wikipediaLabelSources", "Sources for the Wikipedia labels");

  /** Wikidata QIDs */
  public static final Theme WIKIDATAINSTANCES = new Theme("wikidataInstances", "Mappings of YAGO instances to Wikidata QIDs");

  /** Wikidata QIDs type checked */
  public static final Theme YAGOWIKIDATAINSTANCES = new Theme("yagoWikidataInstances", "Mappings of YAGO instances to Wikidata QIDs");

  /** Facts deduced from categories */
  public static final Theme WIKIDATAMULTILABELSNEEDSTYPECHECK = new Theme("wikidataMultiLabelsNeedsTypeCheck", "Labels from Wikidata in multiple languages");
  public static final Theme WIKIDATAMULTILABELS = new Theme("wikidataMultiLabels", "Labels from Wikidata in multiple languages");

  /** Sources */
  public static final Theme WIKIDATAMULTILABELSOURCES = new Theme("wikidataMultiLabelSources", "Sources for the multilingual labels");

  /** Number of translations */
  public static final Theme WIKIDATATRANSLATIONSNEEDSTYPECHECK = new Theme("wikidataTranslationsNeedsTypeCheck", "Number of translations per entity");
  public static final Theme WIKIDATATRANSLATIONS = new Theme("wikidataTranslations", "Number of translations per entity");

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(WIKIPEDIALABELSOURCES, WIKIPEDIALABELS, WIKIDATAINSTANCES, WIKIDATAMULTILABELSOURCES, WIKIDATAMULTILABELSNEEDSTYPECHECK,
        WIKIDATATRANSLATIONSNEEDSTYPECHECK);
  }

  @Override
  public Set<FollowUpExtractor> followUp() {
    return new FinalSet<FollowUpExtractor>(new TypeChecker(WIKIDATAINSTANCES, YAGOWIKIDATAINSTANCES),
        new TypeChecker(WIKIDATAMULTILABELSNEEDSTYPECHECK, WIKIDATAMULTILABELS),
        new TypeChecker(WIKIDATATRANSLATIONSNEEDSTYPECHECK, WIKIDATATRANSLATIONS));
  }

  @Override
  public void extract() throws Exception {
    List<String> availableLanguages = new ArrayList<>(MultilingualExtractor.wikipediaLanguages);

    Map<String, String> languagemap = PatternHardExtractor.LANGUAGECODEMAPPING.factCollection().getStringMap("<hasThreeLetterLanguageCode>");
    Set<String> entities;
    if (Extractor.includeConcepts) {
      entities = new HashSet<>();
      entities.addAll(CategoryExtractor.CATEGORYMEMBERS.inEnglish().factCollection().getSubjects());
      for (String lang : MultilingualExtractor.allLanguagesExceptEnglish()) {
        entities.addAll(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguage(lang).factCollection().getSubjects());
      }
    }
    else {
      entities = CoherentTypeExtractor.YAGOTYPES.factCollection().getSubjects();
    }

    Announce.message("Loaded", languagemap.size(), "languages and ", entities.size(), "entities");

    // first write the English names 
    for (String yagoEntity : entities) {
      write(WIKIPEDIALABELS, new Fact(yagoEntity, YAGO.hasPreferredName, FactComponent.forStringWithLanguage(preferredName(yagoEntity), "eng")),
          WIKIPEDIALABELSOURCES, "<http://wikidata.org>", "WikidataLabelExtractor");
      for (String name : trivialNamesOf(yagoEntity)) {
        write(WIKIPEDIALABELS, new Fact(yagoEntity, RDFS.label, FactComponent.forStringWithLanguage(name, "eng")), WIKIPEDIALABELSOURCES,
            "<http://wikidata.org>", "WikidataLabelExtractor");
      }

    }
    // Now write the foreign names
    N4Reader nr = new N4Reader(inputData);
    // Maps a language such as "en" to the name in that language
    Map<String, String> language2name = new HashMap<String, String>();
    String lastqid = null;
    while (nr.hasNext()) {
      Fact f = nr.next();
      // Record a new name in the map
      if (f.getRelation().endsWith("/inLanguage>")) {
        String lang = FactComponent.stripQuotes(f.getObject());
        String name = FactComponent.stripWikipediaPrefix(Char17.decodePercentage(f.getSubject()));

        if (name != null) language2name.put(lang, name);
      }
      // Get to the line that information about a new item begin from.
      // example: <http://www.wikidata.org/entity/Q1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wikidata.org/ontology#Item> .
      else if (f.getArg(2).endsWith("#Item>")) {
        // Write the previous item: (data between 2 "#Item" belong to previous one)
        if (!language2name.isEmpty()) {
          // New item starts, let's flush out the previous one
          // mostEnglish is based on the input language order from most to least
          String mostEnglishLan = DictionaryExtractor.mostEnglishLanguage(language2name.keySet());
          if (mostEnglishLan != null) {
            String mostEnglishName = language2name.get(mostEnglishLan);
            String yagoEntity = FactComponent.forForeignYagoEntity(mostEnglishName, mostEnglishLan);

            WIKIDATATRANSLATIONSNEEDSTYPECHECK.write(new Fact(yagoEntity, "<numberOfTranslations>", "" + language2name.size()));
            // For on all languages
            for (String lang : language2name.keySet()) {
              String foreignName = language2name.get(lang);

              // Check if the language is available (input languages)
              if (availableLanguages.contains(lang))
                WIKIDATAINSTANCES.write(new Fact(FactComponent.forForeignYagoEntity(foreignName, lang), RDFS.sameas, lastqid));

              // Change 2-letter language code to 3-letter
              if (lang.length() == 2) lang = languagemap.get(lang);
              if (lang == null || lang.length() != 3) continue;
              for (String name : trivialNamesOf(foreignName)) {
                write(WIKIDATAMULTILABELSNEEDSTYPECHECK, new Fact(yagoEntity, RDFS.label, FactComponent.forStringWithLanguage(name, lang)),
                    WIKIDATAMULTILABELSOURCES, "<http://wikidata.org>", "WikidataLabelExtractor");
              }
            }
          }
          language2name.clear();
        }

        lastqid = f.getSubject();
      }
    }
    nr.close();
  }

  /** returns the (trivial) names of an entity */
  public static Set<String> trivialNamesOf(String titleEntity) {
    Set<String> result = new TreeSet<>();
    String name = preferredName(titleEntity);
    result.add(name);
    String norm = Char17.normalize(name);
    if (!norm.contains("[?]")) result.add(norm);
    if (name.contains(" (")) {
      result.add(name.substring(0, name.indexOf(" (")).trim());
    }
    if (name.contains(",") && !name.contains("(")) {
      result.add(name.substring(0, name.indexOf(",")).trim());
    }
    return (result);
  }

  /** returns the preferred name */
  public static String preferredName(String titleEntity) {
    return (Char17.decode(FactComponent.stripBracketsAndLanguage(titleEntity).replace('_', ' ')));
  }

  public static void main(String[] args) throws Exception {
    Parameters.init("configuration/yago_tr.ini");
    new WikidataLabelExtractor().extract(new File("/san/suchanek/yago3-debug"), "test");
  }
}
