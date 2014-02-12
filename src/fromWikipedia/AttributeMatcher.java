package fromWikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import utils.TermExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FrequencyVector;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import fromOtherSources.HardExtractor;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromThemes.InfoboxTermExtractor;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.ExtendedFactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.N4Reader;
import basics.RDFS;
import basics.Theme;
import basics.YAGO;
import basics.Theme.ThemeGroup;

/**
 * YAGO2s - AttributeMatcher
 * 
 * This Extractor matches cross-lingual infobox attributes. 
 * 
 * @author Farzaneh Mahdisoltani
 */

public class AttributeMatcher extends Extractor { 
	
	private static ExtendedFactCollection yagoFactCollection = null;
	
	Map<String, Integer> attCounts; 
	
	Map<String, Map<String,Pair<Integer, Integer>>> statistics;
	private String language;
	private Map<String, String> rdictionary; 
	private double WILSON_THRESHOLD = 0;
	private double SUPPORT_THRESHOLD = 1;
	
	public void setWilsonThreshold(double d){
	  WILSON_THRESHOLD = d;
	}

	public void setSupportThreshold(double d){
	  SUPPORT_THRESHOLD = d;
	}

	public static final HashMap<String, Theme> MATCHED_INFOBOXATTS_MAP = new HashMap<String, Theme>();
	public static final HashMap<String, Theme> MATCHEDATTSOURCES_MAP = new HashMap<String, Theme>();
	  
	static {
	  for (String s : Extractor.languages) {
	    MATCHED_INFOBOXATTS_MAP.put(s, new Theme("matchedInfoboxAtts" + Extractor.langPostfixes.get(s),
	        "Attributes of the Wikipedia infoboxes in different languages are matched.", ThemeGroup.OTHER));
	    MATCHEDATTSOURCES_MAP.put(s, new Theme("matchedAttSources" + Extractor.langPostfixes.get(s), "Sources of infobox", ThemeGroup.OTHER));
	  }

	}

	@Override
	public Set<Theme> input() {
	  HashSet<Theme> result = new HashSet<Theme>(
	      Arrays.asList(
	          InfoboxMapper.INFOBOXFACTS_MAP.get("en"),
	          InterLanguageLinks.INTERLANGUAGELINKS,
	          PatternHardExtractor.INFOBOXPATTERNS, 
	          HardExtractor.HARDWIREDFACTS, 
	          WordnetExtractor.WORDNETWORDS,
	          InfoboxTermExtractor.INFOBOXATTSTRANSLATED_MAP.get(language)
	          ));
	  return result;
	}

	@Override
	public Set<Theme> output() {
		return new HashSet<>(Arrays.asList(MATCHED_INFOBOXATTS_MAP.get(language),MATCHEDATTSOURCES_MAP.get(language)));
	}
	
//	@Override
//	public Set<Extractor> followUp() {
//	  return new HashSet<Extractor>(Arrays.asList(new InfoboxMapper(this.language)));
//	}

	@Override
	public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> input) throws Exception {
    rdictionary = InterLanguageLinksDictionary.get(language, input.get(InterLanguageLinks.INTERLANGUAGELINKS));
    statistics = new HashMap<String, Map<String,Pair <Integer,Integer>>>();
    FactCollection hardWiredFacts = new FactCollection(input.get(HardExtractor.HARDWIREDFACTS));
    Map<String, String> preferredMeaning = WordnetExtractor.preferredMeanings(input);
    ExtendedFactCollection myFactCollection = getFactCollection(input.get(InfoboxMapper.INFOBOXFACTS_MAP.get("en")));
    //        ExtendedFactCollection myFactCollection = getFactCollection(new File("D:/yago2s_ttl"));
    FactSource lang2FactSource= input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(language)); //?

    Announce.progressStart("Running through "+language+" Wikipedia", 12713794); 
    int lang2FactSourceSize = 0;
    for (Fact f2 : lang2FactSource){
      lang2FactSourceSize++;
      Announce.progressStep();
      String secondLangSubject = FactComponent.stripBrackets(f2.getArg(1));
      String secondLangRelation = f2.getRelation();
      String secondLangObject = f2.getArg(2);

      String yagoArg = /*rdictionary.get*/(secondLangSubject);
      if(yagoArg==null) {
        continue;
      }
      List<Fact> yagoFactsWithSubject = myFactCollection.getFactsWithSubject(FactComponent.forYagoEntity(yagoArg));

      for(Fact f: yagoFactsWithSubject){
        String yagoRelation = f.getRelation();
        String yagoObject = FactComponent.stripBrackets(f.getArg(2));

        String expectedDatatype = hardWiredFacts.getArg2(yagoRelation, RDFS.range);
        /* assumption: all the subjects are entities*/

        // if(isEqualr(yagoSubject,secondLangSubject)){   //expectation: always be true
        if(isEqual(yagoObject, secondLangObject, expectedDatatype, preferredMeaning )){

          deduce(yagoRelation, secondLangRelation, true);
        }else

          deduce(yagoRelation, secondLangRelation, false);
        //          }
      }

      List<Fact> yagoFactsWithObject = myFactCollection.getFactsWithObject(FactComponent.forYagoEntity(yagoArg));
      for(Fact f: yagoFactsWithObject){
        String yagoRelation = f.getRelation();
        String yagoSubject = FactComponent.stripBrackets(f.getArg(1));
        String expectedDatatype = hardWiredFacts.getArg2(yagoRelation, RDFS.range);
        //assumption: all the subjects are entities
        if(isEntity(expectedDatatype)){
          if(isEqual(yagoSubject, secondLangObject, "<yagoGeoEntity>", preferredMeaning )/* && isEqualr(yagoObject, secondLangSubject)*/)
            deduce(yagoRelation, "<"+FactComponent.stripBrackets(secondLangRelation)+"->", true);
          else
            deduce(yagoRelation, "<"+FactComponent.stripBrackets(secondLangRelation)+"->", false);
        }
      }
    }

    Announce.progressDone();
    //TODO: set the thresholds here
    //WILSON_THRESHOLD = lang2FactSourceSize/1000; 
    //SUPPORT_THRESHOLD = lang2FactSourceSize /100;
    for (Entry<String, Map<String, Pair<Integer, Integer>>> entry : statistics.entrySet())
    {
      Map<String, Pair<Integer, Integer>> temp = entry.getValue(); 
      if(temp != null){
        for (Entry <String, Pair<Integer, Integer>> subEntry : temp.entrySet() ){
          int total =  subEntry.getValue().second;
          int correct = subEntry.getValue().first;
          double[] ws=  FrequencyVector.wilson(total, correct);

          if(correct > 0 ){
            //            Fact fact = new Fact(entry.getKey(), (double)correct /total + " <" +
            //               correct + "/" +total +">" + "     " +ws[0] + "    " + ws[1] , subEntry.getKey());
            /*filtering out */
//            if(ws[0] - ws[1] > WILSON_THRESHOLD  && correct> SUPPORT_THRESHOLD){
              Fact fact = new Fact(subEntry.getKey(),"<infoboxAttribute>", entry.getKey());

              write(writers, MATCHED_INFOBOXATTS_MAP.get(language), fact, MATCHEDATTSOURCES_MAP.get(language),
                  FactComponent.wikipediaURL(entry.getKey()),"");
//            }

            //            writers.get(MATCHED_INFOBOXATTS).write(new Fact(entry.getKey(),
            //               /*"<isEquivalentTo> " + */ "<"+subEntry.getValue().first+"/"+ subEntry.getValue().second+ "> "+
            //                    (float)subEntry.getValue().first/subEntry.getValue().second+""  , subEntry.getKey()));
            //            write(writers, MATCHED_INFOBOXATTS,new Fact(entry.getKey(), 
            //                (float)subEntry.getValue().first/subEntry.getValue().second+""  ,subEntry.getKey()), MATCHEDATTSources,
            //                FactComponent.wikipediaURL(entry.getKey()),
            //                "TODO");
          }
        }
      }
    }


  }
	
	public Fact flip(Fact f){
	  return new Fact(f.getArg(1), f.getRelation(), f.getArg(2));
	}


	private static synchronized ExtendedFactCollection getFactCollection(FactSource infoboxFacts) {
		if(yagoFactCollection!=null) return(yagoFactCollection);
		yagoFactCollection=new ExtendedFactCollection();
		//File f2 = new File("C:/Users/Administrator/data2/yago2s/");
		//FactSource yagoFactSource = FactSource.from(InfoboxMapper.INFOBOXFACTS_TOREDIRECT.file(f2)); 
		for(Fact f: infoboxFacts){
			yagoFactCollection.add(f);
		}
		return(yagoFactCollection);
	}
	
	 private static synchronized ExtendedFactCollection getFactCollection(File yagoFolder) throws FileNotFoundException, IOException {
	    if(yagoFactCollection!=null) return(yagoFactCollection);
	    yagoFactCollection=new ExtendedFactCollection();
	    for (File factsFile : yagoFolder.listFiles()) {
	      
	      if (factsFile.getName().endsWith("Facts.ttl")) {
	        N4Reader nr = new N4Reader(FileUtils.getBufferedUTF8Reader(factsFile));
	        while(nr.hasNext()){
	          Fact f = nr.next();
	          yagoFactCollection.add(f);
	        }
	 

	      }
	    }
	    return(yagoFactCollection);
	  }

	public boolean isEntity(String type){
		switch (type) {
	      case YAGO.entity:
	      case "rdf:Resource":
	      case "<yagoLegalActorGeo>":
	        return true;
		}
		return false;
	}
//	public  boolean isEqual(String target, String b){
//		if(dictionary.get(target)!= null && dictionary.get(target).equals(FactComponent.stripBrackets(b)))
//			return true;
//		return false;
//	}
	public  boolean isEqualr(String target, String b){
		if(rdictionary.get(FactComponent.stripBrackets(b))!= null && rdictionary.get(FactComponent.stripBrackets(b)).contains(target))
			return true;
		return false;
	}
	public boolean isEqual(String target, String b, String expectedDatatype, Map<String, String> preferredMeaning) throws IOException{
		TermExtractor termExtractor = expectedDatatype.equals(RDFS.clss) ? new TermExtractor.ForClass(
				preferredMeaning) : TermExtractor.forType(expectedDatatype);
		List<String> objects = termExtractor.extractList(preprocess(b));
		switch (expectedDatatype){
		
		case "xsd:date":	
			break;
		case "xsd:nonNegativeInteger":
		case "<m^2>":{
			break;
		}
		case "xsd:string": {
			break;
		}
		case "<yagoGeoEntity>":
		case "<yagoLegalActorGeo>":
		case "<yagoURL>":
		default:
			for(String s:objects){
				String temp = FactComponent.stripBrackets(s);
				if(rdictionary.get(temp)!=null && rdictionary.get(temp).contains(target))
//				if(temp.equals(target))
					return true;
			}
			return false; 
		}
		return false;

	}

	public void deduce(String yagoRelation, String secondLangRelation, boolean both) throws IOException{

	  secondLangRelation = preprocess(secondLangRelation);
		if(!statistics.containsKey(yagoRelation)){
			statistics.put(yagoRelation, new HashMap<String,Pair<Integer,Integer>>());
		}
		if(!statistics.get(yagoRelation).containsKey(secondLangRelation)){
			statistics.get(yagoRelation).put(secondLangRelation,new Pair<Integer, Integer>(0, 0));
		}
		statistics.get(yagoRelation).get(secondLangRelation).first+= both?1:0;
		statistics.get(yagoRelation).get(secondLangRelation).second+= 1;
	}

	public double stringSim(List<String> s1, String s2){

		Set<String> ngrams1=new HashSet<String> ();
		Set<String> ngrams2=new HashSet<String> ();
		for(String s: s1){
			ngrams1.addAll(buildNgrams(s));
		}

		ngrams2.addAll(buildNgrams(s2));
		return ((double)getIntersection(ngrams1, ngrams2).size())/getUnion(ngrams1, ngrams2).size();

	}

	public double numberSim(String n1, String n2){
		double result = 0;
		double num1= Float.parseFloat(n1);
		double num2=Float.parseFloat(n2);
		if( num1 == num2)
			result = 1;
		else
			result =(double) (0.75* Math.min(num1, num2) / Math.max(num1, num2));
		return result;
	}

	public double numberSetSim(List<String> s1, String s2){
		if(s1.size()==0)
			return 0;
		double sum = 0;
		for(String str: s1){ 
			sum+= numberSim(FactComponent.stripQuotes(FactComponent.getString(str)), 
					FactComponent.stripQuotes(FactComponent.getString(s2)));
		}
		
		return sum/s1.size();
	}

	public double dateSim(String s1, String s2){
		String[] date1 = s1.split("-");
		String[] date2 = s2.split("-");
		float sum=0;
		if(isNumeric(date1[0]) && isNumeric(date2[0]))
			sum = (Integer.parseInt(date1[0]) - Integer.parseInt(date2[0]))*365;
		if(isNumeric(date1[1]) && isNumeric(date2[1]))
			sum += (Integer.parseInt(date1[1]) - Integer.parseInt(date2[1]))*30 ;
		if(isNumeric(date1[2]) && isNumeric(date2[2]))
			sum += (Integer.parseInt(date1[1]) - Integer.parseInt(date2[1]));
		if(sum<366)
			return 1;

		return 0;


	}

	public HashSet<String> buildNgrams(String s){
		HashSet<String> ngrams = new HashSet<String> (); 
		for(int i= 2; i<5; i++){
			for(int j = 0; j<=s.length()-i; j++){
				ngrams.add(s.substring(j, j+i));
			}
		}
		return ngrams; 
	}

	public static Set<String> getIntersection(Set<String> set1, Set<String> set2) {
		boolean set1IsLarger = set1.size() > set2.size();
		Set<String> cloneSet = new HashSet<String>(set1IsLarger ? set2 : set1);
		cloneSet.retainAll(set1IsLarger ? set1 : set2);
		return cloneSet;
	}

	public static Set<String> getUnion(Set<String> set1, Set<String> set2){
		Set<String> cloneSet = new HashSet<String>(set1);
		cloneSet.addAll(set2);
		return cloneSet;
	}

	public static boolean isNumeric(String s){
		return  (Pattern.matches("[0-9.,]+", s)); 
	}

	public static Set<String> numberParser(String input){
		Set<String> numberPortions= new HashSet<String>();
		String result = "";
		for (int i = 0; i < input.length(); i++) {
			if(isNumeric( input.substring(i, i+1))){
				result+=input.charAt(i);
			}else{
				if((Pattern.matches("[0-9]+[.,][0-9]+", result)))
					numberPortions.add(result);
				result="";
			}
		}
		if(Pattern.matches("[0-9]+[.,][1-9]+", result))
			numberPortions.add(result);
		return numberPortions;
	} 

	public static String preprocess(String input) throws IOException{
	  if(input.contains("\n"))
	    input=input.replace("\n", "");
	  StringReader reader = new StringReader(input);
	  switch(FileLines.findIgnoreCase(reader, "<ref>", "<br />")){
	    case 0:
	      CharSequence temp = FileLines.readTo(reader, "</ref>");
	      input = input.replace("<ref>"+temp, "");
	      break;
	    case 1:
	      input = input.replace("<br />", "");
	      break;
	  }

	  for (int i = 1; i < input.length()-1; i++) 
	    if((input.charAt(i) == '.') && Character.isDigit(input.charAt(i-1)) && Character.isDigit(input.charAt(i+1)))
	      input = input.replace(".", "");

	  for (int i = 1; i < input.length()-1; i++) 
	    if((input.charAt(i) == ',') && Character.isDigit(input.charAt(i-1)) && Character.isDigit(input.charAt(i+1)))
	      input = input.replace(",", ".");

	  return input;
	}
	

  public AttributeMatcher(String secondLang) {
    language = secondLang;
  }
  

  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    String mylang= "de"; 
    //		new PatternHardExtractor(new File("C:/Users/Administrator/Dropbox/workspace/yago2s/data")).extract(new File("C:/Users/Administrator/Dropbox/data/yago2s/"+mylang), "test");
    //	    new HardExtractor(new File("C:/Users/Administrator/Dropbox/workspace/basics2s/data")).extract(new File("C:/Users/Administrator/Dropbox/data/yago2s/"+mylang), "test");
    //	    new WordnetExtractor(new File("C:/Users/Administrator/Dropbox/workspace/yago2s/data/wordnet")).extract(new File("C:/Users/Administrator/Dropbox/data/yago2s/"+mylang), "This time its gonna work!");


//    Announce.setLevel(Level.MESSAGES);
  
	new AttributeMatcher("de").extract(new File("D:/data3/yago2s"), "mapping infobox attributes in different languages");
	new InfoboxMapper("de").extract(new File("D:data3/yago2s/"), "test");
  } 


}
