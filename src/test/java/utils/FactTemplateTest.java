package utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import basics.Fact;
import javatools.administrative.D;

/**
 * Test cases for FactTemplate class
 * @author Thomas Rebele
 */
public class FactTemplateTest {

  /** Helper to create map in one line */
  private static Map<String, String> map(String... mapping) {
    Map<String, String> m = new HashMap<>();
    for (int i = 0; i < mapping.length; i += 2) {
      m.put(mapping[i], mapping[i + 1]);
    }
    return m;
  }

  /** Check two lists for equality and print useful message */
  private static <T, R> void checkEquality(List<T> expected, List<R> actual) {
    org.junit.Assert.assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      T expItem = expected.get(i);
      R actItem = actual.get(i);
      if (!D.equal(expItem, actItem)) {
        org.junit.Assert.fail("lists differ at position " + i + " expected " + expItem + " but was: " + actItem);
      }
    }
  }

  @Test
  public void test() {
    // multi-fact templates with meta-fact references
    List<FactTemplate> l;
    String t = "$0 <relation> $1; #1 <refOne> $2; #2 <refTwo> $3; #1 <refOne> $4; #2 <refTwo> $5";
    l = FactTemplate.create(t);
    Map<String, String> m = map("$0", "zero", "$1", "one", "$2", "two", "$3", "three", "$4", "four", "$5", "five");

    // create expected facts
    Fact f1 = new Fact("<zero>", "<relation>", "<one>");
    f1.makeId();
    Fact f2 = new Fact(f1.getId(), "<refOne>", "<two>");
    f2.makeId();
    Fact f3 = new Fact(f2.getId(), "<refTwo>", "<three>");
    Fact f4 = new Fact(f1.getId(), "<refOne>", "<four>");
    Fact f5 = new Fact(f2.getId(), "<refTwo>", "<five>");
    List<Fact> expected = Arrays.asList(f1, f2, f3, f4, f5);

    // check
    List<Fact> actual = FactTemplate.instantiate(l, m);
    checkEquality(expected, actual);

    actual = FactTemplate.instantiate(FactTemplate.instantiatePartially(l, m), m);
    checkEquality(expected, actual);
  }
}
