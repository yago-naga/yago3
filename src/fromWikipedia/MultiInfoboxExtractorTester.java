package fromWikipedia;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;

public class MultiInfoboxExtractorTester extends Extractor{
	
//	fromWikipedia.MultiInfoboxExtractorTester

	private File 
	folder;
	static String[] languages;
	public static final HashMap<String, Theme> MULTIATTS= new HashMap<String, Theme>();
	public static final HashMap<String, Theme> MULTIATTSOURCES = new HashMap<String, Theme>();
	
	static {
	 languages = InfoboxExtractor.getAllLangs();
		for(String s:languages){
			MULTIATTS.put(s, new Theme("infoboxAtts_"+s, "Facts of infobox",ThemeGroup.OTHER));
			MULTIATTSOURCES.put(s, new Theme("infoboxAttSources_"+s, "Facts of infobox",ThemeGroup.OTHER));
		}

	}
	
	@Override
	public Set<Theme> input() {
		Set<Theme> result = new HashSet<Theme>(); 
		for (Entry<String, Theme> entry : InfoboxExtractor.INFOBOXATTS_MAP.entrySet())
			result.add(entry.getValue());
//		for (Entry<String, Theme> entry : InfoboxExtractor.INFOBOXATTSOURCES_MAP.entrySet())
//			result.add(entry.getValue());
		return result;
	}

	@Override
	public Set<Theme> output() {
		Set<Theme> result = new TreeSet<Theme>();
		for(String s:languages){
			result.add(MULTIATTS.get(s));
			result.add(MULTIATTSOURCES.get(s));
		}
		return result;
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers,
			Map<Theme, FactSource> input) throws Exception {
		
		languages = InfoboxExtractor.getAllLangs();
		System.out.println(languages);
//		for(String s: languages){
//			InfoboxExtractor ie = new InfoboxExtractor(new File("C:/Users/Administrator/data2/wikipedia/testset/"+s+ "_wikitest.xml"));
//			ie.extract(new File("C:/Users/Administrator/data2/yago2s/"), "one wikipedia article");
//			
//			for(Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(s))) {
//				//				System.out.println("______________________1");
//				//				System.out.println(writers);
//				//				System.out.println(MULTIATTS.get(s));
//				//				System.out.println(MULTIATTSOURCES.get(s));
//				//				System.out.println(f);
//				//				System.out.println(FactComponent.wikipediaURL(f.getArg(1)));
//				//				System.out.println("______________________2");
//				write(writers, MULTIATTS.get(s), f, MULTIATTSOURCES.get(s),
//						FactComponent.wikipediaURL(f.getArg(1)), "nicename");
//				Announce.done();
//				
//			}
//		
//		}
		for ( File file : testset(folder).listFiles()) {
			System.out.println(file.getName());
//		for(String s: languages){
			InfoboxExtractor ie = new InfoboxExtractor(file);
			ie.extract(inputFolder(folder), "one wikipedia article");
			
			String lang = ie.getLang();
			for(Fact f : input.get(InfoboxExtractor.INFOBOXATTS_MAP.get(lang))) {
				//				System.out.println("______________________1");
				//				System.out.println(writers);
				//				System.out.println(MULTIATTS.get(s));
				//				System.out.println(MULTIATTSOURCES.get(s));
				//				System.out.println(f);
				//				System.out.println(FactComponent.wikipediaURL(f.getArg(1)));
				//				System.out.println("______________________2");
				write(writers, MULTIATTS.get(lang), f, MULTIATTSOURCES.get(lang),
						FactComponent.wikipediaURL(f.getArg(1)), "nicename");
				Announce.done();
				
			}
	       
	    }
		
	}
	
	public MultiInfoboxExtractorTester(File folder) {
		this.folder = folder; 
	}

	private static File testset(File testCase) {
		for (File f : testCase.listFiles()) {
			if (f.isDirectory() && f.getName().equals("testset")) {
				return f;
			}
		}
		return null;
	}
	private static File inputFolder(File testCase) {
		for (File f : testCase.listFiles()) {
			if (f.isDirectory() && f.getName().equals("input")) {
				return f;
			}
		}
		return null;
	}


	public static void main(String[] args) throws Exception {
//		Extractor extractor = Extractor.forName("fromWikipedia.MultiInfoboxExtractorTester", null);
//		extractor.extract(new File("C:/Users/Administrator/data2/yago2s/"),
//				"blah blah");
		File f = new File("C:/Users/Administrator/Dropbox/workspace/yago2s/testCases/fromWikipedia.MultiInfoboxExtractorTester");
		new MultiInfoboxExtractorTester(f).
		extract( f, null);
	}
	

}
