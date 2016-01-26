/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scrplugin.bnd;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.Result;
import org.apache.felix.scrplugin.SCRDescriptorGenerator;
import org.apache.felix.scrplugin.Source;
import org.apache.felix.scrplugin.SpecVersion;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * The <code>SCRDescriptorBndPlugin</code> class is a <code>bnd</code> analyzer
 * plugin which generates a service descriptor file based on annotations found
 * in the sources.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SCRDescriptorBndPlugin implements AnalyzerPlugin, Plugin {
	/**
	 * "destdir" parameter, optionally provided in the "-plugin" directive.
	 */
	private static final String DESTDIR = "destdir";

	/**
	 * "generateAccessors" parameter, optionally provided in the "-plugin"
	 * directive.
	 */
	private static final String GENERATE_ACCESSOR = "generateAccessors";

	/**
	 * "strictMode" parameter, optionally provided in the "-plugin" directive.
	 */
	private static final String STRICT_MODE = "strictMode";

	/**
	 * "specVersion" parameter, optionally provided in the "-plugin" directive.
	 */
	private static final String SPECVERSION = "specVersion";

	/**
	 * "log" parameter, which may be provided in the "-plugin" directive.
	 */
	private static final String LOGLEVEL = "log";

    /**
     * "logToFile" parameter, which may be set to false to suppress writing log of plugin action additionally to temp dir. Default: true.
     */
    private static final String LOGTOFILE = "logToFile";

	/**
	 * The name of the directory where the descriptor files are generated into.
	 */
	private File destDir;

	/**
	 * Object allowing to log debug messages using bnd reporter object.
	 */
	private BndLog log;

	/**
	 * This flag controls the generation of the bind/unbind methods.
	 */
	private boolean generateAccessor = true;

	/**
	 * In strict mode the plugin even fails on warnings.
	 */
	private boolean strictMode = false;

	/**
	 * The version of the DS spec this plugin generates a descriptor for. By
	 * default the version is detected by the used tags.
	 */
	private SpecVersion specVersion;

	/**
	 * Bnd plugin properties.
	 */
	private Map<String, String> properties;

	/**
	 * Object used to report logs to bnd.
	 */
	private Reporter reporter;

	/**
	 * Sets the reporter for logging into the bnd logger.
	 */
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	/**
	 * Sets properties which can be specified in the "-plugin" directive. For
	 * example: -plugin
	 * org.apache.felix.scrplugin.bnd.SCRDescriptorBndPlugin;destdir
	 * =target/classes
	 */
	public void setProperties(Map<String, String> map) {
		this.properties = map;
	}

	/**
	 * Scan scr or ds annotation from the target jar.
	 */
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		this.log = new BndLog(reporter, analyzer,
		        parseOption(properties, LOGTOFILE, false));

		try {
			init(analyzer);

			log.info("Analyzing " + analyzer.getBsn());
			final org.apache.felix.scrplugin.Project project = new org.apache.felix.scrplugin.Project();
			project.setClassLoader(new URLClassLoader(getClassPath(analyzer),
					this.getClass().getClassLoader()));
			project.setDependencies(getDependencies(analyzer));
			project.setSources(getClassFiles(analyzer));
			project.setClassesDirectory(destDir.getAbsolutePath());

			// create options
			final Options options = new Options();
			options.setOutputDirectory(destDir);
			options.setGenerateAccessors(generateAccessor);
			options.setStrictMode(strictMode);
			options.setProperties(new HashMap<String, String>());
			options.setSpecVersion(specVersion);

			final SCRDescriptorGenerator generator = new SCRDescriptorGenerator(
					log);

			// setup from plugin configuration
			generator.setOptions(options);
			generator.setProject(project);

			Result r = generator.execute();

			// Embed scr descriptors in target jar
			List<String> scrFiles = r.getScrFiles();
			if (scrFiles != null) {
				StringBuilder sb = new StringBuilder();
				for (String scrFile : scrFiles) {
					log.info("SCR descriptor result file: " + scrFile);
					sb.append(scrFile);
					sb.append(",");
					putResource(analyzer, scrFile);
				}
				sb.setLength(sb.length() - 1);
				addServiceComponentHeader(analyzer, sb.toString());
			}

			// Embed metatype descriptors in target jar
			List<String> metaTypeFiles = r.getMetatypeFiles();
			if (metaTypeFiles != null) {
				for (String metaTypeFile : metaTypeFiles) {
					log.info("Meta Type result file: " + metaTypeFile);
					putResource(analyzer, metaTypeFile);
				}
			}
		} catch (Throwable t) {
			log.error("Got unexpected exception while analyzing",
					t);
		} finally {
			log.close();
		}
		return false; // do not reanalyze bundle classpath because our plugin has not changed it.
	}

	private void addServiceComponentHeader(Analyzer analyzer, String components) {
		Set<String> descriptorsSet = new HashSet<String>();
		String oldComponents = analyzer.getProperty("Service-Component");		
		parseComponents(descriptorsSet, oldComponents);
		parseComponents(descriptorsSet, components);
		
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = descriptorsSet.iterator();
		while (it.hasNext()) {
		    sb.append(it.next());
		    if (it.hasNext()) {
		        sb.append(",");
		    }
		}
		String comps = sb.toString();
		log.info("Setting Service-Component header: " + comps);
		analyzer.setProperty("Service-Component", comps);
	}

    private void parseComponents(Set<String> descriptorsSet, String components) {
        if (components != null && components.length() > 0) {
            for (String comp : components.split(",")) {
                comp = comp.trim();
                if (comp.length() > 0) {
                    descriptorsSet.add(comp);
                }
            }
        }
	}

	private void init(Analyzer analyzer) {
		this.log.setLevel(parseOption(properties, LOGLEVEL,
				BndLog.Level.Warn.toString()));

		String param = parseOption(properties, DESTDIR, new File(analyzer.getBase() + File.separator + "bin").getPath());
		destDir = new File(param);
		if (!destDir.exists() && !destDir.mkdirs()) {
			throw new IllegalArgumentException("Could not create " + destDir
					+ " directory.");
		}

		generateAccessor = parseOption(properties, GENERATE_ACCESSOR,
				generateAccessor);
		strictMode = parseOption(properties, STRICT_MODE, strictMode);
		String version = parseOption(properties, SPECVERSION, null);
		specVersion = SpecVersion.fromName(version);
		if (version != null && specVersion == null) {
			throw new IllegalArgumentException(
					"Unknown spec version specified: " + version);
		}

		if (log.isInfoEnabled()) {
			log.info("Initialized Bnd ScrPlugin: destDir=" + destDir
					+ ", strictMode=" + strictMode
					+ ", specVersion=" + specVersion);
		}
	}

	private String parseOption(Map<String, String> opts, String name, String def) {
		String value = opts.get(name);
		return value == null ? def : value;
	}

	private boolean parseOption(Map<String, String> opts, String name,
			boolean def) {
		String value = opts.get(name);
		return value == null ? def : Boolean.valueOf(value);
	}

	private Collection<Source> getClassFiles(Analyzer analyzer)
			throws Exception {
		ArrayList<Source> files = new ArrayList<Source>();
		Collection<Clazz> expanded = analyzer.getClasses("",
				QUERY.NAMED.toString(), "*");
		for (final Clazz c : expanded) {
			files.add(new Source() {
				public File getFile() {
					log.debug("Found class "
							+ c.getAbsolutePath());
					return new File(c.getAbsolutePath());
				}

				public String getClassName() {
					return c.getFQN();
				}
			});
		}
		return files;
	}

	private URL[] getClassPath(Analyzer a) throws Exception {
		final ArrayList<URL> path = new ArrayList<URL>();
		for (final Jar j : a.getClasspath()) {
			path.add(j.getSource().toURI().toURL());
		}
		log.info("Using claspath: " + path);
		return path.toArray(new URL[path.size()]);
	}

	private void putResource(Analyzer analyzer, String path) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		File f = new File(destDir, path);
		InputStream in = new BufferedInputStream(new FileInputStream(f));
		try {
			int c;
			while ((c = in.read()) != -1) {
				out.write(c);
			}
		} finally {
			in.close();
		}
		byte[] data = out.toByteArray();
		analyzer.getJar().putResource(path, new EmbeddedResource(data, 0));
	}

	private List<File> getDependencies(Analyzer a) {
		ArrayList<File> files = new ArrayList<File>();
		for (final Jar j : a.getClasspath()) {
			File file = j.getSource();
			if (file.isFile()) {
				files.add(file);
			}
		}
		log.info("Using dependencies: " + files);
		return files;
	}
}
