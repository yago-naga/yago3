package fromThemes;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import basics.Fact;
import deduplicators.PriorityDateExtractor;
import extractors.Extractor;
import javatools.datatypes.FinalSet;
import utils.Theme;

/**
 * Extracts age of people, based on birth and death dates.
 * 
 * @author Thomas Rebele
 */
public class AgeExtractor extends Extractor {

  /** Gives ages of people */
  public static final Theme AGE = new Theme("age", "Age of people, in years");

  /** Unrealistic ages (> threshold) */
  public static final Theme AGEERROR = new Theme("ageErrors", "Age of people, in years");

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(PriorityDateExtractor.YAGODATEFACTS);
  }

  @Override
  public Set<Theme> output() {
    return new FinalSet<>(AGE, AGEERROR);
  }

  @Override
  public void extract() throws Exception {

    Map<String, String> birthdates = new HashMap<>(), deathdates = new HashMap<>();

    for (Theme theme : input()) {
      for (Fact f : theme) {
        if ("<wasBornOnDate>".equals(f.getRelation())) {
          birthdates.put(f.getSubject(), stripType(f.getObject()));
        }
        if ("<diedOnDate>".equals(f.getRelation())) {
          deathdates.put(f.getSubject(), stripType(f.getObject()));
        }
      }
    }

    for (Entry<String, String> entity : birthdates.entrySet()) {
      if (!hasYear(entity.getValue())) continue;

      String dod = deathdates.get(entity.getKey());
      if (hasYear(dod)) {
        int age = yearsDiff(entity.getValue(), dod);
        if (age < 0 || age > 150) {
          AGEERROR.write(new Fact(entity.getKey(), "<hasAgeInYears>", "" + age + " dob: " + entity.getValue() + " dod: " + dod));
        } else {
          AGE.write(new Fact(entity.getKey(), "<hasAgeInYears>", "" + age));
        }
      }
    }
  }

  private static String stripType(String str) {
    return str.replaceAll("\"", "").replaceAll("^^xsd:date", "");
  }

  /** ISO date format */
  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

  /** Take a string, such as "12##-##-##", and convert it to a date */
  private static Date yagoToDate(String str) {
    str = str.replaceAll("##", "00");
    try {
      return sdf.parse(str);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static boolean hasYear(String date) {
    if (date == null || date.length() < 2) return false;
    int pos = date.indexOf('-', date.charAt(0) == '-' ? 1 : 0);
    if (pos > 0 && Character.isDigit(date.charAt(pos - 1))) {
      return true;
    }
    return false;
  }

  /** Calculate the difference between two dates in years */
  private static int yearsDiff(String start, String end) {
    LocalDate startDate = yagoToDate(start).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate endDate = yagoToDate(end).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    Period p = Period.between(startDate, endDate);
    return p.getYears();
  }

  public static void main(String[] args) throws Exception {
    new AgeExtractor().extract(new File("/san/suchanek/yago3-2017-02-20/"), "DEBUG");
  }

}
