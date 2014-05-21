package fromOtherSources;

import java.io.File;
import java.util.Set;

import utils.Theme;
import utils.Theme.ThemeGroup;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactSource;

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
	public static final Theme WORDNETDOMAINHIERARCHY = new Theme(
			"yagoWordnetDomainHierarchy",
			"The hierarchy of WordNet Domains from http://wndomains.fbk.eu/hierarchy.html",
			ThemeGroup.LINK);

	public Set<Theme> output() {
		return (new FinalSet<Theme>(WORDNETDOMAINHIERARCHY));
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Copying wordnet domain hierarchy");
		for (Fact f : FactSource.from(new File(inputData,
				"_wordnetDomainHierarchy.ttl"))) {
			WORDNETDOMAINHIERARCHY.write(f);
		}
		Announce.done();
	}

	public WordnetDomainHierarchyExtractor(File inputFolder) {
		super(inputFolder);
	}

	public WordnetDomainHierarchyExtractor() {
		this(new File("./data/wordnetDomains"));
	}

	public static void main(String[] args) throws Exception {
		new WordnetDomainHierarchyExtractor(new File("./data")).extract(
				new File("c:/fabian/data/yago2s"), "test");
	}
}