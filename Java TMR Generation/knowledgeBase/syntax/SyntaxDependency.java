package knowledgeBase.syntax;

import leia.parse.SentencePart;
import helpers.StringHelper;

/**
 * The syntax dependency structure. Built from Andy's specifications:<br>
 * <br>
 * case class Dependency( head:String, partOfSpeech:String,
 * subject:SentencePart, //Nullable; in "I want to ride", "ride" has no subject
 * modifiers:Array[SentencePart]) extends SentencePart
 * 
 * @author Dwight Naylor
 * @deprecated
 */
public class SyntaxDependency implements SentencePart {

	private static final String DIRECT_OBJECT = "direct object";
	private static final String SUBJECT = "subject";
	private final String head;
	private final String partOfSpeech;
	private final SentencePart[] modifiers;

	public SyntaxDependency(String head, String partOfSpeech,
			SentencePart[] modifiers) {
		this.head = head;
		this.partOfSpeech = partOfSpeech;
		this.modifiers = modifiers;
	}

	public boolean isDirectObject() {
		return head.equals(DIRECT_OBJECT);
	}

	public boolean isSubject() {
		return head.equals(SUBJECT);
	}

	public int getNumModifiers() {
		return modifiers.length;
	}

	public SentencePart getModifier(int index) {
		return modifiers[index];
	}

	public String toString() {
		StringBuffer ret = new StringBuffer("");
		ret.append("<" + getHead() + "|" + getPartOfSpeech() + ",");
		for (int i = 0; i < modifiers.length; i++) {
			if (i > 0) {
				ret.append(",");
			}
			ret.append(modifiers[i]);
		}
		ret.append(">");
		return ret.toString();
	}

	public static void printFancy(SyntaxDependency s) {
		StringBuffer ret = new StringBuffer(s.toString());
		int tabs = 0;
		for (int i = 0; i < ret.length(); i++) {
			if (ret.charAt(i) == '<') {
				ret.insert(i++, "\n");
				for (int q = 0; q < tabs; q++, i++) {
					ret.insert(i, "\t");
				}
				tabs++;
			}
			if (ret.charAt(i) == '>') {
				tabs--;
				if (ret.charAt(i - 1) == '>') {
					ret.insert(i++, "\n");
					for (int q = 0; q < tabs; q++, i++) {
						ret.insert(i, "\t");
					}
				}
			}
		}
		System.out.println(ret);
	}

	public String getHead() {
		return head;
	}

	public String getPartOfSpeech() {
		return partOfSpeech;
	}

	public SentencePart getSubject() {
		if (modifiers.length == 0) {
			return null;
		}
		if (!(modifiers[modifiers.length - 1] instanceof SyntaxDependency)
				|| !((SyntaxDependency) modifiers[modifiers.length - 1])
						.isSubject()) {
			return null;
		}
		return modifiers[modifiers.length - 1];
	}

	public SentencePart getDirectObject() {
		if (modifiers.length == 0) {
			return null;
		}
		if (!(modifiers[0] instanceof SyntaxDependency)
				|| !((SyntaxDependency) modifiers[0]).isDirectObject()) {
			return null;
		}
		return modifiers[0];
	}

	public static SyntaxDependency parse(String s) {
		if (s == null || s.equals("null")) {
			return null;
		}
		s = s.replace(" ", "").replace("\t", "").replace("\n", "")
				.replace("\r", "");
		int firstComma = s.indexOf(",");
		String head = s.substring(1, s.indexOf("|"));
		if (head.equals(DIRECT_OBJECT.replaceAll(" ", ""))) {
			head = DIRECT_OBJECT;
		}
		String pos = s.substring(s.indexOf("|") + 1, firstComma);
		String[] modifierStrings = StringHelper.splitOutOfArrows(
				s.substring(firstComma + 1, s.length() - 1), ',');
		SyntaxDependency[] modifiers = new SyntaxDependency[modifierStrings.length];
		for (int i = 0; i < modifiers.length; i++) {
			modifiers[i] = parse(modifierStrings[i]);
		}
		return new SyntaxDependency(head, pos, modifiers);
	}

	public static void main(String[] args) {
		System.out
				.println(parse("<QUESTION|role, <go|v, <to|prep, <Joe's Pizza|n, >>, <SUBJECT|role,<I|n, >>, <should|m, >>>"));
	}
}
