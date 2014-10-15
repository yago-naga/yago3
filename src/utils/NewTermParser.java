package utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.Pair;
import javatools.parsers.Char17;
import javatools.parsers.DateParser;
import javatools.parsers.NumberParser;
import javatools.parsers.PlingStemmer;
import literalparser.Generator;
import literalparser.configuration.XMLConfiguration;
import literalparser.literal.Literal;
import literalparser.literal.Literal.BigDecimalLiteral;
import literalparser.literal.Literal.DateLiteral;
import literalparser.literal.Literal.QuantityValueLiteral;
import literalparser.literal.Literal.StringLiteral;
import literalparser.literal.LiteralFind;
import literalparser.parser.dag.DAGParser;
import literalparser.parser.dag.LinearGetLiteralsStrategy;
import basics.FactComponent;
import basics.RDFS;
import basics.YAGO;

/**
 * Class TermParser
 * 
 * Methods that extract entities from Wikipedia strings
 * 
 * @author Fabian M. Suchanek
 */
public class NewTermParser {

	/** Holds the theme of the parser patterns*/
	public final Theme theme;
	
	/** Holds the parser patterns*/
	public final PatternList patternList;
	
	public NewTermParser(Theme patterns) throws IOException {
		patternList=new PatternList(patterns,"mapsTo");	
		theme=patterns;
	}
		
	public List<String> extract(String input) {
		List<String> result=new ArrayList<>();
		if(input==null) return(result);
		StringBuffer buildi=new StringBuffer(input.length()*3);
		StringBuffer otherBuildi=new StringBuffer(input.length()*3);
		buildi.append(input);
		for (Pair<Pattern, String> pattern : patternList.patterns) {
			Matcher m=pattern.first.matcher(buildi);
			otherBuildi.setLength(0);
			int lastMatchPos=0;
			while(m.find()) {
				String replacement=pattern.second();
				while(lastMatchPos<m.start()) {
					otherBuildi.append(buildi.charAt(lastMatchPos++));
				}
				if(replacement.startsWith("<result:>")) {					
					result.add(replacement.substring(9));				
				} else {
					for(int i=0;i<replacement.length();i++) {
						if(replacement.charAt(i)!='$') {
							otherBuildi.append(replacement.charAt(i));
							continue;
						}
						int groupNum=replacement.charAt(i+1)-'0';
						String group=m.group(groupNum);
						if(group==null) group="";
					}									
				}
			}
			if(otherBuildi.length()>0) {
				m.appendTail(otherBuildi);
				StringBuffer b=buildi;
				buildi=otherBuildi;
				otherBuildi=b;
			}
		}		
		return(result);
	}
}
