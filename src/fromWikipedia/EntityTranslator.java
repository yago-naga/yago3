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

public class EntityTranslator extends FollowUpExtractor {

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
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    //    if(this.language.equals("en")){
    //      
    //    }

    FactCollection translatedFacts = new FactCollection();

    Map<String, String> tempDictionary = InterLanguageLinksDictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
    FactCollection temp = new FactCollection();
    loadFacts(input.get(checkMe), temp);
    for (Fact f : temp) {

      String translatedSubject = FactComponent.stripBrackets(f.getArg(1));

      if (tempDictionary.containsKey(FactComponent.stripBrackets(f.getArg(1)))) {
        translatedSubject = tempDictionary.get(FactComponent.stripBrackets(f.getArg(1)));
      }
      translatedFacts.add(new Fact(FactComponent.forYagoEntity(translatedSubject), f.getRelation(), FactComponent.forYagoEntity((FactComponent
          .stripQuotes(f.getArg(2))))));
    }

    for (Fact fact : translatedFacts) {
      output.get(checked).write(fact);
    }

  }

  public EntityTranslator(Theme in, Theme out, String lang) {
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
