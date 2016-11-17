package fromOtherSources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;


public class MissingWikidataIDInvestigation {
  
  public static void main(String[] args) throws IOException {
    CSVReader reader = null;
 // list of triples of <human readable name, url, id in db>:
    //List <Triple<String, String, String>> missingIds = new LinkedList<Triple<String, String, String>>();
    // Human -> url
    Map<String, String> missingIds = new HashMap<String, String>();
    List<Pair<String,String> > dups = new LinkedList<>();
    try {
      reader = new CSVReader(new FileReader("/home/ghazaleh/Projects/data/missingIDs"), ';');
      String[] line;
      reader.readNext();//names
      while ((line = reader.readNext()) != null) {
        if (missingIds.containsKey(line[1])){
          dups.add(new Pair<String, String>(line[1], line[2]));
          System.out.println(line[1] + " " + line[2]);
        }
        else
          missingIds.put(line[1],  line[2]);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    System.out.println(missingIds.size());
    
    Map<String,  Pair<String, String>> redirects = new HashMap<String, Pair<String, String>>();
    Map<String,  String> havePage = new HashMap<String, String>();
    
    Pattern redirect = Pattern.compile("<redirect title=(.*?)\\s*/>");
    BufferedReader in = FileUtils.getBufferedUTF8Reader("/local_san2/ambiverse/jenkins/workspace/entity_linking_repository_creation/tmp_dumps/en/20160701/enwiki-20160701-pages-articles.xml");
   
    while (FileLines.findIgnoreCase(in, "<title>") != -1 ) {
      String title = FileLines.readToBoundary(in, "</title>");
      
      if (missingIds.containsKey(title)) {
        havePage.put(title, missingIds.get(title));
        String page = FileLines.readTo(in, "</page>").toString();
        Matcher matcher = redirect.matcher(page);
        if (matcher.find()) {
          redirects.put(title, new Pair<String, String>(missingIds.get(title), matcher.group(1)));
          //System.out.println(title + " - " + missingIds.get(title) + " - " + matcher.group(1));
        }
      }
      
    }
 
   
    in.close();
    
    System.out.println("duplicate size: " + dups.size());
    System.out.println("redirect size: " + redirects.size());
    System.out.println("have page: " + havePage.size());
    System.out.println("tamam");
  
  }

}
