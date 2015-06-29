package fromOtherSources;

import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import extractors.DataExtractor;
import extractors.Extractor;
import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.datatypes.FinalSet;
import utils.Theme;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Extracts some metadata about YAGO itself (sources, creation time, ...)
 *
 * @author Johannes Hoffart
 * 
 */
public class MetadataExtractor extends Extractor {

	/** Our output */
	public static final Theme METADATAFACTS = new Theme("metadataFacts",
			"The metadata facts of YAGO");

	@Override public Set<Theme> input() {
		return new HashSet<Theme>();
	}

	public Set<Theme> output() {
		return (new FinalSet<Theme>(METADATAFACTS));
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Storing metadata");

		List<String> wikipedias = Parameters.getList("wikipedias");
		for (String wikipedia : wikipedias) {
			File wikipediaFile = new File(wikipedia);
			String dumpName = wikipediaFile.getName();
			METADATAFACTS.write(
					new Fact(
						FactComponent.forString("WikipediaSource"), "<_yagoMetadata>",
						FactComponent.forString(dumpName)));
		}

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		String dateString = df.format(new Date());
		METADATAFACTS.write(
				new Fact(
						FactComponent.forString("CreationDate"),
						"<_yagoMetadata>",
						FactComponent.forString(FactComponent.forString(dateString))
				)
		);
	}
}
