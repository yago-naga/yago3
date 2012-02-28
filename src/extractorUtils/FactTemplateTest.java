package extractorUtils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({})
public class FactTemplateTest {
	@Test
	public void testMultiply() {
		List<FactTemplate> gold=Arrays.asList(new FactTemplate("<Elvis>", "rdf:txpe", "<theBest>"),new FactTemplate("#1", "rdf:type", "$1"));
		assertEquals("Result", gold, FactTemplate.create("<Elvis> rdf:type <theBest> ; #1 rdf:type $1"));
	}
}
