package knowledgeBase.semanticDeriver;

import java.util.Hashtable;
import java.util.Iterator;

public class TMR {

	public static final Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
	final String identifier;

	/**
	 * The properties for this tmr instance. The string
	 * <code>UNSET_STRING</code> means that it is unset.
	 */
	public final Hashtable<String, Object> properties = new Hashtable<String, Object>();
	private String questionString;
	final int index;

	public TMR(String identifier) {
		this.identifier = identifier;
		if (!indices.containsKey(identifier)) {
			indices.put(identifier, 0);
		}
		index = indices.get(identifier);
		indices.put(identifier, index + 1);
	}

	public String toString() {
		return identifier.toUpperCase() + "-" + index;
	}

	public void print() {
		System.out.println(this);
		Iterator<String> iterator = properties.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (key.equals(questionString)) {
				System.out.print("?????");
			}
			System.out.println("\t" + key + " : " + properties.get(key));
		}
		System.out.println();
	}

	public void addQuestionMark(String key) {

	}
}
