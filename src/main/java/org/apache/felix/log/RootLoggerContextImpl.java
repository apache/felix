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
import java.util.Map;

import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;

public class RootLoggerContextImpl extends LoggerContextImpl {

    private final LogLevel _defaultLevel;

    public RootLoggerContextImpl(String defaultLogLevelString, LoggerAdminImpl loggerAdminImpl) {
        super(null, loggerAdminImpl, null);

        LogLevel defaultLogLevel = LogLevel.WARN;
        if (defaultLogLevelString != null) {
            for (LogLevel level : LogLevel.values()) {
                if (level.name().equalsIgnoreCase(defaultLogLevelString)) {
                    defaultLogLevel = level;
                    break;
                }
            }
        }

        _defaultLevel = defaultLogLevel;
    }

    public LogLevel getEffectiveLogLevel(String name) {
        _lock.lock();
        try {
            if (_levels != null && !_levels.isEmpty()) {
                LogLevel level;
                while (name.length() > 0) {
                    level = _levels.get(name);
                    if (level != null) {
                        return level;
                    }
                    if (ROOT.equals(name))
                        break;
                    name = ancestor(name);
                }
            }
            return getEffectiveRootLogLevel();
        }
        finally {
            _lock.unlock();
        }
    }

    @Override
    public void setLogLevels(Map<String, LogLevel> logLevels) {
        super.setLogLevels(logLevels);
    }

    @Override
    void updateLoggerContext(Dictionary<String, Object> properties) {
        super.updateLoggerContext(properties);
    }

    private LogLevel getEffectiveRootLogLevel() {
        if (_levels == null) return _defaultLevel;
        LogLevel logLevel = _levels.get(Logger.ROOT_LOGGER_NAME);
        return (logLevel == null)? _defaultLevel : logLevel;
    }

}
