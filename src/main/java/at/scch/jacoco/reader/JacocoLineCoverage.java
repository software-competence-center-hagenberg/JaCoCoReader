package at.scch.jacoco.reader;

import org.json.JSONObject;

/**
 * Coverage data for a line
 */
public class JacocoLineCoverage {

	/**
	 * Line the coverage data is for
	 */
	private final JacocoLine line;

	private final int instructionsCovered;

	private final int branchesCovered;

	public JacocoLineCoverage(JacocoLine line, int instructionsCovered, int branchesCovered) {
		this.line = line;
		this.instructionsCovered = instructionsCovered;
		this.branchesCovered = branchesCovered;
	}

	public JacocoLine getLine() {
		return line;
	}

	public int getInstructionsCovered() {
		return instructionsCovered;
	}

	public int getBranchesCovered() {
		return branchesCovered;
	}

	protected JSONObject serialize() {
		JSONObject jLine = new JSONObject();
		jLine.put("lineNumber", this.line.getLineNumber());
		jLine.put("instructionsCovered", instructionsCovered);
		jLine.put("branchesCovered", branchesCovered);
		return jLine;
	}

	public static JacocoLineCoverage deserialize(JSONObject jLineCoverage, JacocoMethod method) {
		int lineNumber = jLineCoverage.getInt("lineNumber");
		JacocoLine line = method.getLine(lineNumber);
		int instructionsCovered = jLineCoverage.getInt("instructionsCovered");
		int branchesCovered = jLineCoverage.getInt("branchesCovered");
		return new JacocoLineCoverage(line, instructionsCovered, branchesCovered);
	}
}
