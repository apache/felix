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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.log.LogLevel;
import org.osgi.service.log.admin.LoggerContext;

public class LoggerContextImpl implements LoggerContext {

    static final String ROOT = "ROOT";

    private final String _name;
    protected volatile Map<String, LogLevel> _levels;
    protected final Lock _lock = new ReentrantLock();
    protected final LoggerAdminImpl _loggerAdminImpl;
    protected final LoggerContext _rootContext;

    private volatile String _toString;

    public LoggerContextImpl(String name, LoggerAdminImpl loggerAdminImpl, LoggerContext rootLoggerContext) {
        _name = name;
        _loggerAdminImpl = loggerAdminImpl;
        _rootContext = rootLoggerContext;
    }

    public String getName() {
        return _name;
    }

    public LogLevel getEffectiveLogLevel(String name) {
        _lock.lock();
        try {
            if (_levels != null && !_levels.isEmpty()) {
                String copy = name;
                LogLevel level;
                while (copy.length() > 0) {
                    level = _levels.get(copy);
                    if (level != null) {
                        return level;
                    }
                    if (ROOT.equals(copy))
                        break;
                    copy = ancestor(copy);
                }
            }
            return _rootContext.getEffectiveLogLevel(name);
        }
        finally {
            _lock.unlock();
        }
    }

    public Map<String, LogLevel> getLogLevels() {
        _lock.lock();
        try {
            if (_levels == null) {
                return new HashMap<>();
            }
            return new HashMap<>(_levels);
        }
        finally {
            _lock.unlock();
        }
    }

    public void setLogLevels(Map<String, LogLevel> logLevels) {
        _lock.lock();
        try {
            _levels = new HashMap<>(logLevels);
            _loggerAdminImpl.keepLoggerContext(_name, this);
        }
        finally {
            _lock.unlock();
        }
    }

    public void clear() {
        _lock.lock();
        try {
            _levels = null;
        }
        finally {
            _lock.unlock();
        }
    }

    public boolean isEmpty() {
        _lock.lock();
        try {
            return _levels == null || _levels.isEmpty();
        }
        finally {
            _lock.unlock();
        }
    }

    void updateLoggerContext(Dictionary<String, Object> properties) {
        _lock.lock();
        try {
            _levels = new HashMap<>();
            if (properties != null) {
                for (Enumeration<String> enu = properties.keys(); enu.hasMoreElements();) {
                    String key = enu.nextElement();
                    Object object = properties.get(key);
                    if (object instanceof String) {
                        String value = (String)object;
                        for (LogLevel level : LogLevel.values()) {
                            if (level.name().equalsIgnoreCase(value)) {
                                _levels.put(key, level);
                                break;
                            }
                        }
                    }
                }
            }
            _loggerAdminImpl.keepLoggerContext(_name, this);
        }
        finally {
            _lock.unlock();
        }
    }

    @Override
    public String toString() {
        if (_toString == null) {
            _toString = getClass().getSimpleName() + "[" + _name + "]";
        }
        return _toString;
    }

    protected String ancestor(String name) {
        int position = name.lastIndexOf('.');
        if (position == -1) {
            return ROOT;
        }
        return name.substring(0, position);
    }

}
