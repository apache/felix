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
package org.apache.felix.dm.context;

/**
 * DependencyManager Dependencies may log messages using this interface, which can be obtained from the ComponentContext 
 * interface.
 */
public interface Log {
    /**
     * Logs a message using LogService.LOG_ERROR log level
     * @param format the message format
     * @param params the message parameters
     */
    public void err(String format, Object ... params);
    
    /**
     * Logs a message using LogService.LOG_ERROR log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void err(String format, Throwable err, Object ... params);

    /**
     * Logs a message using LogService.LOG_WARNING log level
     * @param format the message format
     * @param params the message parameters
     */
    public void warn(String format, Object ... params);
 
    /**
     * Logs a message using LogService.LOG_WARNING log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void warn(String format, Throwable err, Object ... params);

    /**
     * Is the LogService.LOG_INFO level enabled ?
     */
    public boolean info();
    
    /**
     * Logs a message using LogService.LOG_INFO log level
     * @param format the message format
     * @param params the message parameters
     */
    public void info(String format, Object ... params);

    /**
     * Logs a message using LogService.LOG_INFO log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void info(String format, Throwable err, Object ... params);

    /**
     * Is the LogService.LOG_DEBUG level enabled ?
     */
    public boolean debug();    

    /**
     * Logs a message using LogService.LOG_DEBUG log level
     * @param format the message format
     * @param params the message parameters
     */
    public void debug(String format, Object ... params);

    /**
     * Logs a message using LogService.LOG_DEBUG log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void debug(String format, Throwable err, Object ... params);
}
