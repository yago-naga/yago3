package fromWikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fromOtherSources.InterLanguageLinks;
import javatools.administrative.Announce;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;

/**
 * Dictionary - YAGO2s
 * 
 * Useful to build different dictionaries out of INTERLANGUAGELINKS.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class Dictionary {

  private static Map<String, Map<String, String>> rdictionaries = new HashMap<String, Map<String, String>>();

  private static Map<String, String> catDictionary = new HashMap<String, String>();
  private static Map<String, String> infDictionary = new HashMap<String, String>();

  private static FactSource fs = FactSource.from(InterLanguageLinks.INTERLANGUAGELINKS.file(new File("D:/data2/yago2s")));

  private static Map<String, String> buildReverseDictionary(String secondLang) throws FileNotFoundException, IOException {
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

  private static Map<String, String> buildCatDictionary() throws FileNotFoundException, IOException {
    catDictionary = new HashMap<String, String>();
    for (Fact f : fs) {

      String object = FactComponent.stripQuotes(FactComponent.getString(f.getArg(2)));
      String subject = FactComponent.stripBrackets(f.getArg(1));

      if (subject.equals("Category")) {
        catDictionary.put(FactComponent.getLanguage(f.getArg(2)), object);
      }

    }
    return catDictionary;
  }
  
  private static Map<String, String> buildInfDictionary() throws FileNotFoundException, IOException {
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

  public static synchronized Map<String, String> get(String secondLang) throws FileNotFoundException, IOException {

    if (!rdictionaries.containsKey(secondLang)) {
      rdictionaries.put(secondLang, buildReverseDictionary(secondLang));
    }
    return (rdictionaries.get(secondLang));
  }

  public static synchronized Map<String, String> getCatDictionary() throws FileNotFoundException, IOException {
    if (catDictionary.isEmpty()) {
      buildCatDictionary();
    }
    return catDictionary;
  }

  public static synchronized Map<String, String> getInfDictionary() throws FileNotFoundException, IOException {
    if (infDictionary.isEmpty()) {
      buildInfDictionary();
    }
    return infDictionary;
  }

  public static void main(String[] args) throws FileNotFoundException, IOException {
    Dictionary d = new Dictionary();
    d.buildReverseDictionary("en");
  }
  
}
