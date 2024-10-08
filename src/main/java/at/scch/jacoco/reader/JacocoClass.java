package at.scch.jacoco.reader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoClass.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Representation of a Class in the source code structure of the system under test.
 */
public class JacocoClass {

	/**
	 * Name of the class.
	 */
	private final String name;

	/**
	 * Package this class is contained in.
	 */
	private final JacocoPackage package_;

	/**
	 * Methods inside the class.
	 */
	private final Set<JacocoMethod> methods;

	public JacocoClass(JacocoPackage package_, String name) {
		super();
		this.package_ = package_;
		this.package_.addClass(this);
		this.name = name;
		this.methods = new HashSet<>();
	}

	public String getName() {
		return name;
	}

	public Set<JacocoMethod> getMethods() {
		return methods;
	}

	public JacocoMethod getMethodByFullName(String methodName) {
		for (JacocoMethod method : methods) {
			if(methodName.equals(method.getFullName())){
				return method;
			}
		}

		return null;
	}

	/**
	 * Adds a method to this class
	 * @param jacocoMethod - method to be added
	 */
	protected void addMethod(JacocoMethod jacocoMethod) {
		this.methods.add(jacocoMethod);
	}

	/**
	 * Get the full name of this class inside its package.
	 * @return package name and class name
	 */
	public String getFullName() {
		return this.package_.getName() + "." + this.name;
	}

	protected JSONObject serialize() {
		JSONObject jClass = new JSONObject();
		jClass.put("name", this.name);

		// serialize methods
		JSONArray methods = new JSONArray();
		for (JacocoMethod method : this.methods) {
			JSONObject jMethod = method.serialize();
			methods.put(jMethod);
		}
		jClass.put("methods", methods);

		return jClass;
	}

	public static JacocoClass deserialize(JSONObject jClass, JacocoPackage package_) {
		String name = jClass.getString("name");
		JacocoClass deserialized = new JacocoClass(package_, name);

		// parse methods
		JSONArray methods = jClass.getJSONArray("methods");
		for (Object m : methods) {
			JSONObject jMethod = (JSONObject)m;
			JacocoMethod.deserialize(jMethod, deserialized);
		}

		return deserialized;
	}
}
