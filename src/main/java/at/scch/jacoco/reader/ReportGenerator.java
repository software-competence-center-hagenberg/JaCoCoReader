/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *
 *******************************************************************************/
package at.scch.jacoco.reader;

import org.apache.commons.cli.*;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * This example creates a report in a given format (or HTML if non is given)
 * for eclipse like projects based on a single execution data store called
 * jacoco.exec. The report contains no grouping information.
 * <p>
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class ReportGenerator {

    public enum Format {
        HTML,
        XML,
        CSV;

        public static Format fromString(String format) {
            if (format == null) {
                return null;
            }
            switch (format.toUpperCase()) {
                case "HTML":
                    return HTML;
                case "XML":
                    return XML;
                case "CSV":
                    return CSV;
                default:
                    return null;
            }
        }
    }

    private final String title;

    private final File executionDataFile;
    private final Set<File> classDirectories;
    private final Set<File> jarFiles;
    private final File sourceDirectory;
    private final File reportDirectory;

    private final Set<String> includes;
    private final Set<String> excludes;

    private final Format format;

    /**
     * Create a new generator based for the given project.
     *
     * @param projectDirectory
     */
    public ReportGenerator(final File projectDirectory) {
        this(projectDirectory.getName(),
                new File(projectDirectory, "jacoco.exec"),
                new File(projectDirectory, "bin"),
                null,
                new File(projectDirectory, "src"),
                new File(projectDirectory, "coveragereport"));
    }

    /**
     * @param title             - project title
     * @param executionDataFile - coverage data file (e.g. jacoco.exec)
     * @param classesDirectory  - bin directory with Java byte code
     * @param sourceDirectory   - src directory with Java source files
     * @param reportDirectory   - target directory for the generated HTML report
     */
    public ReportGenerator(String title, File executionDataFile, File classesDirectory, File sourceDirectory, File reportDirectory) {
        this(title, executionDataFile, classesDirectory, null, sourceDirectory, reportDirectory);
    }

    /**
     * @param title             - project title
     * @param executionDataFile - coverage data file (e.g. jacoco.exec)
     * @param classesDirectory  - bin directory with Java byte code
     * @param sourceDirectory   - src directory with Java source files
     * @param reportDirectory   - target directory for the generated HTML report
     * @param format            - format the report should be generated in (HTML, XML, CSV)
     */
    public ReportGenerator(String title, File executionDataFile, File classesDirectory, File sourceDirectory, File reportDirectory, Format format) {
        this(title, executionDataFile, classesDirectory, null, sourceDirectory, reportDirectory, format);
    }

    /**
     * @param title             - project title
     * @param executionDataFile - coverage data file (e.g. jacoco.exec)
     * @param classesDirectory  - bin directory with Java byte code
     * @param jarFiles          - jar files for project dependencies
     * @param sourceDirectory   - src directory with Java source files
     * @param reportDirectory   - target directory for the generated report
     */
    public ReportGenerator(String title, File executionDataFile, File classesDirectory, Set<File> jarFiles, File sourceDirectory, File reportDirectory) {
        this(title, executionDataFile, classesDirectory, null, sourceDirectory, reportDirectory, Format.HTML);
    }

    /**
     * @param title             - project title
     * @param executionDataFile - coverage data file (e.g. jacoco.exec)
     * @param classesDirectory  - bin directory with Java byte code
     * @param jarFiles          - jar files for project dependencies
     * @param sourceDirectory   - src directory with Java source files
     * @param reportDirectory   - target directory for the generated report
     * @param format            - format the report should be generated in (HTML, XML, CSV)
     */
    public ReportGenerator(String title, File executionDataFile, File classesDirectory, Set<File> jarFiles, File sourceDirectory, File reportDirectory, Format format) {
        this.title = title;
        this.executionDataFile = executionDataFile;
        this.classDirectories = new HashSet<>();
        this.classDirectories.add(classesDirectory);
        this.jarFiles = jarFiles;
        this.sourceDirectory = sourceDirectory;
        this.reportDirectory = reportDirectory;
        this.format = format;
        this.includes = null;
        this.excludes = null;
    }

    /**
     * @param title             - project title
     * @param executionDataFile - coverage data file (e.g. jacoco.exec)
     * @param classDirectories  - bin directories with Java byte code
     * @param jarFiles          - jar files for project dependencies
     * @param sourceDirectory   - src directory with Java source files
     * @param reportDirectory   - target directory for the generated report
     * @param format            - format the report should be generated in (HTML, XML, CSV)
     */
    public ReportGenerator(String title, File executionDataFile, Set<File> classDirectories, Set<File> jarFiles, File sourceDirectory, File reportDirectory, Format format, Set<String> includes, Set<String> excludes) {
        this.title = title;
        this.executionDataFile = executionDataFile;
        this.classDirectories = classDirectories;
        this.jarFiles = jarFiles;
        this.sourceDirectory = sourceDirectory;
        this.reportDirectory = reportDirectory;
        this.format = format;
        this.includes = includes;
        this.excludes = excludes;
    }

    public void create() throws IOException {
        create(false);
    }

    /**
     * Create the report.
     *
     * @throws IOException
     */
    public void create(boolean perSession) throws IOException {
        ExecutionDataVisitor visitor = JacocoReportGenerator.getExecutionDataVisitor(executionDataFile);

        CoverageBuilder mergedBuilder = JacocoReportGenerator.getCoverageBuilder(visitor.getMerged(), classDirectories, jarFiles, includes, excludes);
        createReport(mergedBuilder, visitor.getSessionInfos(), visitor.getMerged(), reportDirectory, "complete");

        if (perSession) {
            int count = 0;
            for (Map.Entry<String, ExecutionDataStore> entry : visitor.getSessions().entrySet()) {
                File dir;
                if (this.format == Format.HTML) {
                    dir = new File(reportDirectory, entry.getKey());
                } else {
                    dir = reportDirectory;
                }

                CoverageBuilder sessionBuilder = JacocoReportGenerator.getCoverageBuilder(entry.getValue(), classDirectories, jarFiles, includes, excludes);
                List<SessionInfo> sessionInfos = new LinkedList<>();
                sessionInfos.add(visitor.getSession(entry.getKey()));

                createReport(sessionBuilder, sessionInfos, entry.getValue(), dir, count + "-" + entry.getKey());

                count++;
            }
        }
    }

    private void createReport(final CoverageBuilder coverageBuilder, final List<SessionInfo> sessionInfos, final ExecutionDataStore executionDataStore, final File reportDirectory, final String reportName)
            throws IOException {

        final IBundleCoverage bundleCoverage = coverageBuilder.getBundle(title);

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        IReportVisitor visitor;
        switch (this.format) {
            case XML:
                final XMLFormatter xmlFormatter = new XMLFormatter();
                File xmlFile = new File(reportDirectory, reportName + ".xml");
                visitor = xmlFormatter.createVisitor(new BufferedOutputStream(new FileOutputStream(xmlFile)));
                break;
            case CSV:
                final CSVFormatter csvFormatter = new CSVFormatter();
                File csvFile = new File(reportDirectory, reportName + ".csv");
                visitor = csvFormatter.createVisitor(new BufferedOutputStream(new FileOutputStream(csvFile)));
                break;
            case HTML:
            default:
                final HTMLFormatter htmlFormatter = new HTMLFormatter();
                visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(reportDirectory));
        }

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(sessionInfos, executionDataStore.getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(sourceDirectory, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();

    }

    /**
     * Starts the report generation process
     *
     * @param args Arguments to the application. This will be the location of the
     *             eclipse projects that will be used to generate reports for
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {

        Options options = new Options();
        Option titleOption = Option.builder("t")
                .longOpt("title")
                .argName("titleString")
                .hasArg()
                .desc("Title of the project")
                .required()
                .build();
        Option execOption = Option.builder("e")
                .longOpt("exec")
                .argName("execPath")
                .hasArg()
                .desc("Path to the exec file from recording coverage with JaCoCo")
                .required()
                .build();
        Option binariesOption = Option.builder("b")
                .longOpt("binaries")
                .argName("binaryPaths")
                .hasArg()
                .desc("List of paths to the binaries (directory, class files, JAR files) to analyze. The list entries are separated by a semicolon (;) in Windows and a colon (:) in Unix.")
                .required()
                .build();
        Option includesOption = Option.builder("includes")
                .argName("regex")
                .hasArg()
                .desc("A list of class names that should be included in execution analysis. The list entries are separated by a colon (:) and may use wildcard characters (* and ?).")
                .build();
        Option excludesOption = Option.builder("excludes")
                .argName("regex")
                .hasArg()
                .desc("A list of class names that should be excluded from execution analysis. The list entries are separated by a colon (:) and may use wildcard characters (* and ?).")
                .build();
        Option reportOption = Option.builder("r")
                .longOpt("reportDir")
                .argName("reportPath")
                .hasArg()
                .desc("Path to where the report should be generated")
                .required()
                .build();
        Option sourcesOption = Option.builder("s")
                .longOpt("sourceDir")
                .argName("sourcePath")
                .hasArg()
                .desc("Path to the source files for the analyzed binaries (Optional: If not passed a generated HTML report cannot display source code.)")
                .build();
        Option formatOption = Option.builder("f")
                .longOpt("format")
                .argName("<HTML|XML|CSV>")
                .hasArg()
                .desc("Format of the generated report (One of: HTML, XML, CSV). (Optional: By default HTML, if not specified differently.)")
                .build();

//        Option jarsOption = new Option("j", "jars", true, "List of paths to the jar files to analyze"); // TODO JARs and other class binaries are passed exactly the same, so I think we can remove this and just pass everything in a binaries option.

        options.addOption(titleOption);
        options.addOption(execOption);
        options.addOption(binariesOption);
        options.addOption(includesOption);
        options.addOption(excludesOption);
        options.addOption(sourcesOption);
        options.addOption(reportOption);
        options.addOption(formatOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar jaCoCoReader [OPTIONS]", options);
            return;
        }

        String sourceDir = null;
        Format format = Format.HTML;
        if(cmd.hasOption('s')){
            sourceDir = cmd.getOptionValue('s');
        }
        if(cmd.hasOption('f')){
            Format f = Format.fromString(cmd.getOptionValue('f'));
            if(f != null){
                format = f;
            }
        }

        String title = cmd.getOptionValue('t');
        String executionData = cmd.getOptionValue('e');
        String classesDir = cmd.getOptionValue('b');
        String reportDir = cmd.getOptionValue('r');

        Set<String> classesDirs;
        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.contains("win")){
            String[] split = classesDir.split(";");
            classesDirs = new HashSet<>(Arrays.asList(split));
        } else {
            String[] split = classesDir.split(":");
            classesDirs = new HashSet<>(Arrays.asList(split));
        }

        Set<String> includes = null;
        Set<String> excludes = null;
        if(cmd.hasOption("includes")){
            includes = new HashSet<>(Arrays.asList(cmd.getOptionValue("includes").split(":")));
        }
        if(cmd.hasOption("excludes")){
            excludes = new HashSet<>(Arrays.asList(cmd.getOptionValue("excludes").split(":")));
        }

        generate(title, executionData, classesDirs, sourceDir, reportDir, format, includes, excludes);
    }

    public static void generate(final String title, final String executionData, final Set<String> classesDir, final String sourceDir, final String reportDir, Format format, final Set<String> includes, final Set<String> excludes) throws IOException {
        File f = new File(reportDir);
        if (f.exists()) {
            f.mkdirs();
        }

        Set<File> classDirectories = new HashSet<>();
        classesDir.forEach(cd -> classDirectories.add(new File(cd)));

        ReportGenerator generator = new ReportGenerator(
                title,
                new File(executionData),
                classDirectories,
                null,
                sourceDir != null ? new File(sourceDir) : null,
                f,
                format,
                includes,
                excludes
        );
        generator.create();
    }
}
