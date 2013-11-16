package knowledgeBase.syntax;

import leia.parse.SentencePart;

/**
 * Used to point to a specific tmr from the ontology.
 * 
 * @author Dwight Naylor
 */
public class TMRReference implements SentencePart {

	private final String reference;
	private final String tmrKey;

	public TMRReference(String reference, String tmrKey) {
		this.reference = reference;
		this.tmrKey = tmrKey;
	}

	public int hashCode() {
		return (reference + tmrKey).hashCode();
	}

	public boolean equals(Object o) {
		return o instanceof TMRReference
				&& ((TMRReference) o).reference.equals(reference)
				&& ((TMRReference) o).tmrKey.equals(tmrKey);
	}

	public String getReference() {
		return reference;
	}

	public String getTmrKey() {
		return tmrKey;
	}
}
