package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fromOtherSources.InterLanguageLinks;
import fromWikipedia.Extractor.FollowUpExtractor;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * EntityTranslator - YAGO2s
 * 
 * Translates the subjects and objects of the input themes to the most English language.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class CategoryTranslator extends FollowUpExtractor {

  private String language;

  @Override
  public Set<Theme> input() {
    return new HashSet<Theme>(Arrays.asList(checkMe, InterLanguageLinks.INTERLANGUAGELINKS));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<Theme>(checked);
  }

  @Override
  public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    Map<String, String> rdictionary = InterLanguageLinksDictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
    String categoryWord = InterLanguageLinksDictionary.getCatDictionary(input.get(InterLanguageLinks.INTERLANGUAGELINKS)).get(language); //= "Kategorie"; 
    for (Fact f : input.get(checkMe)) {
      String entity = f.getArg(1);
      String category = rdictionary.get(categoryWord + ":" + FactComponent.stripBrackets(f.getArg(2).replace(" ", "_")));

      if (category == null) continue;

      String temp = category;
      if (temp.contains(":")) {
        temp = temp.substring(temp.lastIndexOf(":") + 1);
      }

      Fact fact = new Fact(FactComponent.forYagoEntity(entity), "<hasWikiCategory/en>", FactComponent.forString(temp));
      writers.get(checked).write(fact);

    }

  }

  public CategoryTranslator(Theme in, Theme out, String lang) {
    this.checkMe = in;
    this.checked = out;
    this.language = lang;
  }

  protected FactCollection loadFacts(FactSource factSource, FactCollection temp) {
    for (Fact f : factSource) {
      temp.add(f);
    }
    return (temp);
  }

  public static void main(String[] args) throws Exception {
    new EntityTranslator(Theme.forFile(new File("D:/data2/yago2s/infoboxAttributesRedirected_de.ttl")), Theme.forFile(new File(
        "D:/data2/yago2s/res.ttl")), "de").extract(new File("D:/data2/yago2s"), null);
  }

}
