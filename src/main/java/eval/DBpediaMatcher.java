package eval;

import java.io.File;

import fromThemes.AttributeMatcher.CustomAttributeMatcher;
import fromThemes.InfoboxMapper;

/** Matches DBpedia predicates to YAGO predicates */
public class DBpediaMatcher extends CustomAttributeMatcher {

	public DBpediaMatcher() {
		super(DbpediaExtractor.DBPEDIAFACTS, InfoboxMapper.INFOBOXFACTS
				.inEnglish(), "dbp");
	}

	public static void main(String[] args) throws Exception {
		new DBpediaMatcher().extract(new File("C:/fabian/data/yago3"), "test");
	}
}
