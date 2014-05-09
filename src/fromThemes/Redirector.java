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
import basics.Theme;
import followUp.FollowUpExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import fromWikipedia.Extractor;
import fromWikipedia.RedirectExtractor;

/**
 * Takes the input Themes and checks if any of the entities are actually a
 * redirect and resolves them
 * 
 * @author Johannes Hoffart
 * 
 */
public class Redirector extends FollowUpExtractor {

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(checkMe,
				RedirectExtractor.REDIRECTFACTS_DIRTY.inLanguage(this.language),
				PatternHardExtractor.TITLEPATTERNS,
				WordnetExtractor.WORDNETWORDS));
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(checked);
	}

	@Override
	public Set<Theme> inputCached() {
		return new FinalSet<>(
				RedirectExtractor.REDIRECTFACTS_DIRTY.inLanguage(this.language));
	}

	@Override
	public void extract() throws Exception {
		// Extract the information
		Map<String, String> redirects = new HashMap<>();
		Announce.doing("Loading redirects");
		for (Fact f : RedirectExtractor.REDIRECTFACTS_DIRTY.inLanguage(
				this.language).factCollection()) {
			redirects.put(
					FactComponent.forYagoEntity(FactComponent.asJavaString(
							f.getArg(2)).replace(' ', '_')), f.getArg(1));
		}
		Announce.done();

		Announce.doing("Applying redirects to facts");
		for (Fact dirtyFact : checkMe.factSource()) {
			Fact redirectedDirtyFact = redirectArguments(dirtyFact, redirects);
			checked.write(redirectedDirtyFact);
		}
		Announce.done();
	}

	protected Fact redirectArguments(Fact dirtyFact,
			Map<String, String> redirects) {
		String redirectedArg1 = dirtyFact.getArg(1);
		if (redirects.containsKey(dirtyFact.getArg(1))) {
			redirectedArg1 = redirects.get(dirtyFact.getArg(1));
		}

		String redirectedArg2 = dirtyFact.getArg(2);
		if (redirects.containsKey(dirtyFact.getArg(2))) {
			redirectedArg2 = redirects.get(dirtyFact.getArg(2));
		}

		Fact redirectedFact = new Fact(redirectedArg1, dirtyFact.getRelation(),
				redirectedArg2);
		redirectedFact.makeId();

		return redirectedFact;
	}

	public Redirector(Theme in, Theme out, Extractor parent, String lang) {
		this.checkMe = in;
		this.checked = out;
		this.parent = parent;
		this.language = lang;
	}

	public Redirector(Theme in, Theme out, String lang) {
		this(in, out, null, lang);
	}
}
