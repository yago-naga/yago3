package fromThemes;

import java.io.File;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.Theme;
import fromWikipedia.Extractor;

/**
 * Class MissingFacts - YAGO2S
 * 
 * Produces a theme that contains missing facts
 * 
 * @author Fabian M. Suchanek
 */

public class MissingFactExtractor extends Extractor {

	@Override
	public Set<Theme> input() {
		return new FinalSet<>(FactExtractor.YAGOFACTS,
				LiteralFactExtractor.YAGOLITERALFACTS,
				LabelExtractor.YAGOLABELS, MetaFactExtractor.YAGOMETAFACTS);
	}

	/** Comparison theme */
	public static final Theme MISSING_FACTS = new Theme(
			"missingFacts",
			"Facts that were there in the previous version of YAGO and that are no longer there.");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(MISSING_FACTS);
	}

	/** Folder where the old YAGO lives */
	protected final File oldYagoFolder;

	public MissingFactExtractor(File oldYagoFolder) {
		this.oldYagoFolder = oldYagoFolder;
	}

	@Override
	public void extract() throws Exception {
		for (Theme checkMe : input()) {
			Announce.doing("Checking", checkMe);
			Announce.doing("Loading old facts");
			FactCollection old = new FactCollection();
			int numFacts = 10000;
			for (Fact f : FactSource.from(checkMe.file(oldYagoFolder))) {
				if (f.getArg(1).startsWith("<wordnet_"))
					continue;
				if (f.getArg(1).endsWith("_language>"))
					continue;
				old.add(f);
				if (numFacts-- == 0)
					break;
			}
			Announce.done(old.size() + " facts");
			Announce.doing("Going through new facts");
			for (Fact f : checkMe.factSource()) {
				old.remove(f);
			}
			Announce.done(old.size() + " facts missing");
			MISSING_FACTS.write(new Fact(FactComponent.forTheme(checkMe),
					"rdf:type", "<follows>"));
			for (Fact f : old) {
				MISSING_FACTS.write(f);
			}
			Announce.done();
		}
	}

	public static void main(String[] args) throws Exception {
		new MissingFactExtractor(new File("c:/Fabian/temp/oldYago")).extract(
				new File("c:/Fabian/data/yago2s"), "test");
	}
}
