package utils;

import java.io.File;
import java.io.Reader;

import javatools.filehandlers.FileLines;
import javatools.filehandlers.UTF8Writer;
import javatools.util.FileUtils;

/**
 * Finds Wikipedia page from the article. Can be used for debugging, when a
 * certain page is required.
 * 
 * @author edwin
 * 
 */

public class WikipediaPageExtractor {

	private String inputFilePath = "C:/yago_archives/enwiki-20120502-pages-articles.xml";

	// private String title = "Carmella Bing";

	private String title = "Hadzi Ahmeta Dukatar's Mosque";

	public void find() {
		boolean found = false;
		long time = System.currentTimeMillis();
		try {
			Reader in = FileUtils.getBufferedUTF8Reader(inputFilePath);
			FileLines.readTo(in, "<mediawiki");
			String page = null;
			while (true) {
				page = FileLines.readBetween(in, "<page>", "</page>");
				if (page == null) {
					break;
				}
				String titleEntity = FileLines.readBetween(page, "<title>",
						"</title>");
				if (titleEntity != null && titleEntity.equalsIgnoreCase(title)) {
					found = true;
					break;
				} else {
					page = null;
				}
			}
			File inFile = new File(inputFilePath);
			File f = new File(inFile.getParentFile(), title.replace(' ', '_')
					+ ".xml");
			UTF8Writer writer = new UTF8Writer(f);
			writer.write(pagestart);
			writer.write(page.trim());
			writer.write(pageend);
			writer.flush();
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("DONE in " + time + "ms, found " + found);
	}

	private final static String pagestart = "<mediawiki xmlns=\"http://www.mediawiki.org/xml/export-0.4/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.mediawiki.org/xml/export-0.4/ http://www.mediawiki.org/xml/export-0.4.xsd\" version=\"0.4\" xml:lang=\"en\">\n<page>\n";

	private final static String pageend = "\n</page>\n</mediawiki>";

	public static void main(String[] args) {
		new WikipediaPageExtractor().find();
	}

}
