/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.felix.logback.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.admin.LoggerAdmin;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

public class LogbackLogListener implements LogListener, LoggerContextListener {

    private static final String EVENTS_BUNDLE = "Events.Bundle";
    private static final String EVENTS_FRAMEWORK = "Events.Framework";
    private static final String EVENTS_SERVICE = "Events.Service";
    private static final String LOG_SERVICE = "LogService";

    volatile LoggerContext loggerContext;
    volatile Logger rootLogger;
    volatile LoggerContextVO loggerContextVO;
    final Map<String, LogLevel> initialLogLevels;
    final org.osgi.service.log.admin.LoggerContext osgiLoggerContext;

    public LogbackLogListener(LoggerAdmin loggerAdmin) {
        osgiLoggerContext = loggerAdmin.getLoggerContext(null);
        initialLogLevels = osgiLoggerContext.getLogLevels();

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

        if (!(loggerFactory instanceof LoggerContext)) {
            throw new IllegalStateException("This bundle only works with logback-classic");
        }

        onStart((LoggerContext)loggerFactory);

        loggerContext.addListener(this);
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void logged(final LogEntry entry) {
        String loggerName = entry.getLoggerName();
        String message = entry.getMessage();
        Object[] arguments = null;
        Level level = from(entry.getLogLevel());
        final AtomicBoolean avoidCallerData = new AtomicBoolean();

        if (EVENTS_BUNDLE.equals(loggerName) ||
            EVENTS_FRAMEWORK.equals(loggerName) ||
            LOG_SERVICE.equals(loggerName)) {

            loggerName = formatBundle(entry.getBundle(), loggerName);
            avoidCallerData.set(true);
        }
        else if (EVENTS_SERVICE.equals(loggerName)) {
            loggerName = formatBundle(entry.getBundle(), loggerName);
            message = message + " {}";
            arguments = new Object[] {entry.getServiceReference()};
            avoidCallerData.set(true);
        }

        Logger logger = loggerContext.getLogger(loggerName);

        // Check to see if there's a logger defined in our configuration and
        // if there is, then make sure it's handled as an override for the
        // effective level.
        if (!logger.equals(rootLogger) && !logger.isEnabledFor(level)) {
            return;
        }

        LoggingEvent le = new LoggingEvent() {

            @Override
            public StackTraceElement[] getCallerData() {
                if (avoidCallerData.get() || callerData != null)
                    return callerData;
                return callerData = getCallerData0(entry.getLocation());
            }

            private volatile StackTraceElement[] callerData;

        };

        le.setArgumentArray(arguments);
        le.setMessage(message);
        le.setLevel(level);
        le.setLoggerContextRemoteView(loggerContextVO);
        le.setLoggerName(loggerName);
        le.setThreadName(entry.getThreadInfo());
        le.setThrowableProxy(getThrowableProxy(entry.getException()));
        le.setTimeStamp(entry.getTime());

        rootLogger.callAppenders(le);
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
        Map<String, LogLevel> updatedLevels = osgiLoggerContext.getLogLevels();

        if (Level.OFF.equals(level)) {
            updatedLevels.remove(logger.getName());
        }
        else {
            updatedLevels.put(logger.getName(), from(level));
        }

        osgiLoggerContext.setLogLevels(updatedLevels);
    }

    @Override
    public void onStart(LoggerContext context) {
        loggerContext = context;
        rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        loggerContextVO = loggerContext.getLoggerContextRemoteView();

        Map<String, LogLevel> updatedLevels = updateLevels(loggerContext, initialLogLevels);

        osgiLoggerContext.setLogLevels(updatedLevels);
    }

    @Override
    public void onStop(LoggerContext context) {
        osgiLoggerContext.setLogLevels(initialLogLevels);
    }

    @Override
    public void onReset(LoggerContext context) {
        onStart(context);
    }

    String formatBundle(Bundle bundle, String loggerName) {
        return new StringBuilder().append(
            loggerName
        ).append(
            "."
        ).append(
            bundle.getSymbolicName()
        ).toString();
    }

    LogLevel from(Level level) {
        if (Level.ALL.equals(level)) {
            return LogLevel.TRACE;
        }
        else if (Level.DEBUG.equals(level)) {
            return LogLevel.DEBUG;
        }
        else if (Level.ERROR.equals(level)) {
            return LogLevel.ERROR;
        }
        else if (Level.INFO.equals(level)) {
            return LogLevel.INFO;
        }
        else if (Level.TRACE.equals(level)) {
            return LogLevel.TRACE;
        }
        else if (Level.WARN.equals(level)) {
            return LogLevel.WARN;
        }

        return LogLevel.WARN;
    }

    Level from(LogLevel logLevel) {
        switch (logLevel) {
            case AUDIT:
                return Level.TRACE;
            case DEBUG:
                return Level.DEBUG;
            case ERROR:
                return Level.ERROR;
            case INFO:
                return Level.INFO;
            case TRACE:
                return Level.TRACE;
            case WARN:
            default:
                return Level.WARN;
        }
    }

    StackTraceElement[] getCallerData0(StackTraceElement stackTraceElement) {
        StackTraceElement[] callerData = CallerData.extract(
            new Throwable(),
            org.osgi.service.log.Logger.class.getName(),
            loggerContext.getMaxCallerDataDepth(),
            loggerContext.getFrameworkPackages());

        if (stackTraceElement != null) {
            if (callerData.length == 0) {
                callerData = new StackTraceElement[] {stackTraceElement};
            }
            else {
                StackTraceElement[] copy = new StackTraceElement[callerData.length + 1];
                copy[0] = stackTraceElement;
                System.arraycopy(callerData, 0, copy, 1, callerData.length);
                callerData = copy;
            }
        }

        return callerData;
    }

    ThrowableProxy getThrowableProxy(Throwable t) {
        if (t == null)
            return null;

        ThrowableProxy throwableProxy = new ThrowableProxy(t);

        if (loggerContext.isPackagingDataEnabled()) {
            throwableProxy.calculatePackagingData();
        }

        return throwableProxy;
    }

    Map<String, LogLevel> updateLevels(LoggerContext loggerContext, Map<String, LogLevel> levels) {
        Map<String, LogLevel> copy = new HashMap<String, LogLevel>(levels);

        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        LogLevel rootLevel = from(root.getLevel());
        copy.put(org.osgi.service.log.Logger.ROOT_LOGGER_NAME, rootLevel);
        copy.put(EVENTS_BUNDLE, LogLevel.TRACE);
        copy.put(EVENTS_FRAMEWORK, LogLevel.TRACE);
        copy.put(EVENTS_SERVICE, LogLevel.TRACE);
        copy.put(LOG_SERVICE, LogLevel.TRACE);

        for (Logger logger : loggerContext.getLoggerList()) {
            String name = logger.getName();
            Level level = logger.getLevel();

            if (level != null) {
                copy.remove(name);

                if (level != Level.OFF) {
                    copy.put(name, from(level));
                }
            }
        }

        return copy;
    }

}
