package knowledgeBase.ontology;

import java.util.ArrayList;
import java.util.Hashtable;

import knowledgeBase.semanticDeriver.Deriver;
import knowledgeBase.semanticDeriver.SemanticFact;
import knowledgeBase.semanticDeriver.TMR;
import knowledgeBase.semanticDeriver.TMRConstraint;
import knowledgeBase.syntax.TMRReference;

public class TMRPropertySetter {

	public static final Hashtable<SetterType, ArrayList<TMRPropertySetter>> setterLists = new Hashtable<SetterType, ArrayList<TMRPropertySetter>>();

	public static enum SetterType {
		/**
		 * Settings of the form tmr.name
		 */
		CREATION,
		/**
		 * Settings of the form tmr/prop
		 */
		SETTING_TMR,
		/**
		 * Settings of the form tmr/prop+
		 */
		ADDER,
		/**
		 * Settings of the form tmr/prop=value or tmr/prop&ltvalue or
		 * tmr/prop&gtvalue
		 */
		ASSIGNMENT,
		/**
		 * Settings of the form tmr/prop[value]
		 */
		SETTING_VALUE,
		/**
		 * Settings of the form tmr#ref
		 */
		REFERENCE,
		/**
		 * Settings of the form tmr/?
		 */
		DEMARKER,
		/**
		 * Settings of the form tmr/prop?
		 */
		MARKER,
		/**
		 * Marking a given fact as the thing we want to find.<br>
		 * ?tmr/prop(x)FIXME: Doens't work yet.
		 */
		GOALFACT
	}

	final private SemanticFact fact;
	private final SetterType setterType;

	public TMRPropertySetter(SemanticFact fact) {
		this.fact = fact;
		String typeString = fact.getType();
		if (typeString.contains("#")) {
			setterType = SetterType.REFERENCE;
		} else if (typeString.startsWith("tmr/")) {
			if (typeString.contains("[")) {
				setterType = SetterType.SETTING_VALUE;
			} else if (typeString.contains("<") || typeString.contains(">")
					|| typeString.contains("=")) {
				setterType = SetterType.ASSIGNMENT;
			} else if (typeString.endsWith(".?")) {
				setterType = SetterType.DEMARKER;
			} else if (typeString.contains("?")) {
				setterType = SetterType.MARKER;
			} else if (typeString.contains("+")) {
				setterType = SetterType.ADDER;
			} else {
				setterType = SetterType.SETTING_TMR;
			}
		} else if (typeString.startsWith("?tmr/")) {
			setterType = SetterType.GOALFACT;
		} else {
			setterType = SetterType.CREATION;
		}
		if (setterLists.size() == 0) {
			SetterType[] values = SetterType.values();
			for (int i = 0; i < values.length; i++) {
				setterLists.put(values[i], new ArrayList<TMRPropertySetter>());
			}
		}
		setterLists.get(setterType).add(this);
	}

	public static boolean isTMRRelated(SemanticFact fact) {
		return fact.getType().startsWith("tmr");
	}

	public SemanticFact getFact() {
		return fact;
	}

	public SetterType getType() {
		return setterType;
	}

	@SuppressWarnings("unchecked")
	public int execute(Deriver deriver) {
		int ret = 0;
		String factType = getFact().getType();
		if (getType() == SetterType.REFERENCE) {
			String reference = factType.substring(factType.indexOf("#") + 1,
					factType.length());
			if (reference.equals("user")) {
				deriver.addTMR(getFact().getParticipant(0),
						deriver.getTMR(new TMRReference(reference, "human")));
			}
		}
		if (getType() == SetterType.CREATION) {
			String tmrType = factType.substring(factType.indexOf('.') + 1,
					factType.length());
			String tmrTypeForInheritanceCount = tmrType;
			while (deriver.getTMRType(tmrTypeForInheritanceCount) != null) {
				tmrTypeForInheritanceCount = (String) deriver.getTMRType(
						tmrTypeForInheritanceCount).get("inherits");
				ret++;
			}
			if (deriver.containsTMR(getFact().getParticipant(0))) {
				TMR oldTMR = deriver.getTMR(getFact().getParticipant(0));
				String oldTMRType = oldTMR.getIdentifier();
				if (deriver.inherits(oldTMRType, tmrType)) {
					// If we are putting a more specific tmr type in (IE, if our
					// newtype inherits from the old type), then we have to copy
					// our properties over to the new type, and overwrite it in
					// the deriver.
					oldTMR.changeTMRType(tmrType);
				} else if (deriver.inherits(tmrType, oldTMRType)) {
					// If the current tmr type inherits from our new tmr type
					// then
					// we have nothing to worry about, as a more powerful class
					// is already in place.
				} else {
					System.err
							.println("Multiple conflicting tmr assignments made to same object");
					System.err
							.println("Object :" + getFact().getParticipant(0));
					System.err.println(oldTMR.getIdentifier()
							+ " conflicts with " + tmrType);
					System.exit(0);
				}
			} else {
				if (deriver.getTMRType(tmrType) == null) {
					System.err.println("Illegal TMR type \"" + tmrType
							+ "\" was specified in semantic fact :" + fact);
					System.exit(0);
				}
				deriver.addTMR(getFact().getParticipant(0), new TMR(deriver,
						tmrType));
			}
		}
		if (getType() == SetterType.SETTING_TMR) {
			deriver.getTMR(getFact().getParticipant(0)).properties.put(
					factType.substring(factType.indexOf('/') + 1,
							factType.length()),
					deriver.getTMR(getFact().getParticipant(1)));
		}
		if (getType() == SetterType.MARKER) {
			deriver.getTMR(getFact().getParticipant(0)).setMarker(
					factType.substring(factType.indexOf('/') + 1,
							factType.length() - 1));
		}
		if (getType() == SetterType.DEMARKER) {
			deriver.getTMR(getFact().getParticipant(0)).clearMarker(
					getFact().getParticipant(1));
		}
		if (getType() == SetterType.ADDER) {
			String key = factType.substring(factType.indexOf('/') + 1,
					factType.length() - 1);
			Hashtable<String, Object> properties = deriver.getTMR(getFact()
					.getParticipant(0)).properties;
			if (!properties.containsKey(key) || properties.get(key) == null) {
				properties.put(key, new ArrayList<TMR>());
			}
			((ArrayList<TMR>) properties.get(key)).add(deriver.getTMR(getFact()
					.getParticipant(1)));
		}
		if (getType() == SetterType.ASSIGNMENT) {
			String split;
			if (factType.contains("=")) {
				split = "=";
			} else if (factType.contains("<")) {
				split = "<";
			} else {
				split = ">";
			}
			int splitIndex = factType.indexOf(split);
			deriver.getTMR(getFact().getParticipant(0)).properties.put(
					factType.substring(factType.indexOf('/') + 1, splitIndex),
					new TMRConstraint(split, factType.subSequence(
							splitIndex + 1, factType.length())));
		}
		if (getType() == SetterType.SETTING_VALUE) {
			String type = getFact().getType();
			deriver.getTMR(getFact().getParticipant(0)).properties.put(
					type.substring(type.indexOf('/') + 1, type.indexOf('[')),
					type.substring(type.indexOf('[') + 1, type.length() - 1));
		}
		if (getType() == SetterType.GOALFACT) {
			deriver.getTMR(getFact().getParticipant(0)).setGoalFact(
					factType.substring(factType.indexOf('/') + 1,
							factType.length()));
		}
		return ret;
	}

	public boolean canExecute(Deriver deriver) {
		String factType = getFact().getType();
		if (deriver.getTMR(getFact().getParticipant(0)) == null) {
			return false;
		}
		if (getFact().getNumParticipants() > 1
				&& deriver.getTMR(getFact().getParticipant(1)) == null
				&& getType() != SetterType.SETTING_VALUE) {
			return false;
		}
		if (getType() == SetterType.SETTING_TMR) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(factType.indexOf('/') + 1,
							factType.length()));
		}
		if (getType() == SetterType.MARKER) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(factType.indexOf('/') + 1,
							factType.length() - 1));
		}
		if (getType() == SetterType.DEMARKER) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(factType.indexOf('/') + 1,
							factType.length() - 1));
		}
		if (getType() == SetterType.ADDER) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(factType.indexOf('/') + 1,
							factType.length() - 1));
		}
		if (getType() == SetterType.ASSIGNMENT) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(
							factType.indexOf('/') + 1,
							Math.max(
									factType.indexOf('='),
									Math.max(factType.indexOf('<'),
											factType.indexOf('>')))));
		}
		if (getType() == SetterType.SETTING_VALUE) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(factType.indexOf('/') + 1,
							factType.indexOf('[')));
		}
		if (getType() == SetterType.GOALFACT) {
			return deriver.getTMR(getFact().getParticipant(0)).isLegalProperty(
					factType.substring(factType.indexOf('/') + 1,
							factType.length()));
		}
		return false;
	}
}
