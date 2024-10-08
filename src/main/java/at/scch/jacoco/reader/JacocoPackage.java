package at.scch.jacoco.reader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoPackage.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Representation of a Package in the source code structure of the system under test.
 */
public class JacocoPackage {

	/**
	 * Name of the package.
	 */
	private final String name;

	/**
	 * Classes inside the package.
	 */
	private final Set<JacocoClass> classes;

	public JacocoPackage(String name) {
		super();
		this.name = name;
		this.classes = new HashSet<>();
	}

	public String getName() {
		return name;
	}

	public Set<JacocoClass> getClasses() {
		return classes;
	}

	public JacocoMethod getMethodByFullName(String methodName) {
		for (JacocoClass clazz : classes) {
			if(methodName.startsWith(clazz.getFullName())){
				JacocoMethod found = clazz.getMethodByFullName(methodName);
				if(found != null){
					return found;
				}
			}
		}

		return null;
	}

	/**
	 * Adds a class to this package
	 * @param jacocoClass - class to be added
	 */
	protected void addClass(JacocoClass jacocoClass) {
		this.classes.add(jacocoClass);
	}

	protected JSONObject serialize() {
		JSONObject jPackage = new JSONObject();
		jPackage.put("name", this.name);

		// serialize classes
		JSONArray classes = new JSONArray();
		for (JacocoClass cls : this.classes) {
			JSONObject jClass = cls.serialize();
			classes.put(jClass);
		}
		jPackage.put("classes", classes);
		
		return jPackage;
    }

	protected static JacocoPackage deserialize(JSONObject jPackage) {
		String name = jPackage.getString("name");
		JacocoPackage deserialized = new JacocoPackage(name);

		// parse classes
		JSONArray classes = jPackage.getJSONArray("classes");
		for (Object c : classes) {
			JSONObject jClass = (JSONObject)c;
			JacocoClass.deserialize(jClass, deserialized);
		}

		return deserialized;
	}
}
