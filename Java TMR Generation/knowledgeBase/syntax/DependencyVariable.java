package knowledgeBase.syntax;

import leia.parse.SentencePart;

/**
 * The variables used to represent (internally) the different sentenceparts
 * represented in a theorem. For example, child(x,y) has two dependency
 * variables, x and y.
 * 
 * @author Dwight Naylor
 */
public class DependencyVariable implements SentencePart {

	private final int index;

	public DependencyVariable(int index) {
		this.index = index;
	}

	public boolean equals(Object o) {
		return o instanceof DependencyVariable
				&& ((DependencyVariable) o).index == index;
	}

	public int getIndex() {
		return index;
	}

	public String toString() {
		return index + "";
	}
}
