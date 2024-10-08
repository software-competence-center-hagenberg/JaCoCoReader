package at.scch.jacoco.reader;

import org.json.JSONObject;

/**
 * Representation of a Line in the source code structure of the system under test.
 */
public class JacocoLine {

	/**
	 * Line number of this line in the source code.
	 */
	private final int lineNumber;

	/**
	 * Method this line is contained in.
	 */
	private final JacocoMethod method;

	private final int instructions;

	private final int branches;

	public JacocoLine(int lineNumber, JacocoMethod method, int instructions, int branches) {
		this.lineNumber = lineNumber;
		this.method = method;
		this.method.addLine(this);
		this.instructions = instructions;
		this.branches = branches;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public JacocoMethod getMethod() {
		return method;
	}

	public int getInstructions() {
		return instructions;
	}

	public int getBranches() {
		return branches;
	}

	/**
	 * Get an identifier for this line, with the full name of the method and the line number.
	 * @return package name + class name + method signature + line number
	 */
	public String getIdentifier() {
		return this.method.getFullName() + ":" + lineNumber;
	}

	protected JSONObject serialize() {
		JSONObject jLine = new JSONObject();
		jLine.put("lineNumber", this.lineNumber);
		jLine.put("instructions", instructions);
		jLine.put("branches", branches);
		return jLine;
	}

	public static JacocoLine deserialize(JSONObject jLine, JacocoMethod method) {
		int lineNumber = jLine.getInt("lineNumber");
		int instructions = jLine.getInt("instructions");
		int branches = jLine.getInt("branches");
		return new JacocoLine(lineNumber, method, instructions, branches);
	}
}
