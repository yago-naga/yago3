package fromOtherSources;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import extractors.DataExtractor;
import extractors.MultilingualExtractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

/**
 * YAGO2s - DictionaryExtractor
 *
 * Extracts inter-language links from Wikidata and builds dictionaries.
 *
This class is part of the YAGO project at the Max Planck Institute
for Informatics/Germany and Télécom ParisTech University/France:
http://yago-knowledge.org

This class is copyright 2016 Farzaneh Mahdisoltani, with contributions from Fabian M. Suchanek.

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
public class DictionaryExtractor extends DataExtractor {

  /** Output theme */
  public static final MultilingualTheme ENTITY_DICTIONARY = new MultilingualTheme("entityDictionary",
      "Maps a foreign entity to a YAGO entity. Data from (http://http://www.wikidata.org/).");

  /** Words for "category" in different languages */
  public static final Theme CATEGORYWORDS = new Theme("categoryWords", "Words for 'category' in different languages.");

  /** Translations of infobox templates */
  public static final MultilingualTheme INFOBOX_TEMPLATE_DICTIONARY = new MultilingualTheme("infoboxTemplateDictionary",
      "Maps a foreign infobox template name to the English name.");

  private static final String WIKIDATA_SITELINKS = "wikidata_sitelinks";

  /**
   * This TitleExtractor makes sure every foreign word gets mapped to a valid
   * English one
   */
  protected TitleExtractor titleExtractor;

  /** Translations of categories */
  public static final MultilingualTheme CATEGORY_DICTIONARY = new MultilingualTheme("categoryDictionary",
      "Maps a foreign category name to the English name.");

  public DictionaryExtractor(File wikidata) {
    super(wikidata);
  }

  public DictionaryExtractor() {
    this(Parameters.getFile(WIKIDATA_SITELINKS));
  }

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.PREFMEANINGS);
  }

  @Override
  public Set<Theme> output() {
    Set<Theme> result = new HashSet<Theme>();
    result.add(CATEGORYWORDS);
    result.addAll(CATEGORY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    result.addAll(ENTITY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    result.addAll(INFOBOX_TEMPLATE_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    return (result);
  }

  /** Returns the most English language in the set, or NULL */
  public static String mostEnglishLanguage(Collection<String> langs) {
    for (int i = 0; i < MultilingualExtractor.wikipediaLanguages.size(); i++) {
      if (langs.contains(MultilingualExtractor.wikipediaLanguages.get(i))) return (MultilingualExtractor.wikipediaLanguages.get(i));
    }
    return (null);
  }

  /** Wikipedia article URL*/
  protected static Pattern WikipediaUrlPattern = Pattern.compile("<https://([a-z]+)\\.wikipedia.org/wiki/([^>]*)>");

  @Override
  public void extract() throws Exception {
    // This TitleExtractor is used to filter out lists etc.
    // directly from the dictionary
    titleExtractor = new TitleExtractor("en");

    Announce.message("Input file is", inputData);
    Announce.message("There are 110m facts. On the real Wikidata file on a laptop, this process takes ages.");
    Announce.message("Even if the output files do not seem to fill, they do fill eventually.");

    // Categories for which we have already translated the word "category"
    Set<String> categoryWordLanguages = new HashSet<>();

    N4Reader nr = new N4Reader(inputData);
    // Maps a language such as "en" to the name in that language
    Map<String, String> language2name = new HashMap<String, String>();
    while (nr.hasNext()) {
      Fact f = nr.next();
      // Record a new name in the map
      if (f.getRelation().equals("<http://schema.org/inLanguage>")) {
        Matcher m = WikipediaUrlPattern.matcher(f.getSubject());
        if (!m.matches()) continue;
        String lan = m.group(1);
        if (!MultilingualExtractor.wikipediaLanguages.contains(lan)) continue;
        String entity = m.group(2);
        // Just to make sure that stuff like "Category:" is handled correctly
        entity = Char17.decodePercentage(entity);
        language2name.put(lan, entity);
      } else if (f.getObject().equals("<http://www.wikidata.org/ontology#Item>") && !language2name.isEmpty()) {
        // New item starts, let's flush out the previous one
        String mostEnglishLan = mostEnglishLanguage(language2name.keySet());
        if (mostEnglishLan != null) flush(categoryWordLanguages, language2name, mostEnglishLan);
        language2name.clear();
      }
    }
    // Not sure why they forgot this one fact in Wikidata... It find lots of genders.
    if (MultilingualExtractor.wikipediaLanguages.contains("de")) {
      CATEGORY_DICTIONARY.inLanguage("de").write(new Fact("<de/wikicat_Mann>", "<_hasTranslation>", "<wikicat_Men>"));
      CATEGORY_DICTIONARY.inLanguage("de").write(new Fact("<de/wikicat_Frau>", "<_hasTranslation>", "<wikicat_Women>"));
    }
    nr.close();
  }

  /** Flushes an entity, template, or category */
  private void flush(Set<String> categoryWordLanguages, Map<String, String> language2name, String mostEnglishLan) throws IOException {
    String mostEnglishName = language2name.get(mostEnglishLan);
    if (FactComponent.isEnglish(mostEnglishLan) && mostEnglishName.startsWith("Category:")) {
      flushCategoryWord(categoryWordLanguages, language2name, mostEnglishName);
    } else if (FactComponent.isEnglish(mostEnglishLan) && mostEnglishName.startsWith("Template:Infobox_")) {
      flushTemplateName(language2name, mostEnglishName);
    } else {
      flushEntity(language2name, mostEnglishLan, mostEnglishName);
    }
  }

  /** Flushes an entity */
  private void flushEntity(Map<String, String> language2name, String mostEnglishLan, String mostEnglishName) throws IOException {
    // Make sure that we exclude lists and general concepts
    // right up front.
    if (FactComponent.isEnglish(mostEnglishLan)) {
      if (titleExtractor.createTitleEntity(mostEnglishName.replace('_', ' ')) == null) {
        return;
      }
    }
    for (String lan : language2name.keySet()) {
      if (FactComponent.isEnglish(lan)) continue;
      ENTITY_DICTIONARY.inLanguage(lan).write(new Fact(FactComponent.forForeignYagoEntity(language2name.get(lan), lan), "<_hasTranslation>",
          FactComponent.forForeignYagoEntity(mostEnglishName, mostEnglishLan)));
    }
  }

  /** Flushes a template */
  private void flushTemplateName(Map<String, String> language2name, String mostEnglishName) throws IOException {
    for (String lan : language2name.keySet()) {
      if (FactComponent.isEnglish(lan)) continue;
      String name = language2name.get(lan);
      int cutpos = name.indexOf('_');
      if (cutpos == -1) continue;
      name = FactComponent.forInfoboxTemplate(name.substring(cutpos + 1), lan);
      INFOBOX_TEMPLATE_DICTIONARY.inLanguage(lan)
          .write(new Fact(name, "<_hasTranslation>", FactComponent.forInfoboxTemplate(mostEnglishName.substring(17), "en")));
    }
  }

  /** Flushes a category word */
  private void flushCategoryWord(Set<String> categoryWordLanguages, Map<String, String> language2name, String mostEnglishName) throws IOException {
    for (String lan : language2name.keySet()) {
      String catword = language2name.get(lan);
      int cutpos = catword.indexOf(':');
      if (cutpos == -1) continue;
      String name = catword.substring(cutpos + 1);
      catword = catword.substring(0, cutpos);
      if (!categoryWordLanguages.contains(lan)) {
        CATEGORYWORDS.write(new Fact(FactComponent.forString(lan), "<_hasCategoryWord>", FactComponent.forString(catword)));
        categoryWordLanguages.add(lan);
      }
      if (!FactComponent.isEnglish(lan)) CATEGORY_DICTIONARY.inLanguage(lan).write(new Fact(FactComponent.forForeignWikiCategory(name, lan),
          "<_hasTranslation>", FactComponent.forWikiCategory(mostEnglishName.substring(9))));
    }
  }

  public static void main(String[] args) throws Exception {
    //Parameters.init("configuration/yago_aida_ghazale.ini");
    //File wikidata = Parameters.getFile(WIKIDATA_SITELINKS);
    //new DictionaryExtractor(new File("c:/fabian/data/wikidata/links.n4")).extract(new File("c:/fabian/data/yago3"), "test");
    Parameters.init("/san/suchanek/workspace/yago3/yagoDebug.ini");
    MultilingualExtractor.wikipediaLanguages = Arrays.asList("en", "de");
    new DictionaryExtractor(new File("/san/suchanek/yago3-debug/wikidata-sitelinks.nt")).extract(new File("/san/suchanek/yago3-debug/"), "test");
  }

}