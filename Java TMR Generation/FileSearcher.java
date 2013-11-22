
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
public class FileSearcher {

	private static void delay(int nanos) {
		try {
			Thread.sleep(nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

	private static String getJavaFileWithString(String search, File f)
			throws IOException {
		if (f.isDirectory()) {
			File[] fl = f.listFiles();
			for (int i = 0; i < fl.length; i++) {
				String ret = getJavaFileWithString(search, fl[i]);
				if (ret != null) {
					ret = ret.replace('\\', '.');
					return ret;
				}
			}
		} else {
			BufferedReader b = new BufferedReader(new FileReader(f));
			String s = null;
			while ((s = b.readLine()) != null) {
				if (s.contains(search)) {
					System.out.println(f.getPath());
					break;
				}
			}
			b.close();
		}
		return null;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String file = "C:/Users/Dwight/Desktop/Workspace/TMRMaker/";
		System.out.println(getJavaFileWithString("replace", new File(file)));
	}
}
