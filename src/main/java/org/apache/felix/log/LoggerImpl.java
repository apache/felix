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
package org.apache.felix.log;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;

public class LoggerImpl implements Logger {

    private static final String ESCAPE = "\\";
    private static final String MAX_CHAR = "\uFFFF";
    private static final String BRACE_CLOSE = "}";
    private static final String BRACE_OPEN = "{";

    protected final String m_name;
    protected final Bundle m_bundle;
    protected final Log m_log;
    protected final LoggerAdminImpl m_loggerAdmin;

    public LoggerImpl(final String name, final Bundle bundle, final Log log, final LoggerAdminImpl loggerAdmin) {
        m_name = name;
        m_bundle = bundle;
        m_log = log;
        m_loggerAdmin = loggerAdmin;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public boolean isTraceEnabled() {
        return m_loggerAdmin.getLoggerContext(m_bundle, m_name).getEffectiveLogLevel(m_name).implies(LogLevel.TRACE);
    }

    void trace(String message, ServiceReference<?> serviceReference, Throwable t) {
        if (!isTraceEnabled()) return;
        m_log.log(m_name, m_bundle, serviceReference, LogLevel.TRACE, message, t);
    }

    @Override
    public void trace(String message) {
        trace(message, (ServiceReference<?>)null, null);
    }

    @Override
    public void trace(String format, Object arg) {
        LogParameters logParameters = getLogParameters(arg);
        trace(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        LogParameters logParameters = getLogParameters(arg1, arg2);
        trace(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void trace(String format, Object... arguments) {
        LogParameters logParameters = getLogParameters(arguments);
        trace(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
        if (isTraceEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return m_loggerAdmin.getLoggerContext(m_bundle, m_name).getEffectiveLogLevel(m_name).implies(LogLevel.DEBUG);
    }

    void debug(String message, ServiceReference<?> serviceReference, Throwable t) {
        if (!isDebugEnabled()) return;
        m_log.log(m_name, m_bundle, serviceReference, LogLevel.DEBUG, message, t);
    }

    @Override
    public void debug(String message) {
        debug(message, (ServiceReference<?>)null, null);
    }

    @Override
    public void debug(String format, Object arg) {
        LogParameters logParameters = getLogParameters(arg);
        debug(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        LogParameters logParameters = getLogParameters(arg1, arg2);
        debug(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void debug(String format, Object... arguments) {
        LogParameters logParameters = getLogParameters(arguments);
        debug(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
        if (isDebugEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return m_loggerAdmin.getLoggerContext(m_bundle, m_name).getEffectiveLogLevel(m_name).implies(LogLevel.INFO);
    }

    void info(String message, ServiceReference<?> serviceReference, Throwable t) {
        if (!isInfoEnabled()) return;
        m_log.log(m_name, m_bundle, serviceReference, LogLevel.INFO, message, t);
    }

    @Override
    public void info(String message) {
        info(message, (ServiceReference<?>)null, null);
    }

    @Override
    public void info(String format, Object arg) {
        LogParameters logParameters = getLogParameters(arg);
        info(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        LogParameters logParameters = getLogParameters(arg1, arg2);
        info(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void info(String format, Object... arguments) {
        LogParameters logParameters = getLogParameters(arguments);
        info(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
        if (isInfoEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return m_loggerAdmin.getLoggerContext(m_bundle, m_name).getEffectiveLogLevel(m_name).implies(LogLevel.WARN);
    }

    void warn(String message, ServiceReference<?> serviceReference, Throwable t) {
        if (!isWarnEnabled()) return;
        m_log.log(m_name, m_bundle, serviceReference, LogLevel.WARN, message, t);
    }

    @Override
    public void warn(String message) {
        warn(message, (ServiceReference<?>)null, null);
    }

    @Override
    public void warn(String format, Object arg) {
        LogParameters logParameters = getLogParameters(arg);
        warn(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        LogParameters logParameters = getLogParameters(arg1, arg2);
        warn(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void warn(String format, Object... arguments) {
        LogParameters logParameters = getLogParameters(arguments);
        warn(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
        if (isWarnEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return m_loggerAdmin.getLoggerContext(m_bundle, m_name).getEffectiveLogLevel(m_name).implies(LogLevel.ERROR);
    }

    void error(String message, ServiceReference<?> serviceReference, Throwable t) {
        if (!isErrorEnabled()) return;
        m_log.log(m_name, m_bundle, serviceReference, LogLevel.ERROR, message, t);
    }

    @Override
    public void error(String message) {
        error(message, (ServiceReference<?>)null, null);
    }

    @Override
    public void error(String format, Object arg) {
        LogParameters logParameters = getLogParameters(arg);
        error(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        LogParameters logParameters = getLogParameters(arg1, arg2);
        error(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void error(String format, Object... arguments) {
        LogParameters logParameters = getLogParameters(arguments);
        error(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
        if (isErrorEnabled()) {
            consumer.accept(this);
        }
    }

    public void audit(String message, ServiceReference<?> serviceReference, Throwable t) {
        m_log.log(m_name, m_bundle, serviceReference, LogLevel.AUDIT, message, t);
    }

    @Override
    public void audit(String message) {
        audit(message, (ServiceReference<?>)null, null);
    }

    @Override
    public void audit(String format, Object arg) {
        LogParameters logParameters = getLogParameters(arg);
        audit(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void audit(String format, Object arg1, Object arg2) {
        LogParameters logParameters = getLogParameters(arg1, arg2);
        audit(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    @Override
    public void audit(String format, Object... arguments) {
        LogParameters logParameters = getLogParameters(arguments);
        audit(format(format, logParameters), logParameters.sr, logParameters.t);
    }

    public void log(
        final int level,
        final String message,
        final ServiceReference<?> sr,
        final Throwable exception) {

        m_log.addEntry(new LogEntryImpl(m_name, m_bundle, sr, level, message, exception, Log.getStackTraceElement()));
    }

    LogParameters getLogParameters(Object arg) {
        return getLogParameters0(arg);
    }

    LogParameters getLogParameters(Object arg1, Object arg2) {
        return getLogParameters0(arg1, arg2);
    }

    LogParameters getLogParameters(Object... arguments) {
        return getLogParameters0(arguments);
    }

    LogParameters getLogParameters0(Object... arguments) {
        if (arguments == null || arguments.length == 0) {
            return new LogParameters(null, null, null);
        }
        ServiceReference<?> sr = null;
        Throwable t = null;
        List<Object> args = new ArrayList<>();
        for (Object arg : arguments) {
            if (t == null && arg instanceof Throwable) {
                t = (Throwable)arg;
            }
            else if (sr == null && arg instanceof ServiceReference) {
                sr = (ServiceReference<?>)arg;
            }
            else if (arg != null) {
                args.add(arg);
            }
        }
        return new LogParameters(args.toArray(), sr, t);
    }

    String format(String format, LogParameters logParameters) {
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        int length = format.length();
        String previous = MAX_CHAR;
        boolean escape = false;
        int argIndex = 0;
        while (offset < length) {
            int curChar = format.codePointAt(offset);
            offset += Character.charCount(curChar);
            String cur = new String(Character.toChars(curChar));

            if (argIndex == logParameters.args.length) {
                sb.append(cur);
            }
            else if (escape) {
                escape = false;
                sb.append(cur);
                previous = MAX_CHAR;
            }
            else if (ESCAPE.equals(cur)) {
                escape = true;
                previous = ESCAPE;
            }
            else if (BRACE_OPEN.equals(cur)) {
                if (BRACE_OPEN.equals(previous)) {
                    sb.append(previous);
                }
                previous = BRACE_OPEN;
            }
            else if (BRACE_CLOSE.equals(cur) && BRACE_OPEN.equals(previous)) {
                sb.append(logParameters.args[argIndex++]);
                previous = MAX_CHAR;
            }
            else {
                sb.append(cur);
                previous = MAX_CHAR;
            }
        }

        return sb.toString();
    }

    static class LogParameters {
        public LogParameters(Object[] args, ServiceReference<?> sr, Throwable t) {
            this.args = args;
            this.sr = sr;
            this.t = t;
        }
        final Object[] args;
        final ServiceReference<?> sr;
        final Throwable t;
    }

}
