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
package org.apache.felix.dm.annotation.plugin.bnd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import aQute.service.reporter.Reporter;

/**
 * Clas used to log messages into the bnd logger.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BndLogger extends Logger {
    private final Reporter m_reporter;

    /**
     * Writer to log file, in tmp dir/dmplugin-BSN.log
     */
    private final PrintWriter logWriter;

    /**
     * DateFormat used when logging.
     */
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("E yyyy.MM.dd hh:mm:ss.S");

    /**
     * Enabled log level, which can be configured in bnd plugin declaration.
     */
    private Level logEnabled = Level.Warn;

    /**
     * Creates a new bnd Log implementaiton
     * 
     * @param reporter
     *            the bnd logger
     * @param logLevel
     * @param bsn
     */
    public BndLogger(Reporter reporter, String bsn) {
        m_reporter = reporter;
        File logFilePath = new File(System.getProperty("java.io.tmpdir") + File.separator + "dmplugin"
            + File.separator + bsn + ".log");
        new File(logFilePath.getParent()).mkdirs();

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(logFilePath, false));
        }
        catch (IOException e) {
            reporter.exception(e, "Could not create scrplugin log file: %s", logFilePath);
            writer = null;
        }
        this.logWriter = writer;
    }

    /**
     * Close log file.
     */
    public void close() {
        if (logWriter != null) {
            logWriter.close();
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

    public void debug(String content, Object ... args) {
        if (isDebugEnabled()) {
            m_reporter.trace(content, args);
            logDebug(String.format(content, args), null);
        }
    }

    public boolean isInfoEnabled() {
        return logEnabled.ordinal() >= Level.Info.ordinal();
    }

    public void info(String content, Object ... args) {
        if (isInfoEnabled()) {
            m_reporter.trace(content, args);
            logInfo(String.format(content, args), null);
        }
    }

    public boolean isWarnEnabled() {
        return logEnabled.ordinal() >= Level.Warn.ordinal();
    }

    public void warn(String content, Object ... args) {
        if (isWarnEnabled()) {
            m_reporter.warning(content, args);
            logWarn(String.format(content, args), null);
        }
    }

    public void warn(String content, Throwable err, Object ... args) {
        if (isWarnEnabled()) {
            m_reporter.warning(content, args);
            logWarn(String.format(content, args), err);
        }
    }

    public boolean isErrorEnabled() {
        return logEnabled.ordinal() >= Level.Error.ordinal();
    }

    public void error(String content, Object ... args) {
        m_reporter.error(content, args);
        logErr(String.format(content, args), null);
    }

    public void error(String content, Throwable err, Object ... args) {
        m_reporter.error(content, args);
        logErr(String.format(content, args), err);
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
