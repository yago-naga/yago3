package fromOtherSources;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import basics.Theme.ThemeGroup;

/**
 * WordnetDomainHierarchyExtractor - YAGO2s
 * 
 * Copies the wordnet domain to the output folder
 * 
 * @author Fabian
 * 
 */
public class WordnetDomainHierarchyExtractor extends HardExtractor {

  /** Patterns of infoboxes */
  public static final Theme WORDNETDOMAINHIERARCHY = new Theme("yagoWordnetDomainHierarchy",
      "The hierarchy of WordNet Domains from http://wndomains.fbk.eu/hierarchy.html", ThemeGroup.LINK);

	public Set<Theme> output() {
		return (new FinalSet<Theme>(
		    WORDNETDOMAINHIERARCHY));
	}

	@Override
	public void extract(Map<Theme, FactWriter> writers, Map<Theme, FactSource> factCollections) throws Exception {
		Announce.doing("Copying wordnet domain hierarchy");
		for (Fact f : FactSource.from(new File(inputFolder,"_wordnetDomainHierarchy.ttl"))) {
      writers.get(WORDNETDOMAINHIERARCHY).write(f);
    }		
		Announce.done();
	}

	public WordnetDomainHierarchyExtractor(File inputFolder) {
		super(inputFolder);
		D.p(inputFolder.getAbsoluteFile());
	}
	
	public static void main(String[] args) throws Exception {
    new WordnetDomainHierarchyExtractor(new File("./data")).extract(new File("c:/fabian/data/yago2s"), "test");
  }
}