
import java.awt.AWTException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

/**
 * A class used to analyze random things
 * 
 * @author Dwight Naylor
 * 
 */
@SuppressWarnings("unused")
public class FileDataCollector {

	private static int emptyBracketLines;

	static int javaDocLines = 0;
	static int linesWithComments = 0;
	static int totalLines = 0;

	private static void delay(int nanos) {
		try {
			Thread.sleep(nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static String getAllLinesContaining(File f) throws IOException {
		if (f.isDirectory()) {
			File[] fl = f.listFiles();
			for (int i = 0; i < fl.length; i++) {
				String ret = getAllLinesContaining(fl[i]);
				if (ret != null) {
					ret = ret.replace('\\', '.');
					return ret;
				}
			}
		} else {
			if (f.getName().substring(f.getName().length() - 4)
					.contains("java")) {
				BufferedReader b = new BufferedReader(new FileReader(f));
				String s = null;
				boolean in = false;
				while ((s = b.readLine()) != null) {
					totalLines++;
					if (s.contains("}") && !s.contains("else")) {
						emptyBracketLines++;
					}
					if (s.contains("//")) {
						linesWithComments++;
					}
					if (s.contains("/**")) {
						in = true;
					}
					if (in && s.contains("*")) {
						javaDocLines++;
					}
					if (s.contains("*/")) {
						in = false;
					}
				}
				b.close();
			}
		}
		return null;
	}

	private static String getFile(String name, File f) {
		if (f.isDirectory()) {
			File[] fl = f.listFiles();
			for (int i = 0; i < fl.length; i++) {
				String ret = getFile(name, fl[i]);
				if (ret != null) {
					ret = ret.replace('\\', '.');
					return ret;
				}
			}
		} else {
			if (f.getName().substring(0, f.getName().length() - 5)
					.equalsIgnoreCase(name)) {
				String ret = f.getParent() + "\\"
						+ f.getName().substring(0, f.getName().length() - 5);
				return ret;
			}
		}
		return null;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		File f = new File("knowledgeBase/semanticDeriver");
		String p = f.getAbsolutePath();
		p = p.substring(0, p.lastIndexOf("\\"));
		getAllLinesContaining(new File(p));
		System.out.println("total lines : " + totalLines);
		System.out.println("javadoc lines : " + javaDocLines);
		System.out.println("empty bracket lines : " + emptyBracketLines);
		System.out.println("lines with comments : " + linesWithComments);
	}
}
