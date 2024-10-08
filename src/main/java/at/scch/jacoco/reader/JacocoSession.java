package at.scch.jacoco.reader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoSession.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Represents a session inside the JaCoCo coverage report
 */
public class JacocoSession {

	/**
	 * Session ID
	 */
	private final String id;

	/**
	 * Coverage data of this session, per method
	 */
	private final Map<String, JacocoMethodCoverage> coverage;
	
	public JacocoSession(String id) {
		super();
		this.id = id;
		this.coverage = new HashMap<>();
	}

	public String getId() {
		return id;
	}

	public Map<String, JacocoMethodCoverage> getCoverage() {
		return coverage;
	}

	public void addCoverage(JacocoMethodCoverage methodCoverage) {
		this.coverage.put(methodCoverage.getMethod().getFullName(), methodCoverage); 
	}

	public JacocoMethodCoverage getCoverage(String fullName) {
		return this.coverage.get(fullName);
	}

	public boolean coversLine(JacocoLine line) {
		JacocoMethodCoverage methodCoverage = coverage.get(line.getMethod().getFullName());
		if(methodCoverage == null){
			return false;
		}

		return methodCoverage.isLineCovered(line.getLineNumber());
	}

	public int getNumberOfCoveredMethods() {
		return this.coverage.size();
	}

	public long getNumberOfLinesCovered() {
		long linesCovered = 0;
		for (JacocoMethodCoverage m : this.coverage.values()) {
			linesCovered += m.getNumberOfLinesCovered();
		}
		return linesCovered;
	}

	/**
	 * Add the coverage data of session to this.
	 * Union operation.
	 * @param session
	 */
	public void add(JacocoSession session) {
		for(Map.Entry<String, JacocoMethodCoverage> entry : session.coverage.entrySet()) {
			JacocoMethodCoverage jMethodCoverage = this.coverage.get(entry.getKey());
			if(jMethodCoverage == null) {
				jMethodCoverage = new JacocoMethodCoverage(entry.getValue().getMethod());
				jMethodCoverage.addLinesCovered(entry.getValue());
				this.coverage.put(entry.getKey(), jMethodCoverage);
			} else {
				jMethodCoverage.addLinesCovered(entry.getValue());
			}
		}
	}

	/**
	 * Remove the coverage data of session from this
	 * @param session
	 */
	public void remove(JacocoSession session) {
		for(Map.Entry<String, JacocoMethodCoverage> entry : session.coverage.entrySet()) {
			JacocoMethodCoverage jMethodCoverage = this.coverage.get(entry.getKey());
			if(jMethodCoverage != null) {
				jMethodCoverage.removeLinesCovered(entry.getValue());
				if(jMethodCoverage.getNumberOfLinesCovered() == 0){
					this.coverage.remove(entry.getKey());
				}
			}
		}
	}

	/**
	 * Retail the coverage data of session in this.
	 * Intersection operation.
	 * @param session
	 */
	public void retain(JacocoSession session) {
		Set<String> toRemove = new HashSet<>();
		for(Map.Entry<String, JacocoMethodCoverage> entry : this.coverage.entrySet()) {
			JacocoMethodCoverage jMethodCoverage = session.coverage.get(entry.getKey());
			if(jMethodCoverage != null) {
				entry.getValue().retainLinesCovered(jMethodCoverage);
				if(entry.getValue().getNumberOfLinesCovered() == 0){
					toRemove.add(entry.getKey());
				}
			} else {
				toRemove.add(entry.getKey());
			}
		}
		for(String key : toRemove) {
			this.coverage.remove(key);
		}
	}

	protected JSONObject serialize() {
		JSONObject jSession = new JSONObject();
		jSession.put("id", this.id);

		// serialize coverage
		JSONArray coverage = new JSONArray();
		for (JacocoMethodCoverage methodCoverage : this.coverage.values()) {
			JSONObject jCoverage = methodCoverage.serialize();
			coverage.put(jCoverage);
		}
		jSession.put("coverage", coverage);

		return jSession;
    }

	public static JacocoSession deserialize(JSONObject jSession, JacocoCoverageReport report) {
		String id = jSession.getString("id");
		JacocoSession deserialized = new JacocoSession(id);

		// parse coverage
		JSONArray coverage = jSession.getJSONArray("coverage");
		for (Object c : coverage) {
			JSONObject jCoverage = (JSONObject)c;
			JacocoMethodCoverage methodCoverage = JacocoMethodCoverage.deserialize(jCoverage, report);
			deserialized.addCoverage(methodCoverage);
		}

		return deserialized;
	}
}
