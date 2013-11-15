package knowledgeBase.semanticDeriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import knowledgeBase.syntax.DependencyVariable;
import knowledgeBase.syntax.SentencePart;
import knowledgeBase.syntax.SyntaxDependency;
import knowledgeBase.syntax.TMRReference;
import output.processor;

public class Deriver {

	Hashtable<String, ArrayList<SentencePart>> propertyTables = new Hashtable<String, ArrayList<SentencePart>>();
	Hashtable<SentencePart, ArrayList<SemanticFact>> factLists = new Hashtable<SentencePart, ArrayList<SemanticFact>>();

	Hashtable<String, ArrayList<TMRTheorem>> assertionReactions = new Hashtable<String, ArrayList<TMRTheorem>>();

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
					TMRTheorem newTheorem = TMRTheorem.parse(line);
					ArrayList<String> factsNeeded = newTheorem.getFactsNeeded();
					for (int i = 0; i < factsNeeded.size(); i++) {
						String curFact = factsNeeded.get(i);
						if (!assertionReactions.containsKey(curFact)) {
							assertionReactions.put(curFact,
									new ArrayList<TMRTheorem>());
						}
						assertionReactions.get(curFact).add(newTheorem);
					}
					newTheorem.initialize();
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void deriveSemantics(SentencePart part) {
		if (part instanceof SyntaxDependency) {
			SyntaxDependency tree = (SyntaxDependency) part;
			// Add the head fact
			String head = tree.getHead();
			if (tree.isDirectObject()) {
				expandOn(new TMRTheoremInstance(new SemanticFact("DO", tree)));
			} else {
				expandOn(new TMRTheoremInstance(new SemanticFact("\"" + head
						+ "\"", tree)));
			}
			expandOn(new TMRTheoremInstance(new SemanticFact(
					tree.getPartOfSpeech(), tree)));
			SentencePart subject = tree.getSubject();
			if (subject != null) {
				expandOn(new TMRTheoremInstance(new SemanticFact(
						SyntaxDependency.DIRECT_OBJECT, subject)));
				ArrayList<SentencePart> subjectModifierValues = new ArrayList<SentencePart>();
				subjectModifierValues.add(tree.getSubject());
				subjectModifierValues.add(tree);
				expandOn(new TMRTheoremInstance(new SemanticFact("child",
						subjectModifierValues)));
				deriveSemantics(tree.getSubject());
			}
			for (int i = 0; i < tree.getNumModifiers(); i++) {
				if (tree.getModifier(i) != null) {
					ArrayList<SentencePart> modifierValues = new ArrayList<SentencePart>();
					modifierValues.add(tree.getModifier(i));
					modifierValues.add(tree);
					expandOn(new TMRTheoremInstance(new SemanticFact("child",
							modifierValues)));
					deriveSemantics(tree.getModifier(i));
				}
			}
		}
	}

	private void expandOn(TMRTheoremInstance instance) {
		if (instance.expanded) {
			return;
		}
		instance.expanded = true;
		// instance.printProof();
		for (int i = 0; i < instance.application.size(); i++) {
			expandOnFact(instance.application.get(i));
			String type = instance.application.get(i).getType();
			if (type.startsWith("tmr.")) {
				tmrCreations.add(instance.application.get(i));
			}
			if (type.contains("#")) {
				tmrPointers.add(instance.application.get(i));
			}
			if (type.startsWith("tmr/")) {
				if (type.contains("[")) {
					tmrSpecialSettings.add(instance.application.get(i));
				} else if (type.contains("<") || type.contains(">")
						|| type.contains("=")) {
					tmrAssignmentSettings.add(instance.application.get(i));
				} else if (type.contains("?")) {
					tmrQuestions.add(instance.application.get(i));
				} else if (type.contains("+")) {
					tmrAdderSettings.add(instance.application.get(i));
				} else {
					tmrSettings.add(instance.application.get(i));
				}
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

	/**
	 * Settings of the form tmr.name
	 */
	private ArrayList<SemanticFact> tmrCreations = new ArrayList<SemanticFact>();
	/**
	 * Settings of the form tmr/prop
	 */
	private ArrayList<SemanticFact> tmrSettings = new ArrayList<SemanticFact>();
	/**
	 * Settings of the form tmr/prop?
	 */
	private ArrayList<SemanticFact> tmrQuestions = new ArrayList<SemanticFact>();
	/**
	 * Settings of the form tmr/prop+
	 */
	private ArrayList<SemanticFact> tmrAdderSettings = new ArrayList<SemanticFact>();
	/**
	 * Settings of the form tmr/prop=value or tmr/prop&ltvalue or
	 * tmr/prop&gtvalue
	 */
	private ArrayList<SemanticFact> tmrAssignmentSettings = new ArrayList<SemanticFact>();
	/**
	 * Settings of the form tmr/prop[value]
	 */
	private ArrayList<SemanticFact> tmrSpecialSettings = new ArrayList<SemanticFact>();
	/**
	 * Settings of the form tmr#ref
	 */
	private ArrayList<SemanticFact> tmrPointers = new ArrayList<SemanticFact>();
	Hashtable<SentencePart, TMR> tmrs;

	private TMR getTMR(SentencePart part) {
		if (!tmrs.containsKey(part)) {
			if (part instanceof SyntaxDependency) {
				tmrs.put(part, new TMR(((SyntaxDependency) part).getHead()));
			} else if (part instanceof TMRReference) {
				tmrs.put(part, new TMR(((TMRReference) part).getTmrKey()));
			}
		}
		return tmrs.get(part);
	}

	@SuppressWarnings("unchecked")
	private void assembleTMRs() {
		tmrs = new Hashtable<SentencePart, TMR>();
		// Before we start, make sure any fragments that are supposed to share a
		// tmr do.
		for (int i = 0; i < tmrPointers.size(); i++) {
			SemanticFact curSetting = tmrPointers.get(i);
			String type = curSetting.getType();
			String reference = type.substring(type.indexOf("#") + 1,
					type.length());
			if (reference.equals("user")) {
				tmrs.put(tmrPointers.get(i).getParticipant(0),
						getTMR(new TMRReference(reference, "human")));
			}
		}
		// First create all of the tmrs.
		for (int i = 0; i < tmrCreations.size(); i++) {
			String type = tmrCreations.get(i).getType();
			if (!tmrs.containsKey(tmrCreations.get(i).getParticipant(0))) {
				tmrs.put(
						tmrCreations.get(i).getParticipant(0),
						new TMR(type.substring(type.indexOf('.') + 1,
								type.length())));
			}
		}
		// Then add all the various types of properties...
		for (int i = 0; i < tmrSettings.size(); i++) {
			SemanticFact curSetting = tmrSettings.get(i);
			String type = curSetting.getType();
			getTMR(curSetting.getParticipant(0)).properties.put(
					type.substring(type.indexOf('/') + 1, type.length()),
					getTMR(curSetting.getParticipant(1)));
		}
		// Then add all the various types of properties...
		for (int i = 0; i < tmrQuestions.size(); i++) {
			SemanticFact curSetting = tmrQuestions.get(i);
			String type = curSetting.getType();
			getTMR(curSetting.getParticipant(0)).addQuestionMark(
					type.substring(type.indexOf('/') + 1, type.length() - 1));
		}
		for (int i = 0; i < tmrAdderSettings.size(); i++) {
			SemanticFact curSetting = tmrAdderSettings.get(i);
			String type = curSetting.getType();
			String key = type.substring(type.indexOf('/') + 1,
					type.length() - 1);
			Hashtable<String, Object> properties = tmrs.get(curSetting
					.getParticipant(0)).properties;
			if (!properties.containsKey(key) || properties.get(key) == null) {
				properties.put(key, new ArrayList<TMR>());
			}
			((ArrayList<TMR>) properties.get(key)).add(getTMR(curSetting
					.getParticipant(1)));
		}
		for (int i = 0; i < tmrAssignmentSettings.size(); i++) {
			SemanticFact curSetting = tmrAssignmentSettings.get(i);
			String type = curSetting.getType();
			String split;
			if (type.contains("=")) {
				split = "=";
			} else if (type.contains("<")) {
				split = "<";
			} else {
				split = ">";
			}
			int splitIndex = type.indexOf(split);
			getTMR(curSetting.getParticipant(0)).properties.put(type.substring(
					type.indexOf('/') + 1, splitIndex), new TMRConstraint(
					split, type.subSequence(splitIndex + 1, type.length())));
		}
		for (int i = 0; i < tmrSpecialSettings.size(); i++) {
			SemanticFact curSetting = tmrSpecialSettings.get(i);
			String type = curSetting.getType();
			getTMR(curSetting.getParticipant(0)).properties.put(
					type.substring(type.indexOf('/') + 1, type.indexOf('[')),
					type.substring(type.indexOf('[') + 1, type.length() - 1));
		}
	}

	private void outputTMRs() {
		Iterator<TMR> iterator = tmrs.values().iterator();
		HashSet<TMR> printed = new HashSet<TMR>();
		while (iterator.hasNext()) {
			TMR next = iterator.next();
			if (!printed.contains(next)) {
                //System.out.println("xxx");
				next.print();
				printed.add(next);
			}
		}
	}

	public static void main(String[] args) {
		String stringToParse = "<want|v,<i|pronoun, null, >,<tomorrow|adv, null, >,<direct object|p,<find|v, null,<for|prep,<dinner|n, null,<a|det, null, >>,>,<with|prep,<father|n, null,<my|adj, null, >>,>,<direct object|p,<place|n, null,<nice|adj, null, >,<a|det, null, >>,>>,>>";
		Deriver deriver = new Deriver();
		deriver.addTheorems("ruleList");
		deriver.deriveSemantics(SyntaxDependency.parse(stringToParse));
		deriver.assembleTMRs();
		deriver.outputTMRs();
	}

}
