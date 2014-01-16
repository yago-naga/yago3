package fromWikipedia;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javatools.administrative.Announce;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;

/**
 * Dictionary - YAGO2s
 * 
 * Useful to build different dictionaries out of INTERLANGUAGELINKS.
 * Attention: Every Extractor that uses this dictionary, should have 
 * INTERLANGUAGELINKS as one of the inputs. 
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class InterLanguageLinksDictionary {

  private static Map<String, Map<String, String>> rdictionaries = new HashMap<String, Map<String, String>>();

  private static Map<String, String> catDictionary = new HashMap<String, String>();
  private static Map<String, String> infDictionary = new HashMap<String, String>();

  private static Map<String, String> buildReverseDictionary(String secondLang, FactSource fs) throws FileNotFoundException, IOException {
    Map<String, String> rdictionary = new HashMap<String, String>();
    for (Fact f : fs) {
      if (FactComponent.getLanguage(f.getArg(2)).equals(secondLang)) {

        String object = FactComponent.stripQuotes(FactComponent.getString(f.getArg(2)));
        String subject = FactComponent.stripBrackets(f.getArg(1));
        if (!rdictionary.containsKey(object)) {
          rdictionary.put(object, subject);
//          System.out.println(object+" " +  subject);
        }
        //        if(!rdictionary.get(object).contains(subject)){
        //          rdictionary.get(object).add(subject);
        //        }
      }
    }
   
    Announce.done("Dictionary built for " + secondLang);
    return rdictionary;

  }

  private static Map<String, String> buildCatDictionary(FactSource fs) throws FileNotFoundException, IOException {
    catDictionary = new HashMap<String, String>();
    for (Fact f : fs) {

      String object = FactComponent.stripQuotes(FactComponent.getString(f.getArg(2)));
      String subject = FactComponent.stripBrackets(f.getArg(1));

      if (subject.equals("Category") && !catDictionary.containsKey((FactComponent.getLanguage(f.getArg(2))))) {
        catDictionary.put(FactComponent.getLanguage(f.getArg(2)), object);
      }

    }
    return catDictionary;
  }
  
  private static Map<String, String> buildInfDictionary(FactSource fs) throws FileNotFoundException, IOException {
    infDictionary = new HashMap<String, String>();
    for (Fact f : fs) {

      String object = FactComponent.stripQuotes(FactComponent.getString(f.getArg(2)));
      String subject = FactComponent.stripBrackets(f.getArg(1));

      if (subject.equals("Template:Infobox")) {
        infDictionary.put(FactComponent.getLanguage(f.getArg(2)), object);
      }

    }
    return infDictionary;
  }

  public static synchronized Map<String, String> get(String secondLang, FactSource fs) throws FileNotFoundException, IOException {

    if (!rdictionaries.containsKey(secondLang)) {
      rdictionaries.put(secondLang, buildReverseDictionary(secondLang, fs));
    }
    return (rdictionaries.get(secondLang));
  }

  public static synchronized Map<String, String> getCatDictionary(FactSource fs) throws FileNotFoundException, IOException {
    if (catDictionary.isEmpty()) {
      buildCatDictionary(fs);
    }
    return catDictionary;
  }

  public static synchronized Map<String, String> getInfDictionary(FactSource fs) throws FileNotFoundException, IOException {
    if (infDictionary.isEmpty()) {
      buildInfDictionary(fs);
    }
    return infDictionary;
  }

  
}
