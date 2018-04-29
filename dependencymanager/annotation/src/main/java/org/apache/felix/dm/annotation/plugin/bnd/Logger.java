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

/**
 * Base class for our logger. Under Maven, we log into the Maven logger. Under bnd, we log into the Bnd logger.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class Logger
{    
    protected final static int ERROR = 1;
    protected final static int WARN = 2;
    protected final static int INFO = 3;
    protected final static int DEBUG = 4;
    
    protected int parseLogLevel(String level)
    {
        if (level == null || level.regionMatches(true, 0, "err", 0, "err".length())) 
        {
            return ERROR;
        }
        else if (level.regionMatches(true, 0, "warn", 0, "warn".length()))
        {
            return WARN;
        }
        else if (level.regionMatches(true, 0, "info", 0, "info".length()))
        {
            return INFO;
        }
        else if (level.regionMatches(true, 0, "debug", 0, "debug".length()))
        {
            return DEBUG;
        }
        else
        {
            throw new IllegalArgumentException("Invalid log level value: " + level + " (valid values are \"error\", \"warn\", \"debug\")");
        }
    }
    
    public abstract void error(String msg, Object ... args);
    public abstract void error(String msg, Throwable err, Object ... args);
    public abstract void warn(String msg , Object ... args);
    public abstract void info(String msg , Object ... args);
    public abstract void debug(String msg, Object ... args);    
}
