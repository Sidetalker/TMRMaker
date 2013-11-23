package knowledgeBase.semanticDeriver;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import leia.parse.SentencePart;

public class TMR {

	/**
	 * Static table keeping track of how many of each type of tmr exist. Used
	 * for indexing the tmrs.
	 */
	public static Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
	/**
	 * The identifier of this tmr (the "human" in "human-0"). This is the same
	 * thing as the type of the tmr.
	 */
	private String identifier;
	/**
	 * The properties for this tmr instance.
	 */
	public final Hashtable<String, Object> properties = new Hashtable<String, Object>();
	/**
	 * A set of all the properties that this tmr is allowed to have. If a
	 * property is set that is not on this list, an error will be thrown (note
	 * that this should never happen due to the conflict resolver).
	 */
	private final HashSet<String> legalProperties = new HashSet<String>();
	/**
	 * The index for this tmr. The "0" in "human-0"
	 */
	int index;
	/**
	 * The property that is marked to be filled by the first de-marker tmr
	 * setter.
	 */
	private String markedProperty;
	/**
	 * The property name that is marked as "needing to be known" in the tmr.
	 */
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
			System.out.print("\t");
			if (key.equals(goalFact)) {
				System.out.print("<?>");
			}
			System.out.println(key + " : " + properties.get(key));
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
