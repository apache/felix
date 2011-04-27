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
package org.apache.felix.dm.annotation.plugin.mvn;

import org.apache.felix.dm.annotation.plugin.bnd.Logger;
import org.apache.maven.plugin.logging.Log;

/**
 * Maven logger.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MvnLogger extends Logger
{
    private final Log m_log;
    private final int m_level;

    public MvnLogger(Log log, String level)
    {
        m_log = log;
        m_level = parseLogLevel(level);
    }
    
    @Override
    public void error(String msg, Object... args)
    {
        m_log.error("DependencyManager: " + String.format(msg, args));
    }

    @Override
    public void error(String msg, Throwable err, Object... args)
    {
        m_log.error("DependencyManager: " + String.format(msg, args), err);
    }

    @Override
    public void warn(String msg, Object... args)
    {
        m_log.warn("DependencyManager: " + String.format(msg, args));
    }
    
    @Override
    public void info(String msg, Object... args)
    {
        if (m_level >= INFO)
        {
            m_log.info("DependencyManager: " + String.format(msg, args));
        }
    }
    
    @Override
    public void debug(String msg, Object... args)
    {
        if (m_level >= DEBUG)
        {
            m_log.info("DependencyManager: " + String.format(msg, args));
        }
    }
}
