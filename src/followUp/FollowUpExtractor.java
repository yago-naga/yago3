package followUp;

import java.util.Set;

import javatools.datatypes.FinalSet;
import basics.Theme;
import fromWikipedia.Extractor;
import fromWikipedia.MultilingualExtractor;

/** A Dummy class to indicate extractors that are called en suite */
public abstract class FollowUpExtractor extends MultilingualExtractor {

	/** This is the theme we want to check */
	protected Theme checkMe;

	/** This is the theme we produce */
	protected Theme checked;

	/** This is the theme we produce */
	protected Extractor parent;

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
}