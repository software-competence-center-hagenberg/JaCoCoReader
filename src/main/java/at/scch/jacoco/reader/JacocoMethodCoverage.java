package at.scch.jacoco.reader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoMethodCoverage.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Coverage data for a method
 */
public class JacocoMethodCoverage {

	/**
	 * Method the coverage data is for
	 */
	private final JacocoMethod method;

	/**
	 * Lines inside the method the were covered, by number.
	 */
	private final Map<Integer, JacocoLineCoverage> linesCovered;

	public JacocoMethodCoverage(JacocoMethod method) {
		this(method, new HashMap<>());
	}

	public JacocoMethodCoverage(JacocoMethod method, Map<Integer, JacocoLineCoverage> linesCovered) {
		super();
		this.method = method;
		this.linesCovered = linesCovered;
	}

	public JacocoMethod getMethod() {
		return method;
	}

	public Set<Integer> getLineNumbersCovered() {
		return linesCovered.keySet();
	}

	public boolean isLineCovered(int lineNumber){
		return linesCovered.containsKey(lineNumber);
	}

	public int getNumberOfLinesCovered() {
		return this.linesCovered.size();
	}

	private JacocoLineCoverage getLineCoverage(int lineNumber) {
		return this.linesCovered.get(lineNumber);
	}

	public int getBranchesCovered() {
		int branches = 0;
		for (JacocoLineCoverage line : this.linesCovered.values()) {
			branches += line.getBranchesCovered();
		}
		return branches;
	}

	public int getInstructionsCovered() {
		int instructions = 0;
		for (JacocoLineCoverage line : this.linesCovered.values()) {
			instructions += line.getInstructionsCovered();
		}
		return instructions;
	}

	/**
	 * Add lines to the coverage data. (Union)
	 * If a line already exists: keep the one with the best coverage (i.e. max instructions covered).
	 * @param toAdd - coverage to add
	 */
	public void addLinesCovered(JacocoMethodCoverage toAdd) {
		for (Map.Entry<Integer, JacocoLineCoverage> coverageEntry : toAdd.linesCovered.entrySet()) {
			if(this.linesCovered.containsKey(coverageEntry.getKey())) {
				JacocoLineCoverage addCoverage = coverageEntry.getValue();
				if (addCoverage != null) {
					JacocoLineCoverage coverage = this.getLineCoverage(coverageEntry.getKey());
					if(coverage.getInstructionsCovered() < addCoverage.getInstructionsCovered()){
						this.linesCovered.put(coverageEntry.getKey(), coverageEntry.getValue());
					}

					continue;
				}
			}

			this.linesCovered.put(coverageEntry.getKey(), coverageEntry.getValue());
		}
	}

	/**
	 * Remove lines from the coverage data. (Difference / Subtraction)
	 * @param remove - coverage to remove
	 */
	public void removeLinesCovered(JacocoMethodCoverage remove) {
		Set<Integer> toRemove = new HashSet<>();
		for (Map.Entry<Integer, JacocoLineCoverage> coverageEntry : remove.linesCovered.entrySet()) {
			toRemove.add(coverageEntry.getKey());
		}
		for (Integer line : toRemove) {
			this.linesCovered.remove(line);
		}
	}

	/**
	 * Retain only lines in the coverage data from toRetain. (Intersection)
	 * If a line exists in both: keep the one with the lowest coverage (i.e. min instructions covered).
	 * @param toRetain - coverage to retain
	 */
	public void retainLinesCovered(JacocoMethodCoverage toRetain) {
		Set<Integer> toRemove = new HashSet<>();
		for (Map.Entry<Integer, JacocoLineCoverage> coverageEntry : this.linesCovered.entrySet()) {
			if(toRetain.linesCovered.containsKey(coverageEntry.getKey())) {
				JacocoLineCoverage retainCoverage = toRetain.getLineCoverage(coverageEntry.getKey());
				if (retainCoverage != null) {
					JacocoLineCoverage coverage = coverageEntry.getValue();
					if(retainCoverage.getInstructionsCovered() < coverage.getInstructionsCovered()){
						coverageEntry.setValue(retainCoverage);
					}

					continue;
				}
			}

			toRemove.add(coverageEntry.getKey());
		}

		for (Integer line : toRemove) {
			this.linesCovered.remove(line);
		}
	}

	protected JSONObject serialize() {
		JSONObject jCoverage = new JSONObject();
		jCoverage.put("method", this.method.getFullName());

		// serialize linesCovered
		JSONArray linesCovered = new JSONArray();
		for (JacocoLineCoverage lineCoverage : this.linesCovered.values()) {
			JSONObject jLineCoverage = lineCoverage.serialize();
			linesCovered.put(jLineCoverage);
		}
		jCoverage.put("coverage", linesCovered);

		return jCoverage;
	}

	public static JacocoMethodCoverage deserialize(JSONObject jCoverage, JacocoCoverageReport report) {
		String methodName = jCoverage.getString("method");
		JacocoMethod method = report.getMethodByFullName(methodName);

		if(method == null){
			System.out.println("Could not find method: " + methodName);
		}

		Map<Integer, JacocoLineCoverage> linesCovered = new HashMap<>();
		// parse linesCovered
		JSONArray coverage = jCoverage.getJSONArray("coverage");
		for (Object c : coverage) {
			JSONObject jLineCoverage = (JSONObject)c;
			JacocoLineCoverage lineCoverage = JacocoLineCoverage.deserialize(jLineCoverage, method);
			linesCovered.put(lineCoverage.getLine().getLineNumber(), lineCoverage);
		}

		return new JacocoMethodCoverage(method, linesCovered);
	}
}
