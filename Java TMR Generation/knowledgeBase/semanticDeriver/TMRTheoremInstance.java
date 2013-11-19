package knowledgeBase.semanticDeriver;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;

public class TMRTheoremInstance {
	boolean expanded = false;
	TMRTheorem theorem;
	ArrayList<SemanticFact> application;
	private ArrayList<TMRTheoremInstance> evidence = new ArrayList<TMRTheoremInstance>();

	public TMRTheoremInstance() {
		// no-op
	}

	public TMRTheoremInstance(SemanticFact fact) {
		theorem = TMRTheorem.GIVEN;
		fact.setDerivation(this);
		application = new ArrayList<SemanticFact>();
		application.add(fact);
	}

	public TMRTheoremInstance(ArrayList<TMRTheoremInstance> theoremEvidence) {
		this.evidence = theoremEvidence;
	}

	public void contributeToProof(ArrayList<TMRTheoremInstance> proof) {
		proof.add(0, this);
		for (int i = 0; i < evidence.size(); i++) {
			evidence.get(i).contributeToProof(proof);
		}
	}

	public void printProof(PrintStream output) {
		ArrayList<TMRTheoremInstance> proof = new ArrayList<TMRTheoremInstance>();
		contributeToProof(proof);
		Hashtable<TMRTheoremInstance, Integer> reverseLookup = new Hashtable<TMRTheoremInstance, Integer>();
		output.println("------------------------------------------------");
		// if (proof.size() == 1) {
		// return;
		// }
		for (int i = 0; i < proof.size(); i++) {
			reverseLookup.put(proof.get(i), i);
		}
		for (int i = 0; i < proof.size(); i++) {
			output.print(i + ":" + proof.get(i).theorem.toString() + "|||||"
					+ proof.get(i).application.toString() + "|||||||");
			for (int q = 0; q < proof.get(i).evidence.size(); q++) {
				if (q > 0) {
					output.print(",");
				}
				output.print(reverseLookup.get(proof.get(i).evidence.get(q)));
			}
			output.println();
		}
	}
}
