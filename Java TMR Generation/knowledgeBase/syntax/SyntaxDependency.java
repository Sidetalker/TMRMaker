package knowledgeBase.syntax;

import helpers.StringHelper;

/**
 * The syntax dependency structure. Built from Andy's specifications:<br>
 * <br>
 * case class Dependency( head:String, partOfSpeech:String,
 * subject:SentencePart, //Nullable; in "I want to ride", "ride" has no subject
 * modifiers:Array[SentencePart]) extends SentencePart
 * 
 * @author Dwight Naylor
 */
public class SyntaxDependency implements SentencePart {

	public static final String DIRECT_OBJECT = "direct object";
	private final String head;
	private final String partOfSpeech;
	private final SentencePart subject;
	final SentencePart[] modifiers;

	public SyntaxDependency(String head, String partOfSpeech,
			SentencePart subject, SentencePart[] modifiers) {
		this.head = head;
		this.partOfSpeech = partOfSpeech;
		this.subject = subject;
		this.modifiers = modifiers;
	}

	public boolean isDirectObject() {
		return head.equals(DIRECT_OBJECT);
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
		ret.append(", " + getSubject() + ">");
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
		SyntaxDependency[] modifiers = new SyntaxDependency[modifierStrings.length - 1];
		for (int i = 0; i < modifiers.length; i++) {
			modifiers[i] = parse(modifierStrings[i]);
		}
		return new SyntaxDependency(head, pos,
				parse(modifierStrings[modifiers.length]), modifiers);
	}

	public String getHead() {
		return head;
	}

	public String getPartOfSpeech() {
		return partOfSpeech;
	}

	public SentencePart getSubject() {
		return subject;
	}
}
