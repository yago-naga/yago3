package utils.literalParsers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fromOtherSources.PatternHardExtractor;
import javatools.administrative.Announce;
import javatools.administrative.D;
import utils.PatternList;
import utils.TermParser;
import utils.Theme;
import basics.Fact;

/**
 * Class LiteralParser
 * 
 * Superclass for classes that extract literals from Wikipedia strings by help
 * of "mapsTo" patterns.
 * 
 * @author Fabian
 */
public abstract class LiteralParser extends TermParser {

	/** Holds the pattern that indicates a result */
	public static final Pattern resultPattern = Pattern
			.compile("_result_([^_]+)_([^_]*)_");

	/** Holds the pattern list */
	protected final PatternList patternList;

	/**
	 * Constructs a LiteralParser from a theme that contains patterns
	 * 
	 * @throws IOException
	 */
	protected LiteralParser(Theme patterns) throws IOException {
		patternList = new PatternList(patterns, "<mapsTo>");
	}

	/** Produces a result entity from a String */
	public abstract String resultEntity(Matcher resultMatch);

	@Override
	public List<String> extractList(String input) {
		input = patternList.transform(input);
		if (input == null)
			return (Collections.emptyList());
		List<String> result = new ArrayList<>();
		Matcher m = resultPattern.matcher(input);
		while (m.find()) {
			String resultEntity = resultEntity(m);
			if (resultEntity != null)
				result.add(resultEntity);
		}
		return (result);
	}

	/** Test one particular case */
	public void test(String expression) {
		Announce.setLevel(Announce.Level.DEBUG);
		D.p(extractList(expression));
		D.exit();
	}

	/** Test method */
	public static void main(String[] args) throws Exception {
		PatternHardExtractor.DATEPARSER.assignToFolder(new File("./data"));
		LiteralParser parsi = new DateParser();
		parsi.test("{{birth date and age|1954|11|10|df=y}}");
		Theme terms = new Theme("yagoInfoboxAttributes_new", "blah");
		terms.assignToFolder(new File("../../data/yago3"));
		for (Fact f : terms) {
			String obj = f.getObjectAsJavaString();
			D.p(obj, parsi.extractList(obj));
		}
	}
}
