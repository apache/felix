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

import java.util.Dictionary;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Bundle;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

public class LoggerAdminImpl implements LoggerAdmin {

    private final Log m_log;
    private final LoggerContext m_rootContext;
    private final ConcurrentMap<String, LoggerContext> m_contexts = new ConcurrentHashMap<>();
    private final ConcurrentMap<LoggerKey, Logger> _loggers = new ConcurrentHashMap<>();

    public LoggerAdminImpl(final String defaultLogLevelString, final Log log) {
        m_rootContext = new RootLoggerContextImpl(defaultLogLevelString, this);
        m_log = log;
    }

    @Override
    public LoggerContext getLoggerContext(String name) {
        return getOrCreateLoggerContext(name);
    }

    public Set<String> getLoggerContextNames() {
        return m_contexts.keySet();
    }

    protected void updateConfiguration(String name, Dictionary<String, Object> properties) {
        LoggerContext loggerContext = getOrCreateLoggerContext(name);

        LoggerContextImpl contextImpl = (LoggerContextImpl)loggerContext;

        contextImpl.updateLoggerContext(properties);
    }

    protected LoggerContext getOrCreateLoggerContext(String name) {
        if (name == null) {
            return m_rootContext;
        }

        LoggerContext loggerContext = m_contexts.get(name);

        if (loggerContext == null) {
            loggerContext = new LoggerContextImpl(name, this, m_rootContext);
        }

        return loggerContext;
    }

    public void keepLoggerContext(String name, LoggerContextImpl loggerContext) {
        if (loggerContext instanceof RootLoggerContextImpl) return;
        m_contexts.put(name, loggerContext);
    }

    @SuppressWarnings("unchecked")
    public <L extends Logger> L getLogger(
        final Bundle bundle, final String name, final Class<L> loggerType) {

        LoggerKey key = new LoggerKey(bundle, name, loggerType);

        L logger = (L)_loggers.get(key);

        if (logger == null) {
            if (loggerType.equals(FormatterLogger.class)) {
                logger = (L)new FormatterLoggerImpl(name, bundle, m_log, this);
            }
            else {
                logger = (L)new LoggerImpl(name, bundle, m_log, this);
            }

            L previous = (L)_loggers.putIfAbsent(key, logger);

            if (previous != null) {
                logger = previous;
            }
        }

        return logger;
    }

    LoggerContext getLoggerContext(Bundle bundle, String name) {
        String loggerContextName = String.format(
            "%s|%s|%s", bundle.getSymbolicName(), bundle.getVersion(), bundle.getLocation());

        LoggerContext loggerContext = getLoggerContext(loggerContextName);

        if (loggerContext.isEmpty()) {
            loggerContextName = String.format(
                "%s|%s", bundle.getSymbolicName(), bundle.getVersion());

            loggerContext = getLoggerContext(loggerContextName);
        }

        if (loggerContext.isEmpty()) {
            loggerContext = getLoggerContext(bundle.getSymbolicName());
        }

        return loggerContext;
    }

    static class LoggerKey {

        public LoggerKey(Bundle bundle, String name, Class<? extends Logger> loggerType) {
            this.m_bundle = bundle;
            this.m_name = name;
            this.m_loggerType = loggerType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_bundle == null) ? 0 : m_bundle.hashCode());
            result = prime * result + ((m_loggerType == null) ? 0 : m_loggerType.hashCode());
            result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LoggerKey other = (LoggerKey) obj;
            if (m_bundle == null) {
                if (other.m_bundle != null)
                    return false;
            } else if (m_bundle.getBundleId() != other.m_bundle.getBundleId())
                return false;
            if (m_loggerType == null) {
                if (other.m_loggerType != null)
                    return false;
            } else if (!m_loggerType.equals(other.m_loggerType))
                return false;
            if (m_name == null) {
                if (other.m_name != null)
                    return false;
            } else if (!m_name.equals(other.m_name))
                return false;
            return true;
        }

        @Override
        public String toString() {
            if (m_string == null) {
                m_string = getClass().getSimpleName() + "[" + m_bundle + "#" + m_name + "!" + m_loggerType.getSimpleName() + "]";
            }
            return m_string;
        }

        private final Bundle m_bundle;
        private final String m_name;
        private final Class<? extends Logger> m_loggerType;
        private volatile String m_string;

    }

}
