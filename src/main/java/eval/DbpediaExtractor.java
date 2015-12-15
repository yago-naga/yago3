package eval;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import javatools.datatypes.FinalSet;
import utils.Theme;
import basics.Fact;
import basics.FactComponent;
import basics.FactSource;
import extractors.DataExtractor;

/** Produces YAGO facts from DBpedia, without mapping the predicates*/
public class DbpediaExtractor extends DataExtractor {

	public DbpediaExtractor(File input) {
		super(input);
	}

	@Override
	public Set<Theme> input() {
		return (Collections.emptySet());
	}

	public static final Theme DBPEDIAFACTS = new Theme("dbpediaFacts",
			"Facts of http://dbpedia.org, in YAGO format");

	@Override
	public Set<Theme> output() {
		return new FinalSet<>(DBPEDIAFACTS);
	}

	/**
	 * Translates instances 1:1 into the YAGO namespace, cleans literal
	 * datatypes
	 */
	public static String makeYago(String dbpedia) {
		if (FactComponent.isLiteral(dbpedia))
			return (dbpedia);
		return ("<" + FactComponent.stripPrefix(dbpedia) + ">");
	}

	@Override
	public void extract() throws Exception {
		for (Fact f : FactSource.from(inputData)) {
			DBPEDIAFACTS.write(new Fact(makeYago(f.getSubject()), f
					.getRelation(), makeYago(f.getObject())));
		}
	}

	public static void main(String[] args) throws Exception {
		new DbpediaExtractor(
				new File(
						"c:/fabian/data/dbpedia/mappingbased_properties_cleaned_en.ttl"))
				.extract(new File("c:/fabian/data/dbpedia/"), "blah");
	}
}
