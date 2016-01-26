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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.felix.scrplugin.Log;

import aQute.bnd.osgi.Analyzer;
import aQute.service.reporter.Reporter;

/**
 * Scrplugin Log implementation, which redirects log to both bnd "Reporter" and
 * to /tmp/scrplugin/<BundleSymbolicName>.log
 */
public class BndLog implements Log {
	/**
	 * The BndLib logging reported.
	 */
	private final Reporter reporter;

	/**
	 * Enabled log level, which can be configured in bnd scrplugin declaration.
	 */
	private Level logEnabled = Level.Warn;

	/**
	 * Writer to log file, in tmp dir/scrplugin-BSN.log
	 */
	private final PrintWriter logWriter;

	/**
	 * The Bnd Analyzer (only used to enable trace mode if at least info log is enabled).
	 */
	private final Analyzer analyzer;

	/**
	 * Was Reporter traces enabled before our plugin is running ?
	 */
	private boolean previousTrace;

	/**
	 * DateFormat used when logging.
	 */
	private final static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"E yyyy.MM.dd hh:mm:ss.S");
	
	/**
	 * Log Levels.
	 */
	enum Level {
		Error, Warn, Info, Debug
	}

	/**
	 * Creates a new bnd Log implementaiton
	 * 
	 * @param reporter
	 *            the bnd logger
	 * @param analyzer
	 * @param logToFile
	 *            Set to false to suppress writing log of plugin action additionally to temp dir.
	 */
	BndLog(Reporter reporter, Analyzer analyzer, boolean logToFile) {
		this.reporter = reporter;
		this.analyzer = analyzer;
		String bsn = analyzer.getBsn();
        
        if (logToFile) {
            File logFilePath = new File(System.getProperty("java.io.tmpdir")
            		+ File.separator + "scrplugin" + File.separator + bsn + ".log");
            new File(logFilePath.getParent()).mkdirs();
            
            PrintWriter writer = null;
            try {
            	writer = new PrintWriter(new FileWriter(logFilePath, false));
            } catch (IOException e) {
            	reporter.exception(e, "Could not create scrplugin log file: %s",
            			logFilePath);
            	writer = null;
            }
            this.logWriter = writer;
        }
        else {
            this.logWriter = null;
        }
	}

	/**
	 * Close log file.
	 */
	public void close() {
		if (logWriter != null) {
			logWriter.close();
		}
		if (this.logEnabled.ordinal() >= Level.Info.ordinal()) {
			this.analyzer.setTrace(this.previousTrace);
		}		
	}

	/**
	 * Sets the enabled log level.
	 * 
	 * @param level
	 *            the enabled level ("Error", "Warn", "Info", or "Debug")
	 */
	public void setLevel(String level) {
		try {
			level = Character.toUpperCase(level.charAt(0))
					+ level.substring(1).toLowerCase();
			this.logEnabled = Level.valueOf(level);
			if (this.logEnabled.ordinal() >= Level.Info.ordinal()) {
				// We have to enable traces, if not the bnd reporter won't log info or debug messages.
				this.previousTrace = analyzer.isTrace();
				analyzer.setTrace(true);
			}
		} catch (IllegalArgumentException e) {
			this.logEnabled = Level.Warn;
			warn("Bnd scrplugin logger initialized with invalid log level: "
					+ level);
		}
	}

	// Reporter

	public boolean isDebugEnabled() {
		return logEnabled.ordinal() >= Level.Debug.ordinal();
	}

	public boolean isInfoEnabled() {
		return logEnabled.ordinal() >= Level.Info.ordinal();
	}

	public boolean isWarnEnabled() {
		return logEnabled.ordinal() >= Level.Warn.ordinal();
	}

	public boolean isErrorEnabled() {
		return logEnabled.ordinal() >= Level.Error.ordinal();
	}

	public void debug(String content) {
		if (isDebugEnabled()) {
			reporter.trace("%s", content);
			logDebug(content, null);
		}
	}

	public void debug(String content, Throwable error) {
		if (isDebugEnabled()) {
			reporter.trace("%s:%s", content, toString(error));
			logDebug(content, error);
		}
	}

	public void debug(Throwable error) {
		if (isDebugEnabled()) {
			reporter.trace("%s", toString(error));
			logDebug("exception", error);
		}
	}

	public void info(String content) {
		if (isInfoEnabled()) {
			reporter.trace("%s", content);
			logInfo(content, null);
		}
	}

	public void info(String content, Throwable error) {
		if (isInfoEnabled()) {
			reporter.trace("%s:%s", content, toString(error));
			logInfo(content, error);
		}
	}

	public void info(Throwable error) {
		if (isInfoEnabled()) {
			reporter.trace("%s", toString(error));
			logInfo("exception:", error);
		}
	}

	public void warn(String content) {
		if (isWarnEnabled()) {
			reporter.warning("%s", content);
			logWarn(content, null);
		}
	}

	public void warn(String content, Throwable error) {
		if (isWarnEnabled()) {
			reporter.warning("%s:%s", content, toString(error));
			logWarn(content, error);
		}
	}

	public void warn(Throwable error) {
		if (isWarnEnabled()) {
			reporter.warning("%s", toString(error));
			logWarn("exception:", error);
		}
	}

	public void warn(String content, String location, int lineNumber) {
		warn(String.format("%s [%s,%d]", content, location, lineNumber));
	}

	public void warn(String content, String location, int lineNumber,
			int columNumber) {
		warn(String.format("%s [%s,%d:%d]", content, location, lineNumber,
				columNumber));
	}

	public void error(String content) {
		reporter.error("%s", content);
		logErr(content, null);
	}

	public void error(String content, Throwable error) {
		reporter.error("%s:%s", content, toString(error));
		logErr(content, error);
	}

	public void error(Throwable error) {
		reporter.error("%s", toString(error));
		logErr("exception:", error);
	}

	public void error(String content, String location, int lineNumber) {
		error(String.format("%s [%s,%d]", content, location, lineNumber));
	}

	public void error(String content, String location, int lineNumber,
			int columnNumber) {
		error(String.format("%s [%s,%d:%d]", content, location, lineNumber,
				columnNumber));
	}

	private void logErr(String msg, Throwable t) {
		log(Level.Error, msg, t);
	}

	private void logWarn(String msg, Throwable t) {
		log(Level.Warn, msg, t);
	}

	private void logInfo(String msg, Throwable t) {
		log(Level.Info, msg, t);
	}

	private void logDebug(String msg, Throwable t) {
		log(Level.Debug, msg, t);
	}

	private void log(Level level, String msg, Throwable t) {
		if (logWriter != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(dateFormat.format(new Date()));
			sb.append(" - ");
			sb.append(level);
			sb.append(": ");
			sb.append(msg);
			if (t != null) {
				sb.append(" - ").append(toString(t));
			}
			logWriter.println(sb.toString());
		}
	}

	private static String toString(Throwable e) {
		StringWriter buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		e.printStackTrace(pw);
		return (buffer.toString());
	}
}
