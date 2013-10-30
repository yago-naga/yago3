package fromWikipedia;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.attachment.AttachmentMarshaller;

import fromOtherSources.InterLanguageLinks;
import javatools.administrative.Announce;
import basics.ExtendedFactCollection;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;


public class Dictionary {
  

  private static Map<String,Map<String, Set<String>>> rdictionaries = new HashMap<String, Map<String,Set<String>>> ();
  private static Map<String, String> catDictionary = new HashMap<String, String> ();

  private static Map<String, Set<String>> buildReverseDictionary(String secondLang, FactSource fs) throws FileNotFoundException, IOException{
    Map<String, Set<String>> rdictionary = new HashMap<String, Set<String>>(); 
    for(Fact f: fs){
      if(FactComponent.getLanguage(f.getArg(2)).equals( secondLang)){

        String object= FactComponent.stripQuotes(FactComponent.getString(f.getArg(2)));
        String subject =  FactComponent.stripBrackets(f.getArg(1));
        if(!rdictionary.containsKey(object)){
          rdictionary.put(object, new HashSet<String>());
        }
        if(!rdictionary.get(object).contains(subject)){
          rdictionary.get(object).add(subject);
        }
      }
    }
    int count = 0 ; 
    for (Entry<String, Set<String>> e : rdictionary.entrySet())
    {
     
      if(e.getValue().size()>1){ 
        count++;
 
      }
    }
    System.out.println(count +" "+ rdictionary.size());
    Announce.done("Dictionary built for " + secondLang);
    return rdictionary;
    
  }
  
  private static Map<String, String> buildCatDictionary(FactSource fs) throws FileNotFoundException, IOException{
    catDictionary = new HashMap<String, String>(); 
    for(Fact f: fs){

      String object= FactComponent.stripQuotes(FactComponent.getString(f.getArg(2)));
      String subject =  FactComponent.stripBrackets(f.getArg(1));
   
    if(subject.equals("Category")){
        catDictionary.put(FactComponent.getLanguage(f.getArg(2)), object);
        System.out.println("added " + FactComponent.getLanguage(f.getArg(2)) + " " + object);
    }
      

    }
    return catDictionary;
  }
  
  public static synchronized Map<String, Set<String>> get(String secondLang,FactSource fs) throws FileNotFoundException, IOException{
   
    if(!rdictionaries.containsKey(secondLang)) {
      rdictionaries.put(secondLang, buildReverseDictionary(secondLang, fs));
    }
    return(rdictionaries.get(secondLang));
  }
  
  public static synchronized Map<String, String> getCatDictionary(FactSource fs) throws FileNotFoundException, IOException{
    if(catDictionary.isEmpty()){
      buildCatDictionary(fs);
    }
    System.out.println(catDictionary);
    return catDictionary; 
  }
  
  

}
