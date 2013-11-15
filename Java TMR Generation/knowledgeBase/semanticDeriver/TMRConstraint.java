package knowledgeBase.semanticDeriver;

public class TMRConstraint {

	private String constraint;
	private Object reference;

	public TMRConstraint(String constraint, Object reference) {
		this.constraint = constraint;
		this.reference = reference;
	}

	public String getConstraint() {
		return constraint;
	}

	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}

	public Object getReference() {
		return reference;
	}

	public void setReference(Object reference) {
		this.reference = reference;
	}

	public String toString() {
		return constraint + reference;
	}
}
