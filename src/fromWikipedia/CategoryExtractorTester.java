package fromWikipedia;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fromOtherSources.HardExtractor;
import fromOtherSources.InterLanguageLinks;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * CategoryExtractorTester - YAGO2s
 * 
 * Tests CategoryExtractor for different languages
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */
public class CategoryExtractorTester extends Extractor{

	private File inputFolder;
	
	@Override
	public Set<Theme> input() {

    HashSet<Theme> result = new HashSet<Theme>( 
        Arrays.asList(PatternHardExtractor.CATEGORYPATTERNS, 
        PatternHardExtractor.TITLEPATTERNS,
        WordnetExtractor.WORDNETWORDS));
    return result;
  
	}

	@Override
	public Set<Theme> output() {
		Set<Theme> result = new TreeSet<Theme>();
		for(String s:Extractor.languages){
			result.add(CategoryExtractor.CATEGORYMEMBERSHIP_MAP.get(s));
			result.add(CategoryExtractor.CATEGORYMEMBSOURCES_MAP.get(s));
		}
		return result;
	}

	@Override
	public void extract(File inputFolder, File outputFolder, String header) throws Exception {
	  for(String s: Extractor.languages){
	    CategoryExtractor ce = new CategoryExtractor(getInputFile(dataFolder(inputFolder), s));
	    ce.extract(inputFolder, header);
	  }

	}

	public CategoryExtractorTester(File folder) {
	  this.inputFolder = folder;
  }
	public CategoryExtractorTester() {
	}
public File getInputFile(File inputFolder, String lang){
	File[] listoffiles = inputFolder.listFiles();
  for(File f: listoffiles){
    if(f.getName().startsWith(lang))
      return f;
  }
  return null;
    
}
private static File dataFolder(File testCase) {
  for (File f : testCase.listFiles()) {
    if (f.isDirectory() && f.getName().equals("data")) {
      return f;
    }
  }
  return null;
}
	 
	public static void main(String[] args) throws Exception {
//		Extractor extractor = Extractor.forName("fromWikipedia.MultiInfoboxExtractorTester", null);
//		extractor.extract(new File("C:/Users/Administrator/data2/yago2s/"),
//				"blah blah");
		File f =  new File("D:/data2/yago2s/input");
		new CategoryExtractorTester().extract( new File("D:/data2/yago2s"), "");
	}

  @Override
  public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
    // TODO Auto-generated method stub
    
  }
	

}
