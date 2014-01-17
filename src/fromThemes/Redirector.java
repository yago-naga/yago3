package fromThemes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.RedirectExtractor;
import fromWikipedia.Extractor.FollowUpExtractor;

/**
 * Takes the input Themes and checks if any of the entities
 * are actually a redirect and resolves them
 * 
 * @author Johannes Hoffart
 * 
 */
public class Redirector extends FollowUpExtractor {
	
	private String language;

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(checkMe, RedirectExtractor.RAWREDIRECTFACTS_MAP.get(this.language), PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Map<String, String> redirects = new HashMap<>();
		Announce.doing("Loading redirects");
		for (Fact f : input.get(RedirectExtractor.RAWREDIRECTFACTS_MAP.get(this.language))) {
		  redirects.put(FactComponent.forYagoEntity(FactComponent.asJavaString(f.getArg(2)).replace(' ','_')), f.getArg(1));		  
		}
		Announce.done();
		
		FactWriter out = output.get(checked);

		FactSource dirtyFacts = input.get(checkMe);

		Announce.doing("Applying redirects to facts");

		for (Fact dirtyFact : dirtyFacts) {
			Fact redirectedDirtyFact = redirectArguments(dirtyFact, redirects);
			out.write(redirectedDirtyFact);
		}
		Announce.done();
	}

	protected Fact redirectArguments(Fact dirtyFact, Map<String, String> redirects) {
		String redirectedArg1 = dirtyFact.getArg(1);
		if (redirects.containsKey(dirtyFact.getArg(1))) {
			redirectedArg1 = redirects.get(dirtyFact.getArg(1));
		}

		String redirectedArg2 = dirtyFact.getArg(2);
		if (redirects.containsKey(dirtyFact.getArg(2))) {
			redirectedArg2 = redirects.get(dirtyFact.getArg(2));
		}

		Fact redirectedFact = new Fact(redirectedArg1, dirtyFact.getRelation(), redirectedArg2);
		redirectedFact.makeId();

		return redirectedFact;
	}

	public Redirector(Theme in, Theme out, Extractor parent, String lang) {
		this.checkMe=in;
		this.checked=out;
		this.parent=parent;
		this.language = lang;
	}
	
	public Redirector(Theme in, Theme out, String lang) {
		this(in, out, null, lang);
	}
}
