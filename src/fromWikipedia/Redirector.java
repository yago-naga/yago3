package fromWikipedia;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.Extractor.FollowUpExtractor;



import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactCollection;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;

/**
 * Takes the input Themes and checks if any of the entities
 * are actually a redirect and resolves them
 * 
 * @author Johannes Hoffart
 * 
 */
public class Redirector extends FollowUpExtractor {

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(checkMe, RedirectExtractor.REDIRECTFACTS, PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		FactCollection redirectFacts = new FactCollection(input.get(RedirectExtractor.REDIRECTFACTS));
		Map<String, String> redirects = new HashMap<>();
		for (Fact f : redirectFacts) {
		  redirects.put(FactComponent.forYagoEntity(FactComponent.asJavaString(f.getArg(1))), f.getArg(2));
		}
		
		FactWriter out = output.get(checked);

		FactSource dirtyFacts = input.get(checkMe);

		Announce.doing("Applying redirects to facts");

		for (Fact dirtyFact : dirtyFacts) {
			Fact redirectedDirtyFact = redirectArguments(dirtyFact, redirects);
			out.write(redirectedDirtyFact);
		}
		Announce.done();
	}

	private Fact redirectArguments(Fact dirtyFact, Map<String, String> redirects) {
		String redirectedArg1 = dirtyFact.getArg(1);
		if (redirects.containsKey(dirtyFact.getArg(1))) {
			redirectedArg1 = redirects.get(dirtyFact.getArg(1));
		}

		String redirectedArg2 = dirtyFact.getArg(2);
		if (redirects.containsKey(dirtyFact.getArg(2))) {
			redirectedArg2 = redirects.get(dirtyFact.getArg(2));
		}

		Fact redirectedFact = new Fact(dirtyFact.getId(), redirectedArg1, dirtyFact.getRelation(), redirectedArg2);

		return redirectedFact;
	}

	public Redirector(Theme in, Theme out) {
		this.checkMe=in;
		this.checked=out;
	}

}
