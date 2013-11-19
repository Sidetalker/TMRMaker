package knowledgeBase.semanticDeriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import knowledgeBase.ontology.TMRPropertySetter;
import knowledgeBase.ontology.TMRPropertySetter.SetterType;
import knowledgeBase.syntax.DependencyVariable;
import knowledgeBase.syntax.TMRReference;
import leia.parse.Dependency;
import leia.parse.DependencyParse;
import leia.parse.SentencePart;
import leia.parse.Time;

public class Deriver {

	Hashtable<String, ArrayList<SentencePart>> propertyTables = new Hashtable<String, ArrayList<SentencePart>>();
	Hashtable<SentencePart, ArrayList<SemanticFact>> factLists = new Hashtable<SentencePart, ArrayList<SemanticFact>>();

	Hashtable<String, ArrayList<TMRTheorem>> assertionReactions = new Hashtable<String, ArrayList<TMRTheorem>>();
	private Hashtable<String, JSONObject> legalTMRTypes = new Hashtable<String, JSONObject>();

	public void addTheorems(String file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					new File(file)));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && !line.startsWith("//")) {
					if (line.contains("//")) {
						line = line.substring(0, line.indexOf("//"));
					}
					addNewTheorem(line);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addNewTheorem(String theorem) {
		theorem = theorem.toLowerCase();
		TMRTheorem newTheorem = TMRTheorem.parse(theorem);
		ArrayList<String> factsNeeded = newTheorem.getFactsNeeded();
		for (int i = 0; i < factsNeeded.size(); i++) {
			String curFact = factsNeeded.get(i);
			if (!assertionReactions.containsKey(curFact)) {
				assertionReactions.put(curFact, new ArrayList<TMRTheorem>());
			}
			assertionReactions.get(curFact).add(newTheorem);
		}
		newTheorem.initialize();
	}

	public void deriveSemantics(SentencePart part) {
		if (part instanceof Time) {
			Time time = (Time) part;
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr.time", part)));
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr/month["
					+ time.month() + "]", part)));
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr/dayOfMonth["
					+ time.dayOfMonth() + "]", part)));
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr/dayOfWeek["
					+ time.dayOfWeek() + "]", part)));
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr/hour["
					+ time.hour() + "]", part)));
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr/minute["
					+ time.minute() + "]", part)));
			expandOn(new TMRTheoremInstance(new SemanticFact("tmr/pm["
					+ time.pm() + "]", part)));
		}
		if (part instanceof Dependency) {
			Dependency tree = (Dependency) part;
			// Add the head fact
			String head = tree.head();
			if (isBeneficiary((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("ben",
						tree.modifiers()[0])));
			} else if (isSubject((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("subj",
						tree.modifiers()[0])));
			} else if (isDirectObject(tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("do",
						tree.modifiers()[0])));
			} else if (isLinkedAdjective((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("la",
						tree.modifiers()[0])));
			} else if (isQuestion((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("ques",
						tree.modifiers()[0])));
			} else if (isCommand((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("comm",
						tree.modifiers()[0])));
			} else if (isWhatFor((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("what",
						tree.modifiers()[0])));
			} else if (isPlural((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("plural",
						tree.modifiers()[0])));
			} else if (isPast((Dependency) tree)) {
				expandOn(new TMRTheoremInstance(new SemanticFact("past",
						tree.modifiers()[0])));
			} else {
				expandOn(new TMRTheoremInstance(new SemanticFact("\""
						+ head.toLowerCase() + "\"", tree)));
			}
			expandOn(new TMRTheoremInstance(new SemanticFact(
					tree.partOfSpeech(), tree)));
			for (int i = 0; i < tree.modifiers().length; i++) {
				if (tree.modifiers()[i] != null) {
					SentencePart child = tree.modifiers()[i];
					if (tree.modifiers()[i] instanceof Dependency
							&& isRole((Dependency) tree.modifiers()[i])) {
						child = ((Dependency) child).modifiers()[0];
					}
					ArrayList<SentencePart> modifierValues = new ArrayList<SentencePart>();
					modifierValues.add(child);
					modifierValues.add(tree);
					expandOn(new TMRTheoremInstance(new SemanticFact("child",
							modifierValues)));
					deriveSemantics(tree.modifiers()[i]);
				}
			}
		}
	}

	private boolean isBeneficiary(Dependency d) {
		return d.head().equals("BENEFICIARY");
	}

	private boolean isRole(Dependency dependency) {
		return dependency.partOfSpeech().equals("role");
	}

	public static boolean isSubject(Dependency d) {
		return d.head().equals("SUBJECT");
	}

	public static boolean isDirectObject(Dependency d) {
		return d.head().equals("DIRECT OBJECT");
	}

	public static boolean isLinkedAdjective(Dependency d) {
		return d.head().equals("LINKED ADJECTIVE");
	}

	public static boolean isQuestion(Dependency d) {
		return d.head().equals("QUESTION");
	}

	public static boolean isCommand(Dependency d) {
		return d.head().equals("COMMAND");
	}

	public static boolean isWhatFor(Dependency d) {
		return d.head().equals("WHAT FOR");
	}

	public static boolean isPlural(Dependency d) {
		return d.head().equals("PLURAL");
	}

	public static boolean isPast(Dependency d) {
		return d.head().equals("PAST");
	}

	private void expandOn(TMRTheoremInstance instance) {
		if (instance.expanded) {
			return;
		}
		instance.expanded = true;
		instance.printProof(System.out);
		for (int i = 0; i < instance.application.size(); i++) {
			SemanticFact curTheorem = instance.application.get(i);
			expandOnFact(curTheorem);
			if (TMRPropertySetter.isTMRRelated(curTheorem)) {
				new TMRPropertySetter(curTheorem);
			}
		}
	}

	private void expandOnFact(SemanticFact fact) {
		for (int i = 0; i < fact.getNumParticipants(); i++) {
			if (fact.getParticipant(i) instanceof DependencyVariable) {
				try {
					throw new Exception(
							"A fact was added that contained incomplete theorems.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		for (int i = 0; i < fact.getNumParticipants(); i++) {
			String key = TMRTheorem.getKey(fact.getType(), i);
			if (!propertyTables.containsKey(key)) {
				propertyTables.put(key, new ArrayList<SentencePart>());
			}
			if (!propertyTables.get(key).contains(fact.getParticipant(i))) {
				propertyTables.get(key).add(fact.getParticipant(i));
				if (!factLists.containsKey(fact.getParticipant(i))) {
					factLists.put(fact.getParticipant(i),
							new ArrayList<SemanticFact>());
				}
				factLists.get(fact.getParticipant(i)).add(fact);
				if (assertionReactions.get(key) != null) {
					for (int t = 0; t < assertionReactions.get(key).size(); t++) {
						TMRTheorem cur = assertionReactions.get(key).get(t);
						ArrayList<TMRTheoremInstance> expandableInstances = cur
								.getDerivedInstances(key, fact, i);
						for (int e = 0; e < expandableInstances.size(); e++) {
							expandOn(expandableInstances.get(e));
						}
					}
				}
			}
		}
	}

	Hashtable<SentencePart, TMR> tmrs = new Hashtable<SentencePart, TMR>();

	public boolean containsTMR(SentencePart participant) {
		return tmrs.containsKey(participant);
	}

	public void addTMR(SentencePart participant, TMR tmr) {
		tmrs.put(participant, tmr);
	}

	public TMR getTMR(SentencePart part) {
		if (!tmrs.containsKey(part)) {
			if (part instanceof TMRReference) {
				tmrs.put(part, new TMR(this, ((TMRReference) part).getTmrKey()));
			} else {
				return null;
			}
		}
		return tmrs.get(part);
	}

	public void resetTMRs() {
		tmrs = new Hashtable<SentencePart, TMR>();
		TMR.indices = new Hashtable<String, Integer>();
	}

	private Iterator<ArrayList<TMRPropertySetter>> generateTMRInterpretations() {
		// First, assemble a list of the possible tmrs that could be assigned to
		// each SentencePart (for which tmrs are assigned).
		Hashtable<SentencePart, ArrayList<TMRPropertySetter>> possibleTMRs = new Hashtable<SentencePart, ArrayList<TMRPropertySetter>>();
		ArrayList<TMRPropertySetter> tmrCreators = TMRPropertySetter.setterLists
				.get(TMRPropertySetter.SetterType.CREATION);
		if (tmrCreators == null) {
			System.err
					.println("No tmrs were able to be generated from the given sentence.");
			System.exit(0);
		}
		for (int i = 0; i < tmrCreators.size(); i++) {
			SentencePart part = tmrCreators.get(i).getFact().getParticipant(0);
			if (!possibleTMRs.containsKey(part)) {
				possibleTMRs.put(part, new ArrayList<TMRPropertySetter>());
			}
			boolean found = false;
			for (int q = 0; q < possibleTMRs.get(part).size(); q++) {
				if (possibleTMRs.get(part).get(q).toString()
						.equals(tmrCreators.get(i).toString())) {
					found = true;
				}
			}
			if (!found) {
				possibleTMRs.get(part).add(tmrCreators.get(i));
			}
		}
		// The final list is a list of lists, where each child list is the
		// possible candidates for a given sentencepart.
		final ArrayList<ArrayList<TMRPropertySetter>> possibleTMRList = new ArrayList<ArrayList<TMRPropertySetter>>();
		Iterator<Entry<SentencePart, ArrayList<TMRPropertySetter>>> iterator = possibleTMRs
				.entrySet().iterator();
		while (iterator.hasNext()) {
			possibleTMRList.add(iterator.next().getValue());
		}
		Iterator<ArrayList<TMRPropertySetter>> ret = new Iterator<ArrayList<TMRPropertySetter>>() {

			public boolean hasNext() {
				return !closed;
			}

			ArrayList<TMRPropertySetter> currentValue;
			boolean closed = false;
			int[] previousProgress;

			public ArrayList<TMRPropertySetter> next() {
				if (closed) {
					throw new NoSuchElementException();
				}
				if (previousProgress == null) {
					// If this is the first time this iterator is called, don't
					// do anything but establish a first value and declare
					// things.
					previousProgress = new int[possibleTMRList.size()];
					currentValue = getCurrentResult();
					return null;
				} else if (currentValue != null) {
					if (previousProgress.length == 0) {
						closed = true;
						return new ArrayList<TMRPropertySetter>();
					}
					int currentIndex = previousProgress.length - 1;
					while (true) {
						if (previousProgress[currentIndex] + 1 == possibleTMRList
								.get(currentIndex).size()) {
							previousProgress[currentIndex] = 0;
							currentIndex--;
							// If we've backed out all the way past the first
							// option, then we've exhausted all possibilities
							// and should close the iterator.
							if (currentIndex == -1) {
								closed = true;
								break;
							}
						} else {
							previousProgress[currentIndex]++;
							break;
						}
					}
					// Return the result, setting the next return as we do so.
					ArrayList<TMRPropertySetter> ret = currentValue;
					currentValue = getCurrentResult();
					return ret;
				}
				return null;
			}

			private ArrayList<TMRPropertySetter> getCurrentResult() {
				if (previousProgress == null) {
					return null;
				}
				ArrayList<TMRPropertySetter> ret = new ArrayList<TMRPropertySetter>();
				for (int i = 0; i < previousProgress.length; i++) {
					ret.add(possibleTMRList.get(i).get(previousProgress[i]));
				}
				return ret;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		ret.next();
		return ret;
	}

	private void outputTMRs() {
		Iterator<TMR> iterator = tmrs.values().iterator();
		HashSet<TMR> printed = new HashSet<TMR>();
		while (iterator.hasNext()) {
			TMR next = iterator.next();
			if (!printed.contains(next)) {
				next.print();
				printed.add(next);
			}
		}
	}

	private int assembleTMRs(ArrayList<TMRPropertySetter> tmrCreators) {
		int succesfulApplications = 0;
		// FIXME: Resolve reference errors beforehand.
		ArrayList<TMRPropertySetter> referenceList = TMRPropertySetter.setterLists
				.get(TMRPropertySetter.SetterType.REFERENCE);
		for (int i = 0; i < referenceList.size(); i++) {
			referenceList.get(i).execute(this);
			succesfulApplications++;
		}
		for (int i = 0; i < tmrCreators.size(); i++) {
			succesfulApplications += tmrCreators.get(i).execute(this);
		}
		SetterType[] setterTypes = TMRPropertySetter.SetterType.values();
		for (int i = 0; i < setterTypes.length; i++) {
			SetterType cur = setterTypes[i];
			if (cur != TMRPropertySetter.SetterType.CREATION
					&& cur != TMRPropertySetter.SetterType.REFERENCE) {
				ArrayList<TMRPropertySetter> setterList = TMRPropertySetter.setterLists
						.get(setterTypes[i]);
				for (int a = 0; a < setterList.size(); a++) {
					if (setterList.get(a).canExecute(this)) {
						setterList.get(a).execute(this);
						succesfulApplications++;
					}
				}
			}
		}
		return succesfulApplications;
	}

	@SuppressWarnings("unchecked")
	private void addOntology(String file) {
		StringBuffer fullText = new StringBuffer();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					new File(file)));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("//")) {
					line = line.substring(0, line.indexOf("//"));
				}
				fullText.append(line + "\n");
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		JSONObject parse = (JSONObject) JSONValue.parse(fullText.toString());
		Iterator<String> tmrTypeIterator = parse.keySet().iterator();
		while (tmrTypeIterator.hasNext()) {
			String key = tmrTypeIterator.next();
			JSONObject newTMRType = (JSONObject) parse.get(key);
			legalTMRTypes.put(key.toLowerCase(), newTMRType);
			if (newTMRType.containsKey("inherits")
					&& !newTMRType.get("inherits").equals("")) {
				addNewTheorem("x st tmr." + key + "(x) : tmr."
						+ newTMRType.get("inherits") + "(x)");
			}
		}
		// Pass all inherited properties up.
		Iterator<Entry<String, JSONObject>> tmrIterator = legalTMRTypes
				.entrySet().iterator();
		while (tmrIterator.hasNext()) {
			Entry<String, JSONObject> curTMREntry = tmrIterator.next();
			JSONObject curTMR = curTMREntry.getValue();
			while (curTMR.containsKey("inherits")
					&& !curTMR.get("inherits").equals("")) {
				curTMR = legalTMRTypes.get(((String) curTMR.get("inherits"))
						.toLowerCase());
				Iterator<String> parentIterator = curTMR.keySet().iterator();
				while (parentIterator.hasNext()) {
					String curKey = parentIterator.next();
					if (!curTMREntry.getValue().containsKey(curKey)) {
						curTMREntry.getValue().put(curKey, curTMR.get(curKey));
					}
				}
			}
		}
	}

	public JSONObject getTMRType(String identifier) {
		if (identifier == null || identifier.length() == 0) {
			return null;
		}
		return this.legalTMRTypes.get(identifier.toLowerCase());
	}

	/**
	 * Determines whether or not tmrType2 inherits from tmrType1, that is,
	 * whether or not tmrType2 ISA tmrType1.
	 */
	public boolean inherits(String tmrType1, String tmrType2) {
		return inherits(tmrType1, tmrType2, new HashSet<String>());
	}

	/**
	 * Determines whether or not tmrType2 inherits from tmrType1, that is,
	 * whether or not tmrType2 ISA tmrType1.
	 */
	public boolean inherits(String tmrType1, String tmrType2,
			HashSet<String> exclude) {
		if (!this.legalTMRTypes.containsKey(tmrType1)
				|| !this.legalTMRTypes.containsKey(tmrType2)) {
			return false;
		}
		if (tmrType1.equals(tmrType2)) {
			return true;
		}
		if (!legalTMRTypes.get(tmrType2).containsKey("inherits")) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Iterator<String> inheritanceIterator = ((JSONObject) legalTMRTypes.get(
				tmrType1).get("inherits")).keySet().iterator();
		while (inheritanceIterator.hasNext()) {
			String currentInheritor = inheritanceIterator.next();
			if (!exclude.contains(currentInheritor)) {
				if (inherits(tmrType1, currentInheritor)) {
					return true;
				}
				exclude.add(currentInheritor);
			}
		}
		return false;
	}

	private final static boolean showAllTMRS = false;

	public static void main(String[] args) {
		// Scanner scanner = new Scanner(System.in);
		// String sentence = scanner.next();
		// scanner.close();
		// String sentence = "When is Joe's Pizza open?";
		String sentence = "I want to find a nice place for a dinner with my father tomorrow at 7 pm.";
		// String sentence = "I like Mexican.";
		// String sentence = "What is open tonight?";
		// String sentence =
		// "Could you give me a place I could eat at sometime?";
		Deriver deriver = new Deriver();
		deriver.addTheorems("ruleList");
		deriver.addOntology("ontology.json");
		SentencePart part = DependencyParse.parse(sentence)[0];
		System.out.println("\"" + sentence + "\"");
		System.out.println(part);
		deriver.deriveSemantics(part);
		Iterator<ArrayList<TMRPropertySetter>> interpretationList = deriver
				.generateTMRInterpretations();
		ArrayList<Hashtable<SentencePart, TMR>> tmrList = new ArrayList<Hashtable<SentencePart, TMR>>();
		int bestTMRIndex = -1;
		int bestNum = -1;
		while (interpretationList.hasNext()) {
			if (showAllTMRS) {
				System.out
						.println("====================================================================");
			}
			int numTMRAssignmentsUsed = deriver.assembleTMRs(interpretationList
					.next());
			if (numTMRAssignmentsUsed > bestNum) {
				bestTMRIndex = tmrList.size();
				bestNum = numTMRAssignmentsUsed;
			}
			tmrList.add(deriver.tmrs);
			if (showAllTMRS) {
				System.out.println(numTMRAssignmentsUsed);
				deriver.outputTMRs();
			}
			deriver.resetTMRs();
		}
		if (!showAllTMRS) {
			System.out
					.println("====================================================================");
			System.out.println(bestNum);
			if (bestTMRIndex != -1) {
				deriver.tmrs = tmrList.get(bestTMRIndex);
				deriver.outputTMRs();
			}
		}
	}
}
