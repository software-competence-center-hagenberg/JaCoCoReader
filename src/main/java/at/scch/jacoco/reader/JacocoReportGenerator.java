package at.scch.jacoco.reader;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.internal.ContentTypeDetector;
import org.jacoco.report.JavaNames;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoReportGenerator.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

public class JacocoReportGenerator {

	private JacocoReportGenerator() {
		super();
	}

	public static JacocoCoverageReport parseExecFile(final File execFile, final File binDir, final File... jars) throws IOException {
		Set<File> jarsSet = new HashSet<>(Arrays.asList(jars));
		return parseExecFile(execFile, binDir, jarsSet);
	}

	public static JacocoCoverageReport parseExecFile(final File execFile, final Set<File> bins, final File... jars) throws IOException {
		Set<File> jarsSet = new HashSet<>(Arrays.asList(jars));
		return parseExecFile(execFile, bins, jarsSet);
	}

	/**
	 * Parse the exec file generate by JaCoCo
	 * @param execFile - exec file
	 * @param binDir - directory containing class files
	 * @param jars - jar files that should be part of the analysis
	 * @return
	 * @throws IOException
	 */
	public static JacocoCoverageReport parseExecFile(final File execFile, final File binDir, final Set<File> jars)
			throws IOException {
		Set<File> bins = new HashSet<>();
		bins.add(binDir);
		return parseExecFile(execFile, bins, jars);
	}

	/**
	 * Parse the exec file generate by JaCoCo
	 * @param execFile - exec file
	 * @param bins - directories containing class files
	 * @param jars - jar files that should be part of the analysis
	 * @return
	 * @throws IOException
	 */
	public static JacocoCoverageReport parseExecFile(final File execFile, final Set<File> bins, final Set<File> jars)
			throws IOException {

		ExecutionDataVisitor visitor = getExecutionDataVisitor(execFile);

		// read merged results, over all sessions
		CoverageBuilder mergedBuilder = getCoverageBuilder(visitor.getMerged(), bins, jars);

		Map<String, JacocoPackage> packages = new HashMap<>();
		Map<String, JacocoMethod> methods = new HashMap<>();
		for (final IClassCoverage cc : mergedBuilder.getClasses()) {
			String pkg = getPackageName(cc);
			JacocoPackage jPkg = packages.get(pkg);
			if(jPkg == null) {
				jPkg = new JacocoPackage(pkg);
				packages.put(pkg, jPkg);
			}
			String className = getClassName(cc);
			JacocoClass jClass = new JacocoClass(jPkg, className);
			for (final IMethodCoverage mc : cc.getMethods()) {
				String signature = getMethodSignature(cc, mc);
				JacocoMethod jMethod = new JacocoMethod(jClass, signature, mc.getComplexityCounter().getTotalCount());
				getLines(mc, pkg, className, signature, jMethod);
				methods.put(jMethod.getFullName(), jMethod);
			}
		}
		
		JacocoCoverageReport report = new JacocoCoverageReport(packages.values());

		for (Map.Entry<String, ExecutionDataStore> entry : visitor.getSessions().entrySet()) {
			if(entry.getKey().equals("No-Test")) {
				continue;
			}

//			System.out.println(entry.getKey());

			CoverageBuilder coverageBuilder = getCoverageBuilder(entry.getValue(), bins, jars);

			JacocoSession session = new JacocoSession(entry.getKey());
			
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String pkg = getPackageName(cc);
				String className = getClassName(cc);
				for (final IMethodCoverage mc : cc.getMethods()) {
					String signature = getMethodSignature(cc, mc);
					String key = pkg + "." + className + "." + signature;
					JacocoMethod jMethod = methods.get(key);
					Map<Integer, JacocoLineCoverage> linesCovered = getLinesCovered(mc, pkg, className, signature, jMethod);
					if(!linesCovered.isEmpty()) {
						JacocoMethodCoverage methodCoverage = new JacocoMethodCoverage(jMethod, linesCovered);
						session.addCoverage(methodCoverage);
					}
				}
			}
			report.addSession(session);
		}
		return report;
	}

	public static ExecutionDataVisitor getExecutionDataVisitor(final File execFile) throws IOException{
		ExecutionDataVisitor visitor = new ExecutionDataVisitor();
		ExecutionDataReader reader = new ExecutionDataReader(new FileInputStream(execFile));
		reader.setExecutionDataVisitor(visitor);
		reader.setSessionInfoVisitor(visitor);
		reader.read();

		return visitor;
	}

	public static CoverageBuilder getCoverageBuilder(final ExecutionDataStore executionDataStore, final File binDir, final File... jars) throws IOException {
		Set<File> bins = new HashSet<>();
		bins.add(binDir);
		return getCoverageBuilder(executionDataStore, bins, jars);
	}

	public static CoverageBuilder getCoverageBuilder(final ExecutionDataStore executionDataStore, final File binDir, final Set<File> jars) throws IOException {
		Set<File> bins = new HashSet<>();
		bins.add(binDir);
		return getCoverageBuilder(executionDataStore, bins, jars);
	}

	public static CoverageBuilder getCoverageBuilder(final ExecutionDataStore executionDataStore, final Set<File> bins, final File... jars) throws IOException {
		Set<File> jarsSet = new HashSet<>(Arrays.asList(jars));
		return getCoverageBuilder(executionDataStore, bins, jarsSet);
	}

	public static CoverageBuilder getCoverageBuilder(final ExecutionDataStore executionDataStore, final Set<File> bins, final Set<File> jars) throws IOException {
		return getCoverageBuilder(executionDataStore, bins, jars, null, null);
	}

	public static CoverageBuilder getCoverageBuilder(final ExecutionDataStore executionDataStore, final Set<File> bins, final Set<File> jars, final Set<String> includes, final Set<String> excludes) throws IOException {
		CoverageBuilder coverageBuilder = new CoverageBuilder();
		Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
		analyzeBins(analyzer, bins, jars, includes, excludes);

		return coverageBuilder;
	}

//	private static void analyzeBins(final Analyzer analyzer, final File binDir, final Set<File> jars) throws IOException {
//		analyzer.analyzeAll(binDir);
//		if (jars != null) {
//			for (File jar : jars) {
//				try {
//					analyzer.analyzeAll(jar);
//				} catch (IOException e) {
//					System.err.println("ERROR: " + e.getMessage());
//					e.printStackTrace();
//				}
//			}
//		}
//	}

	private static void analyzeBins(final Analyzer analyzer, final Set<File> bins, final Set<File> jars, final Set<String> includes, final Set<String> excludes) throws IOException {
		if (bins != null) {
			for (File bin : bins) {
				try {
					analyzeBins(analyzer, bin, includes, excludes);
				} catch (IOException e) {
					System.err.println("ERROR: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		if (jars != null) {
			for (File jar : jars) {
				try {
					analyzeBins(analyzer, jar, includes, excludes);
				} catch (IOException e) {
					System.err.println("ERROR: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private static void analyzeBins(final Analyzer analyzer, final File bins, final Set<String> includes, final Set<String> excludes) throws IOException {
		if(bins.isDirectory()){
			for (final File f : bins.listFiles()) {
				analyzeBins(analyzer, f, includes, excludes);
			}
		} else {
			final InputStream in = new FileInputStream(bins);
			try {
				final ContentTypeDetector detector = new ContentTypeDetector(in);
				switch (detector.getType()) {
					case ContentTypeDetector.CLASSFILE:
						if(includeClassFile(bins.getPath(), includes, excludes)){
							analyzer.analyzeAll(bins);
						}
						break;
					case ContentTypeDetector.ZIPFILE:
						if(includes.isEmpty() && excludes.isEmpty()){
							analyzer.analyzeAll(bins);
						} else {
							ZipInputStream zip = new ZipInputStream(new FileInputStream(bins));
							ZipEntry entry;
							while((entry = nextEntry(zip)) != null){
								if(entry.getName().endsWith(".class") && includeClassFile(entry.getName(), includes, excludes)) {
									analyzer.analyzeAll(zip, bins.getPath() + "@" + entry.getName());
								}
							}
						}
						break;
					case ContentTypeDetector.GZFILE:
						// TODO Handle file type GZFILE, if a filter in includes or excludes is specified.
						if(!includes.isEmpty() || !excludes.isEmpty()){
							System.err.println("Filter are not supported for GZFILE files yet.");
						}
						analyzer.analyzeAll(bins);
						break;
					case ContentTypeDetector.PACK200FILE:
						// TODO Handle file type PACK200FILE, if a filter in includes or excludes is specified.
						if(!includes.isEmpty() || !excludes.isEmpty()){
							System.err.println("Filter are not supported for PACK200FILE files yet.");
						}
						analyzer.analyzeAll(bins);
						break;
					default:
				}
			} finally {
				in.close();
			}
		}
	}

	private static boolean includeClassFile(final String pathToClassFile, final Set<String> includes, final Set<String> excludes){
		return includeClassFile(pathToClassFile, includes) && !excludeClassFile(pathToClassFile, excludes);
	}

	private static boolean includeClassFile(final String pathToClassFile, final Set<String> includes){
		if(includes == null || includes.isEmpty()){
			return true;
		}

		String filePath = pathToClassFile.replaceAll("\\\\", "/");
		for (String include : includes) {
			String regex = getFileRegexFromPattern(include);
			if(filePath.matches(regex)){
				return true;
			}
		}

		return false;
	}

	private static boolean excludeClassFile(final String pathToClassFile, final Set<String> excludes){
		if(excludes == null || excludes.isEmpty()){
			return false;
		}

		String filePath = pathToClassFile.replaceAll("\\\\", "/");
		for (String exclude : excludes) {
			String regex = getFileRegexFromPattern(exclude);
			if(filePath.matches(regex)){
				return true;
			}
		}

		return false;
	}

	private static ZipEntry nextEntry(final ZipInputStream input) {
		try {
            return input.getNextEntry();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getFileRegexFromPattern(String pattern){
        return ".*" + pattern.replaceAll("\\.","/").
				replaceAll("\\*", ".*").
				replaceAll("\\?", ".");
	}

	private static Set<JacocoLine> getLines(final IMethodCoverage mc, String pkg, String className, String methodName, JacocoMethod jMethod) {
		Set<JacocoLine> coveredLines = new HashSet<>();
		for (int i = mc.getFirstLine(); i <= mc.getLastLine(); i++) {
			ILine line = mc.getLine(i);
			if(line.getStatus() != ICounter.EMPTY) {
				coveredLines.add(new JacocoLine(i, jMethod, line.getInstructionCounter().getTotalCount(), line.getBranchCounter().getTotalCount()));
			}
		}
		return coveredLines;
	}
	
	private static Map<Integer, JacocoLineCoverage> getLinesCovered(final IMethodCoverage mc, String pkg, String className, String methodName, JacocoMethod jMethod) {
		Map<Integer, JacocoLineCoverage> coveredLines = new HashMap<>();
		for (int i = mc.getFirstLine(); i <= mc.getLastLine(); i++) {
			ILine line = mc.getLine(i);

//			String fqn = pkg + "." + className + "." + methodName + ":" + i;
//			if(fqn.equals("org.springframework.samples.petclinic.owner.OwnerController.processCreationForm(Owner, BindingResult):67")) {
//				System.out.println(fqn);
//				System.out.println(line.getInstructionCounter().getCoveredCount() + " / " + line.getInstructionCounter().getTotalCount());
//				System.out.println(line.getStatus());
//			}

			if(line.getStatus() != ICounter.EMPTY && line.getStatus() != ICounter.NOT_COVERED) {
				JacocoLine l = jMethod.getLine(i);
				if(l != null){
					coveredLines.put(i, new JacocoLineCoverage(l, line.getBranchCounter().getCoveredCount(), line.getInstructionCounter().getCoveredCount()));
				} else {
					String fqn = pkg + "." + className + "." + methodName + ":" + i;
					System.err.println("No line for coverage in: " + fqn);
				}
			}
		}
		return coveredLines;
	}

	private static String getPackageName(final IClassCoverage cc) {
		String pkg = cc.getPackageName();
		return pkg.replace("/", ".");
	}

	private static String getClassName(final IClassCoverage cc) {
		String[] pkg = cc.getName().split("/");
		return pkg[pkg.length - 1];
	}

	private static String getMethodSignature(final IClassCoverage cc, final IMethodCoverage mc) {
		JavaNames names = new JavaNames();
		return names.getMethodName(cc.getName(), mc.getName(), mc.getDesc(), mc.getSignature());
	}

}
