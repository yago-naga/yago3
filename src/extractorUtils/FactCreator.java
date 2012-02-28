package extractorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import basics.Fact;
import basics.FactComponent;

/**
 * YAGO2s - FactCreator
 * 
 * This class can instantiate a template of the form "S P O; S P O; ..." ...where
 * the components can be (1) constants (2) variables of the form $i (3) fact
 * references of the form #i (4) typed variables of the form @XXX($i), where XXX
 * is String, Url, WikiLink, Date or Number
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactCreator {
	/** Reads facts from a fact template*/
	public static List<FactTemplate> createFacts(String factTemplates) {
		List<FactTemplate> factList = new ArrayList<>();
		for (String factTemplate : factTemplates.split(";")) {
			factTemplate = factTemplate.trim();
			if(factTemplate.length()==0) continue;
			factTemplate+=' ';
			String[] split = new String[3];
			int argNum = 0;
			int pos = 0;
			while (argNum < 3) {
				while (factTemplate.charAt(pos) == ' ')
					if (++pos >= factTemplate.length())
						throw new RuntimeException("Template must have 3 components: "+
								factTemplate);
				if (factTemplate.charAt(pos) == '"') {
					int endPos = factTemplate.indexOf('"', pos + 1);
					if (endPos == -1)
						throw new RuntimeException("Closing quote is missing in: "+
								factTemplate);
					split[argNum] = factTemplate.substring(pos, endPos + 1);
					pos = endPos + 1;
				} else if (factTemplate.charAt(pos) == '\'') {
					int endPos = factTemplate.indexOf('\'', pos + 1);
					if (endPos == -1)
						throw new RuntimeException("Closing quote is missing in: "+
								factTemplate);
					split[argNum] = factTemplate.substring(pos+1, endPos);
					pos = endPos + 1;
				} else {
					int endPos = factTemplate.indexOf(' ', pos + 1);
					split[argNum] = factTemplate.substring(pos, endPos);
					pos = endPos + 1;
				}
				argNum++;
			}
			if (pos != factTemplate.length())
				throw new RuntimeException("Too many components in template:"+ factTemplate);
			FactTemplate result = new FactTemplate(split[0],split[1],split[2]);
			if (result.arg1.startsWith("#")) {
				if (result.arg1.length() != 2)
					throw new RuntimeException(
									"A template list can only contain template references of the form #x: "+
									factTemplate);
				int factId = result.arg1.charAt(1) - '0';
				if (factId < 1 || factId > factList.size())
					throw new RuntimeException(
									"#x in a template can only refer to preceding templates by their id, 1-based: "+
									factTemplate);
			}
			factList.add(result);
		}
		return (factList);
	}
	
	/** Instantiates fact templates with variables*/
	public static List<Fact> instantiate(List<FactTemplate> templates, Map<String,String> variables) {
		List<Fact> factList = new ArrayList<Fact>();
		Set<Integer> factReferences=new TreeSet<>();
		for(FactTemplate template : templates) {
			template.addFactReferencesTo(factReferences);
		}
		for(int i=0;i<templates.size();i++) {
			String id=null;
			if(factReferences.contains(i)) {
				id=FactComponent.makeId();
				variables.put("#"+i, id);
			}
			factList.add(templates.get(i).instantiate(variables,id));
		}		
		return(factList);
	}
}
