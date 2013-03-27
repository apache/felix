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

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Processor;

/**
 * Clas used to log messages into the bnd logger.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BndLogger extends Logger
{
    private Processor m_processor = new Processor();

    private final int m_level;

    public BndLogger(String level)
    {
        m_level = parseLogLevel(level);
    }
    
    @Override
    public void error(String msg, Object ... args)
    {
        m_processor.error(msg, args);
    }
    
    @Override
    public void error(String msg, Throwable err, Object ... args)
    {
        m_processor.error(msg, err, args);
    }

    @Override
    public void warn(String msg , Object ... args)
    {
        if (m_level >= WARN)
        {
            m_processor.warning(msg, args);
        }
    }
    
    @Override
    public void info(String msg, Object ... args)
    {
        if (m_level >= INFO)
        {
            m_processor.warning(msg, args);
        }
    }

    @Override
    public void debug(String msg, Object ... args)
    {
        if (m_level >= DEBUG)
        {
            m_processor.warning(msg, args);
        }
    }
    
    public void getLogs(Analyzer to)
    {
        to.getInfo(m_processor, "DependencyManager: ");
    }
}
