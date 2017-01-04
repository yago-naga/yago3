package fromOtherSources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char17;
import javatools.util.FileUtils;


public class MissingWikidataIDInvestigation {
  
  public static void main(String[] args) throws Exception {
    String str1 = "https://en.wikipedia.org/wiki/Minolta_AF_Macro_50mm_f/2.8";
    String str2 = "https://en.wikipedia.org/wiki/Minolta_AF_Macro_50mm_f%2F2.8";
    
    System.out.println(FactComponent.forWikipediaURL(str1));
    System.out.println(FactComponent.forWikipediaURL(str2));
   
    
    System.exit(1);
    CSVReader reader = null;
    // Map missingIds: Human readable name -> url
    Map<String, String> missingIds = new HashMap<String, String>();
    // The duplicates are actually not duplicates. They are a bit wrong.
    List<Pair<String,String> > duplicates = new LinkedList<>();
    try {
      reader = new CSVReader(new FileReader("/home/ghazaleh/Projects/data/missingIDs"), ';');
      String[] line;
      reader.readNext();//names
      while ((line = reader.readNext()) != null) {
        if (missingIds.containsKey(line[1])){
          duplicates.add(new Pair<String, String>(line[1], line[2]));
          //System.out.println(line[0] + " " + line[1] + " " + line[2] + " " + line[3]);
        }
        else
          missingIds.put(line[1],  line[2]);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    
    
    Map<String,  Pair<String, String>> redirects = new HashMap<String, Pair<String, String>>();
    Map<String,  String> havePage = new HashMap<String, String>();
    
    Pattern redirect = Pattern.compile("<redirect\\s+title=(.*?)\\s*\\/>");
    BufferedReader in = FileUtils.getBufferedUTF8Reader("/local_san2/ambiverse/jenkins/workspace/entity_linking_repository_creation/tmp_dumps/en/20160701/enwiki-20160701-pages-articles.xml");
    PrintWriter redirectWriter = new PrintWriter("/home/ghazaleh/Projects/data/redirects", "UTF-8");
    while (FileLines.findIgnoreCase(in, "<title>") != -1 ) {
      String title = FileLines.readToBoundary(in, "</title>");
      
      if (missingIds.containsKey(title)) {
        havePage.put(title, missingIds.get(title));
        String page = FileLines.readTo(in, "</page>").toString();
        Matcher matcher = redirect.matcher(page);
        if (matcher.find()) {
          redirects.put(title, new Pair<String, String>(missingIds.get(title), matcher.group(1)));
          redirectWriter.println(title + " " + missingIds.get(title) + " " + matcher.group(1));
        }
      }
      
    }
 
    in.close();
    
    
   
    // FOUND the problem: here:
    
    Map<String, String>pagesWithNoID_reverse = new HashMap<String, String>();
    //PrintWriter out = new PrintWriter("/home/ghazaleh/Projects/data/havePage_NoWikiID", "UTF-8");
    for (String key:havePage.keySet()) {
      if (redirects.containsKey(key)) continue;
      //out.println(key + " " + havePage.get(key));
      pagesWithNoID_reverse.put(Char17.decodePercentage(havePage.get(key)).replaceAll(" ", "_"), key);
    }
    
    Map<String, String> havePage_reverse = new HashMap<>();
    for (String key:missingIds.keySet()) {
      havePage_reverse.put(Char17.decodePercentage(missingIds.get(key)).replaceAll(" ", "_"), key);
    }
    
//    Map<String, String>pagesWithNoID_reverse = new HashMap<String, String>();
//    try(BufferedReader br = new BufferedReader(new FileReader("/home/ghazaleh/Projects/data/havePage_NoWikiID"))) {
//      for(String line;(line = br.readLine()) != null;) {
//        int seperator = line.indexOf("http");
//        pagesWithNoID_reverse.put(Char17.decodePercentage(line.substring(seperator)).replaceAll(" ", "_"), line.substring(0, seperator));
//      }
//    }
    

    PrintWriter idhavingWriter = new PrintWriter("/home/ghazaleh/Projects/data/idhaving", "UTF-8");
    Map<String, String> IDsOfmissing = new HashMap<>();
    //N4Reader nr = new N4Reader(new File("/local_san2/ambiverse/jenkins/workspace/aida_repository_creation/tmp_dumps/wikidata_dump/20160801/wikidata-sitelinks.nt"));
    N4Reader nr = new N4Reader(new File("/local_san2/ambiverse/jenkins/workspace/entity_linking_repository_creation/tmp_dumps/wikidata_dump/20160621/wikidata-sitelinks.nt"));
    while (nr.hasNext()) {
      Fact f = nr.next();
      String url = FactComponent.stripBrackets(f.getSubject());
      if (f.getRelation().equals("<http://schema.org/about>") && havePage_reverse.containsKey(url)){
        if (pagesWithNoID_reverse.containsKey(url) ) {
          IDsOfmissing.put(url, f.getObject());
          idhavingWriter.println(url + " " + f.getObject());
        }
      }
    }
    nr.close();
    
    PrintWriter idmissingWriter = new PrintWriter("/home/ghazaleh/Projects/data/idsmissing", "UTF-8");
    
    for(String key:pagesWithNoID_reverse.keySet()) {
      if (IDsOfmissing.containsKey(key))  continue;
      idmissingWriter.println(key + " " + havePage_reverse.get(key));
      //System.out.println(key);
    }
    idhavingWriter.close();
    
    System.out.println("missing ID size: " + missingIds.size());
    System.out.println("duplicate size: " + duplicates.size());
    System.out.println("redirect size: " + redirects.size());
    System.out.println("have page: " + havePage.size());
    System.out.println("no id pages: " + pagesWithNoID_reverse.size());
    System.out.println("size of id dara: " + IDsOfmissing.size());
  }

}
