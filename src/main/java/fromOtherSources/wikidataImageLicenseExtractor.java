package fromOtherSources;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.DataExtractor;
import extractors.Extractor;
import javatools.datatypes.FinalSet;
import javatools.parsers.Char17;
import utils.Theme;

public class wikidataImageLicenseExtractor extends Extractor {

  //public static final Theme WIKIDATAIMAGELICENSE = new Theme("wikidataImageLicenses", 
    //  "Licences extracted for wikidata Images");
  
  public static int cnt;
  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(WikidataImageExtractor.WIKIDATAIMAGES));
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>();
    //return (new FinalSet<>(WIKIDATAIMAGELICENSE));

  }

  @Override
  public void extract() throws Exception {
    
   cnt = 0;
    Set<Fact> wikidataEntityImage = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getFactsWithRelation(YAGO.hasWikiDataImageUrl);
    for(Fact fact : wikidataEntityImage){
      String url = FactComponent.stripBrackets(fact.getObject());
      String editUrl = null;
      try {
        editUrl = URI.create(url).toASCIIString().replaceAll("&", "%26");
      }
      catch (Exception e) {
        editUrl = url.replaceAll("&", "%26");;
      }
      int fileNameIndex = editUrl.indexOf("/wiki/") + "/wiki/".length();
      editUrl = "https://commons.wikimedia.org/w/index.php?title=" + editUrl.substring(fileNameIndex) + "&action=edit";
      temp(editUrl, FactComponent.forUri(url));
    }
  }
  
  private static void temp(String imageUrl, String printUrl){
    Connection connect = Jsoup.connect(imageUrl);
    try {
      Document doc = connect.get();
      Element elem = doc.getElementById("wpTextbox1");
      if (elem == null) {
        System.out.println("no: " + printUrl + "\n" + imageUrl);
        cnt++;
      }
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      System.out.println(imageUrl + " not connected");
    }
  }
  

}
