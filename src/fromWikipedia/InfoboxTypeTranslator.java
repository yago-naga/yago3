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

public class InfoboxTypeTranslator extends FollowUpExtractor {

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
    String infoboxWord = InterLanguageLinksDictionary.getInfDictionary(input.get(InterLanguageLinks.INTERLANGUAGELINKS)).get(language);

    for (Fact f : input.get(checkMe)) {

      String entity = f.getArg(1);
      //          rdictionary.get(FactComponent.stripBrackets(f.getArg(1)));  
      String word = FactComponent.stripBrackets(f.getArg(2).replace(" ", "_"));
      if (word.length() > 0) word = word.substring(0, 1).toUpperCase() + word.substring(1);

      String category = rdictionary.get(infoboxWord + "_" + word);
      if (category == null) continue;

      String temp = category;
      if (temp.contains("_")) {
        temp = temp.substring(temp.lastIndexOf("_") + 1);
      }

      Fact fact = new Fact(FactComponent.forYagoEntity(entity), "<hasInfoboxType/en>", FactComponent.forString(temp));
      writers.get(checked).write(fact);

    }

  }

  public InfoboxTypeTranslator(Theme in, Theme out, String lang) {
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
