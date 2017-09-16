package fromThemes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import basics.Fact;
import basics.RDFS;
import basics.YAGO;
import extractors.Extractor;
import extractors.MultilingualExtractor;
import fromOtherSources.DictionaryExtractor;
import fromWikipedia.CategoryExtractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import main.ParallelCaller;
import utils.Theme;

public class PeopleExtractor extends Extractor {

  public static final Theme MANUALPEOPLE = new Theme("peopleByCategory", "People extracted by manually selected categories");

  public static final Theme MANUALPEOPLESOURCE = new Theme("peopleByCategorySource", "People extracted by manually selected categories");

  private Set<Theme> categoryInput() {
    Set<Theme> input = new HashSet<Theme>();
    input.addAll(CategoryExtractor.CATEGORYMEMBERS.inLanguages(MultilingualExtractor.wikipediaLanguages));
    input.addAll(CategoryExtractor.CATEGORYMEMBERS_TRANSLATED.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    return input;
  }

  @Override
  public Set<Theme> input() {
    Set<Theme> input = new HashSet<Theme>();
    input.addAll(categoryInput());
    input.addAll(DictionaryExtractor.ENTITY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish()));
    return input;
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(MANUALPEOPLE, MANUALPEOPLESOURCE);
  }

  private String join(List<String> list, String delimiter) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.size(); i++) {
      if (i != 0) sb.append(delimiter);
      sb.append(list.get(i));
    }
    return sb.toString();
  }

  @Override
  public void extract() throws Exception {

    Map<String, String> entityDictionary = new HashMap<>();
    for (Theme t : DictionaryExtractor.ENTITY_DICTIONARY.inLanguages(MultilingualExtractor.allLanguagesExceptEnglish())) {
      for (Fact f : t) {
        if ("<_hasTranslation>".equals(f.getRelation())) {
          entityDictionary.put(f.getSubject(), f.getObject());
        }
      }
    }

    List<String> inclRegex = new ArrayList<>();
    inclRegex.addAll(Arrays.asList(".*<wikicat_[0-9].*_births>", ".*<wikicat_[0-9].*_deaths>"));
    inclRegex.add("<wikicat.*_(men|women|people)>");
    inclRegex.addAll(Arrays.asList("<de/wikicat_Geboren_.*[0-9].*>", "<de/wikicat_Gestorben_.*[0-9].*>"));
    inclRegex.addAll(Arrays.asList("<de/wikicat_Mann>", "<de/wikicat_Frau>"));
    inclRegex.addAll(Arrays.asList("<fr/wikicat_Naissance_(en|à)_.*>", "<fr/wikicat_Décès_(en|à)_.*>"));
    inclRegex.addAll(Arrays.asList("<es/wikicat_Hombres>", "<es/wikicat_Mujer>", "<es/wikicat_Nacidos_en_.*>", "<es/wikicat_Fallecidos_en_.*>"));
    inclRegex.addAll(Arrays.asList("<it/wikicat_Nati_nel_.*>", "<es/wikicat_Morti_nel_.*>"));
    inclRegex.addAll(Arrays.asList("<ro/wikicat_Nașteri_(în|pe)_.*>", "<ro/wikicat_Decese_(în|pe)_.*>"));
    // infoboxes
    inclRegex.addAll(Arrays.asList("\"person\".*"));
    List<String> exclRegex = Arrays.asList("(racehorse|animal|tree|Animal|fictional|named_for)");

    Pattern inclPattern = Pattern.compile("(" + join(inclRegex, ")|(") + ")", Pattern.CASE_INSENSITIVE);
    Pattern exclPattern = Pattern.compile("(" + join(exclRegex, ")|(") + ")", Pattern.CASE_INSENSITIVE);

    HashMap<String, String> people = new HashMap<>();
    for (Theme t : categoryInput()) {
      for (Fact f : t) {
        if (inclPattern.matcher(f.getObject()).matches() && !exclPattern.matcher(f.getObject()).find()) {
          people.put(entityDictionary.getOrDefault(f.getSubject(), f.getSubject()), f.getObject());
        }
      }
    }

    for (Entry<String, String> e : people.entrySet()) {
      Fact f = new Fact(e.getKey(), RDFS.type, YAGO.person);
      write(MANUALPEOPLE, f, MANUALPEOPLESOURCE, e.getValue(), "PeopleExtractor");
    }

  }

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    Parameters.init(args[0]);
    File yago = Parameters.getFile("yagoFolder");
    ParallelCaller.createWikipediaList(Parameters.getList("languages"), Parameters.getList("wikipedias"));
    new PeopleExtractor().extract(yago, "MANUAL RUN");
  }
}
