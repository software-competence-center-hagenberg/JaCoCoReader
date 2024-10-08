package at.scch.jacoco.reader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoMethod.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Representation of a Method in the source code structure of the system under test.
 */
public class JacocoMethod {

	/**
	 * Signature of the method.
	 */
	private final String signature;

	/**
	 * Class this method is contained in.
	 */
	private final JacocoClass clazz;

	/**
	 * Lines by numbers inside the method.
	 */
	private final Map<Integer, JacocoLine> lines;

	private final int complexity;

	public JacocoMethod(JacocoClass clazz, String signature, int complexity) {
		super();
		this.clazz = clazz;
		this.clazz.addMethod(this);
		this.signature = signature;
		this.lines = new HashMap<>();
		this.complexity = complexity;
	}

	public JacocoClass getClazz() {
		return clazz;
	}

	public String getSignature() {
		return signature;
	}

	public String getName() {
		return signature.split("\\(")[0].trim();
	}

	public Set<Integer> getLineNumbers() {
		return lines.keySet();
	}

	public Collection<JacocoLine> getLines() {
		return lines.values();
	}

	public JacocoLine getLine(int lineNumber) {
		return this.lines.get(lineNumber);
	}

	/**
	 * @return - number of lines contained in this method
	 */
	public int getNumberOfLines() {
		return this.lines.size();
	}

	/**
	 * Get the full name of this method inside its class and package.
	 * @return package name and class name and method signature
	 */
	public String getFullName() {
		return clazz.getFullName() + "." + this.signature;
	}

	protected void addLine(JacocoLine jacocoLine) {
		this.lines.put(jacocoLine.getLineNumber(), jacocoLine);
	}

	public int getBranches() {
		int branches = 0;
		for (JacocoLine line : this.lines.values()) {
			branches += line.getBranches();
		}
		return branches;
	}

	public int getInstructions() {
		int instructions = 0;
		for (JacocoLine line : this.lines.values()) {
			instructions += line.getInstructions();
		}
		return instructions;
	}

	public int getComplexity() {
		return complexity;
	}

	protected JSONObject serialize() {
		JSONObject jMethod = new JSONObject();
		jMethod.put("signature", this.signature);
		jMethod.put("complexity", this.complexity);

		// serialize lines
		JSONArray lines = new JSONArray();
		for (JacocoLine line : this.lines.values()) {
			JSONObject jLine = line.serialize();
			lines.put(jLine);
		}
		jMethod.put("lines", lines);

		return jMethod;
	}

	public static JacocoMethod deserialize(JSONObject jMethod, JacocoClass clazz) {
		String name = jMethod.getString("signature");
		int complexity = jMethod.getInt("complexity");
		JacocoMethod deserialized = new JacocoMethod(clazz, name, complexity);

		// parse lines
		JSONArray lines = jMethod.getJSONArray("lines");
		for (Object l : lines) {
			JSONObject jLine = (JSONObject)l;
			JacocoLine.deserialize(jLine, deserialized);
		}

		return deserialized;
	}
}
