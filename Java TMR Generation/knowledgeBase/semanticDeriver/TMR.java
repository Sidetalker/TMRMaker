package knowledgeBase.semanticDeriver;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import leia.parse.SentencePart;

public class TMR {

	public static Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
	private String identifier;

	/**
	 * The properties for this tmr instance. The string
	 * <code>UNSET_STRING</code> means that it is unset.
	 */
	public final Hashtable<String, Object> properties = new Hashtable<String, Object>();
	private final HashSet<String> legalProperties = new HashSet<String>();
	int index;
	private String markedProperty;
	private String goalFact;

	public TMR(Deriver deriver, String identifier) {
		this.identifier = identifier;
		if (!indices.containsKey(identifier)) {
			indices.put(identifier, 0);
		}
		index = indices.get(identifier);
		indices.put(identifier, index + 1);
		@SuppressWarnings("unchecked")
		Iterator<String> propertyIterator = deriver.getTMRType(identifier)
				.keySet().iterator();
		while (propertyIterator.hasNext()) {
			legalProperties.add(propertyIterator.next());
		}
	}

	public void changeTMRType(String newType) {
		if (newType.equals(identifier)) {
			return;
		}
		this.identifier = newType;
		if (!indices.containsKey(identifier)) {
			indices.put(identifier, 0);
		}
		index = indices.get(identifier);
	}

	public String toString() {
		return getIdentifier().toUpperCase() + "-" + index;
	}

	public void print() {
		System.out.println(this);
		Iterator<String> iterator = properties.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (key.equals(goalFact)) {
				System.out.print("<?>");
			}
			System.out.println("\t" + key + " : " + properties.get(key));
		}
		System.out.println();
	}

	public void setMarker(String propToMark) {
		this.markedProperty = propToMark;
	}

	public void clearMarker(SentencePart markerValue) {
		properties.put(markedProperty, markerValue);
	}

	public boolean isLegalProperty(String property) {
		return legalProperties.contains(property);
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setGoalFact(String goalProperty) {
		goalFact = goalProperty;
	}
}
