package knowledgeBase.semanticDeriver;

import java.util.ArrayList;
import java.util.Hashtable;

import knowledgeBase.syntax.DependencyVariable;
import leia.parse.SentencePart;

/**
 * A class representing a single "fact" about a syntax tree as it is parsed into
 * a semantic dependency. Note that the class is not meant to be copied and
 * "passed around", as it retains links to it's creator.
 * 
 * <br>
 * The following is a list of all unique identifiers with special meanings:<br>
 * child(x,y) is a fact representing that x is a child of y<br>
 * "&lttext&gt"(x) is for a tree x with &lttext&gt as its word<br>
 * &lttext&gt(x) is for a tree x with &lttext&gt as its POS<br>
 * 
 * @author Dwight Naylor
 */
public class SemanticFact {

	/**
	 * The theorem instance used to create
	 */
	private TMRTheoremInstance derivation;
	private final String type;
	private final ArrayList<SentencePart> participants;

	public SemanticFact(TMRTheoremInstance derivation, String type,
			ArrayList<SentencePart> participants) {
		this(type, participants);
		this.derivation = derivation;
	}

	public SemanticFact(String type, ArrayList<SentencePart> participants) {
		if (type.startsWith("\"") && type.endsWith("\"")) {
			type = type.toLowerCase();
		}
		this.type = type;
		this.participants = participants;
	}

	public SemanticFact(String type, SentencePart participant) {
		this.type = type;
		this.participants = new ArrayList<SentencePart>();
		participants.add(participant);
	}

	public String toString() {
		StringBuffer ret = new StringBuffer(type + "(");
		for (int i = 0; i < participants.size(); i++) {
			if (i > 0) {
				ret.append(",");
			}
			ret.append(participants.get(i).toString());
		}
		ret.append(")");
		return ret.toString();
	}

	public SemanticFact(TMRTheoremInstance derivation, String type,
			SentencePart participant) {
		this.derivation = derivation;
		this.type = type;
		this.participants = new ArrayList<SentencePart>();
		this.participants.add(participant);
	}

	public String getType() {
		return type;
	}

	public SentencePart getParticipant(int index) {
		return participants.get(index);
	}

	public int getNumParticipants() {
		return participants.size();
	}

	public static SemanticFact parse(String string,
			Hashtable<String, Integer> variableTable) {
		String[] input = string.substring(string.indexOf('(') + 1,
				string.indexOf(')')).split(",");
		ArrayList<SentencePart> participants = new ArrayList<SentencePart>();
		for (int i = 0; i < input.length; i++) {
			if (input[i].charAt(0) == '\"'
					&& input[i].charAt(input[i].length() - 1) == '\"') {
				if (variableTable.get(input[i]) == null) {
					System.err.println("Variable " + input[i]
							+ " was not recognized in theorem " + string);
					System.exit(0);
				}
				participants.add(new DependencyVariable(variableTable
						.get(input[i])));
			} else {
				participants.add(new DependencyVariable(variableTable
						.get(input[i])));
			}
		}
		String theorem = deleteOutOfBrackets(
				string.substring(0, string.indexOf('(')), ' ');
		return new SemanticFact(new TMRTheoremInstance(), theorem, participants);
	}

	private static String deleteOutOfBrackets(String string, char find) {
		StringBuffer ret = new StringBuffer(string);
		int bracketDepth = 0;
		boolean inQuotes = false;
		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (cur == '\"') {
				inQuotes = !inQuotes;
			}
			if (cur == '[') {
				bracketDepth++;
			} else if (cur == ']') {
				bracketDepth--;
			}
			if (bracketDepth == 0 && !inQuotes) {
				if (Character.isUpperCase(cur)) {
					ret.deleteCharAt(i);
					ret.insert(i, Character.toLowerCase(cur));
				}
				if (cur == find) {
					ret.deleteCharAt(i);
				}
			}
		}
		return ret.toString();
	}

	public TMRTheoremInstance getDerivation() {
		return derivation;
	}

	public void setDerivation(TMRTheoremInstance given) {
		this.derivation = given;
	}
}
