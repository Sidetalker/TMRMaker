package knowledgeBase.semanticDeriver;

import helpers.StringHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import knowledgeBase.syntax.DependencyVariable;
import leia.parse.SentencePart;

public class TMRTheorem {

	public static final TMRTheorem GIVEN = new TMRTheorem();
	int numParticipants;
	ArrayList<SemanticFact> causes;
	ArrayList<SemanticFact> effects;
	/**
	 * The list of candidates for each variable in this theorem. The key is the
	 * sentencepart being analyzed. The value is the list of all the facts that
	 * the candidate still has to satisfy.
	 */
	Hashtable<SentencePart, ArrayList<String>>[] candidates;
	Hashtable<SentencePart, ArrayList<SemanticFact>>[] candidateEvidence;
	HashSet<SentencePart>[] completedCandidates;
	/**
	 * A mapping from each key prefix (the "cat" in "cat0") to all of the
	 * variables in this theorem that need that string at some point. Note that
	 * this only contains single-variable clauses, IE "cat0" is ok, "cat0,1" is
	 * not.
	 */
	Hashtable<String, ArrayList<Integer>> keyMappings = new Hashtable<String, ArrayList<Integer>>();
	/**
	 * Lists of all of the keys that have to be fulfilled for each variable to
	 * be true.
	 */
	HashSet<String>[] reverseKeyMappings;
	String[] vars;
	private static Hashtable<String, Integer> variableTable;

	public String toString() {
		if (GIVEN == this) {
			return "Included in the syntax tree.";
		}
		StringBuffer ret = new StringBuffer();
		Iterator<String> variableIterator = variableTable.keySet().iterator();
		boolean firstVar = true;
		while (variableIterator.hasNext()) {
			if (!firstVar) {
				ret.append(", ");
			}
			firstVar = false;
			ret.append(variableIterator.next());
		}
		ret.append(" st ");
		for (int i = 0; i < causes.size(); i++) {
			if (i > 0) {
				ret.append(", ");
			}
			ret.append(causes.get(i).toString());
		}
		ret.append(" : ");
		for (int i = 0; i < effects.size(); i++) {
			if (i > 0) {
				ret.append(", ");
			}
			ret.append(effects.get(i).toString());
		}
		return ret.toString();
	}

	@SuppressWarnings("unchecked")
	public void initialize() {
		reverseKeyMappings = new HashSet[numParticipants];
		completedCandidates = new HashSet[numParticipants];
		candidates = new Hashtable[numParticipants];
		candidateEvidence = new Hashtable[numParticipants];
		for (int i = 0; i < numParticipants; i++) {
			reverseKeyMappings[i] = new HashSet<String>();
			completedCandidates[i] = new HashSet<SentencePart>();
			candidates[i] = new Hashtable<SentencePart, ArrayList<String>>();
			candidateEvidence[i] = new Hashtable<SentencePart, ArrayList<SemanticFact>>();
		}
		for (int i = 0; i < this.causes.size(); i++) {
			String type = causes.get(i).getType();
			for (int p = 0; p < causes.get(i).getNumParticipants(); p++) {
				if (!keyMappings.containsKey(type)) {
					keyMappings.put(type, new ArrayList<Integer>());
				}
				int index = ((DependencyVariable) causes.get(i).getParticipant(
						p)).getIndex();
				keyMappings.get(type).add(index);
				reverseKeyMappings[index].add(getKey(type, p));
			}
		}
	}

	/**
	 * Assembles a list of all the of the factual keys needed to engage this
	 * theorem.
	 */
	public ArrayList<String> getFactsNeeded() {
		ArrayList<String> ret = new ArrayList<String>();
		for (int i = 0; i < causes.size(); i++) {
			for (int q = 0; q < causes.get(i).getNumParticipants(); q++) {
				ret.add(getKey(causes.get(i).getType(), q));
			}
		}
		return ret;
	}

	public static TMRTheorem parse(String string) {
		TMRTheorem ret = new TMRTheorem();
		int suchThatIndex = string.indexOf("st");
		String varString = string.substring(0, suchThatIndex);
		ret.vars = varString.replace(" ", "").split(",");
		// Add all the variables to a reverse-lookup table
		variableTable = new Hashtable<String, Integer>();
		for (int i = 0; i < ret.vars.length; i++) {
			variableTable.put(ret.vars[i], i);
		}
		ret.numParticipants = ret.vars.length;
		// Parse in all of the causes
		ArrayList<String> causeStringList = StringHelper.splitOutOfGroupings(
				string.substring(suchThatIndex + 2, string.indexOf(":")), ',');
		ArrayList<SemanticFact> causes = new ArrayList<SemanticFact>();
		for (int i = 0; i < causeStringList.size(); i++) {
			causes.add(SemanticFact.parse(causeStringList.get(i).trim(),
					variableTable));
		}
		ret.causes = causes;
		// Make sure all of the given variables were used.
		checkCausesForVariableUsage(string, causes, ret.vars);
		// Parse in all of the effects
		ArrayList<String> effectStringList = StringHelper
				.splitOutOfGroupings(
						string.substring(string.indexOf(":") + 1,
								string.length()), ',');
		ArrayList<SemanticFact> effects = new ArrayList<SemanticFact>();
		for (int i = 0; i < effectStringList.size(); i++) {
			effects.add(SemanticFact.parse(effectStringList.get(i).trim(),
					variableTable));
		}
		ret.effects = effects;
		return ret;
	}

	private static void checkCausesForVariableUsage(String rule,
			ArrayList<SemanticFact> causes, String[] vars) {
		boolean[] found = new boolean[vars.length];
		for (int i = 0; i < causes.size(); i++) {
			for (int p = 0; p < causes.get(i).getNumParticipants(); p++) {
				found[((DependencyVariable) causes.get(i).getParticipant(p))
						.getIndex()] = true;
			}
		}
		boolean unusedVar = false;
		for (int i = 0; i < vars.length; i++) {
			if (!found[i]) {
				System.err.println("Unused variable " + vars[i] + " in rule "
						+ rule);
				unusedVar = true;
			}
		}
		if (unusedVar) {
			System.exit(0);
		}
	}

	/**
	 * Constructs an unfilled (filled with empty units) instance of this
	 * theorem.
	 */
	public TMRTheoremInstance constructInstance() {
		ArrayList<SentencePart> toUse = new ArrayList<SentencePart>();
		for (int i = 0; i < numParticipants; i++) {
			toUse.add(new DependencyVariable(i));
		}
		return constructInstance(toUse);
	}

	public TMRTheoremInstance constructInstance(
			ArrayList<SentencePart> applicationParts) {
		TMRTheoremInstance ret = new TMRTheoremInstance();
		ret.theorem = this;
		ret.application = new ArrayList<SemanticFact>();
		for (int i = 0; i < effects.size(); i++) {
			ArrayList<SentencePart> toUse = new ArrayList<SentencePart>();
			for (int q = 0; q < effects.get(i).getNumParticipants(); q++) {
				toUse.add(applicationParts.get(((DependencyVariable) effects
						.get(i).getParticipant(q)).getIndex()));
			}
			ret.application.add(new SemanticFact(ret, effects.get(i).getType(),
					toUse));
		}
		return ret;
	}

	public static String getPrefix(String key) {
		return key.substring(0, key.lastIndexOf("|"));
	}

	public static String getSuffix(String key) {
		return key.substring(key.lastIndexOf("|") + 1, key.length());
	}

	public static String getKey(String type, int varNum) {
		return type + "|" + varNum;
	}

	/**
	 * @param key
	 *            The key representing the fact that was found.
	 * @param fact
	 *            The fact that was found.
	 * @param index
	 *            The index of the variable to derive from in the given fact.
	 * @return A list of all of the instances that can be derived from the new
	 *         given fact.
	 */
	public ArrayList<TMRTheoremInstance> getDerivedInstances(String key,
			SemanticFact fact, int index) {
		ArrayList<TMRTheoremInstance> ret = new ArrayList<TMRTheoremInstance>();
		SentencePart part = fact.getParticipant(index);
		String keyPrefix = getPrefix(key);
		ArrayList<Integer> varNumList = keyMappings.get(keyPrefix);
		if (varNumList != null) {
			for (int i = 0; i < varNumList.size(); i++) {
				int varIndex = varNumList.get(i);
				if (!completedCandidates[varIndex].contains(part)) {
					if (candidates[varIndex].containsKey(part)) {
						candidates[varIndex].get(part).remove(key);
						candidateEvidence[varIndex].get(part).add(fact);
						if (candidates[varIndex].get(part).size() == 0) {
							completedCandidates[varIndex].add(part);
							getDerivedInstances(ret,
									new SentencePart[numParticipants], 0,
									varIndex, part);
						}
					} else {
						// Add the new list then backtrack the loop by one to
						// add the new string.
						candidates[varIndex].put(part, new ArrayList<String>());
						candidateEvidence[varIndex].put(part,
								new ArrayList<SemanticFact>());
						candidates[varIndex].get(part).addAll(
								reverseKeyMappings[varIndex]);
						i--;
					}
				}
			}
		}
		return ret;
	}

	private void getDerivedInstances(ArrayList<TMRTheoremInstance> list,
			SentencePart[] values, int curVarIndex, int newVarIndex,
			SentencePart part) {
		// BASE CASE: use the values accumulated to assert a new theorem
		// instance.
		if (curVarIndex == numParticipants) {
			// Before we can assert that the theorem has been satisfied, we have
			// to make sure that all of the multi-variable terms are satisfied.
			// Note that we will do this while we begin accumulating evidence.
			HashSet<TMRTheoremInstance> theoremEvidence = new HashSet<TMRTheoremInstance>();
			ArrayList<SemanticFact> causeListCopy = new ArrayList<SemanticFact>(
					causes);
			for (int i = 0; i < causeListCopy.size(); i++) {
				if (causeListCopy.get(i).getNumParticipants() == 1) {
					causeListCopy.remove(i--);
				}
			}
			// Go through all of the values and verify that they satisfy all of
			// the multiple-variable theorems.
			for (int i = 0; i < values.length; i++) {
				ArrayList<SemanticFact> facts = candidateEvidence[i]
						.get(values[i]);
				for (int f = 0; f < facts.size(); f++) {
					SemanticFact candidateFact = facts.get(f);
					if (candidateFact.getNumParticipants() != 1) {
						for (int c = 0; c < causeListCopy.size(); c++) {
							boolean satisfies = true;
							for (int q = 0; q < causeListCopy.get(c)
									.getNumParticipants(); q++) {
								if (candidateFact.getParticipant(q) != values[((DependencyVariable) causeListCopy
										.get(c).getParticipant(q)).getIndex()]) {
									satisfies = false;
									break;
								}
							}
							if (satisfies) {
								theoremEvidence.add(candidateFact
										.getDerivation());
								causeListCopy.remove(c--);
							}
						}
					}
				}
			}
			if (causeListCopy.size() > 0) {
				// If we haven't satisfied all the requirements, then this isn't
				// a valid application of the theorem.
				return;
			}
			// Then loop through and all of the unary constraints.
			for (int i = 0; i < values.length; i++) {
				ArrayList<SemanticFact> facts = candidateEvidence[i]
						.get(values[i]);
				for (int f = 0; f < facts.size(); f++) {
					if (facts.get(f).getNumParticipants() == 1) {
						theoremEvidence.add(facts.get(f).getDerivation());
					}
				}
			}
			ArrayList<TMRTheoremInstance> evidenceList = new ArrayList<TMRTheoremInstance>();
			Iterator<TMRTheoremInstance> iterator = theoremEvidence.iterator();
			while (iterator.hasNext()) {
				evidenceList.add(iterator.next());
			}
			TMRTheoremInstance newTheorem = new TMRTheoremInstance(evidenceList);
			newTheorem.theorem = this;
			newTheorem.application = new ArrayList<SemanticFact>();
			for (int i = 0; i < effects.size(); i++) {
				ArrayList<SentencePart> toUse = new ArrayList<SentencePart>();
				for (int q = 0; q < effects.get(i).getNumParticipants(); q++) {
					toUse.add(values[((DependencyVariable) effects.get(i)
							.getParticipant(q)).getIndex()]);
				}
				newTheorem.application.add(new SemanticFact(newTheorem, effects
						.get(i).getType(), toUse));
			}
			list.add(newTheorem);
			return;
		}
		// If we're trying to fill the variable that we just found, it only
		// makes sense to use the found value.
		if (curVarIndex == newVarIndex) {
			values[curVarIndex] = part;
			getDerivedInstances(list, values, curVarIndex + 1, newVarIndex,
					part);
			return;
		}
		Iterator<SentencePart> candidateIterator = completedCandidates[curVarIndex]
				.iterator();
		while (candidateIterator.hasNext()) {
			values[curVarIndex] = candidateIterator.next();
			getDerivedInstances(list, values, curVarIndex + 1, newVarIndex,
					part);
		}
	}
}
