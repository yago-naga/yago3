package followUp;

import java.util.Set;

import utils.Theme;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import extractors.Extractor;

/** A Dummy class to indicate extractors that are called en suite */
public abstract class FollowUpExtractor extends Extractor {

	/** This is the theme we want to check */
	protected final Theme checkMe;

	/** This is the theme we produce */
	protected final Theme checked;

	/** Points to whoever created us */
	protected final Extractor parent;

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(checked);
	}

	@Override
	public String name() {
		if (parent != null) {
			return String.format("%s:%s", super.name(), parent.name());
		} else {
			return super.name();
		}
	}

	protected FollowUpExtractor(Theme in, Theme out, Extractor parent) {
		checkMe = in;
		checked = out;
		this.parent = parent;
	}

	/** Creates an extractor given by name */
	public static FollowUpExtractor forName(Class<FollowUpExtractor> className,
			Theme in, Theme out) {
		Announce.doing("Creating extractor", className + "(" + in + ", " + out
				+ ")");
		FollowUpExtractor extractor = null;
		try {
			extractor = className.getConstructor(Theme.class, Theme.class,
					Extractor.class).newInstance(in, out, null);

		} catch (Exception ex) {
			Announce.error(ex);
		}
		Announce.done();
		return (extractor);
	}

}