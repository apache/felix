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
package org.apache.felix.dm.runtime;

import org.osgi.service.log.LogService;

/**
 * This class logs some formattable strings into the OSGi Log Service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Log
{
    /** The wrap log service which is actually used (Injected by Activator) */
    private volatile LogService m_logService;

    /** Log INFO/DEBUG only if logs are enabled, else only log ERROR/WARN messages */
    private volatile boolean m_logEnabled;
    
    /** Our sole instance */
    private static Log m_instance = new Log();
    
    public static Log instance()
    {
        return m_instance;
    }
    
    public void enableLogs(boolean logEnabled) 
    {
        m_logEnabled = logEnabled;
    }

    public void error(String format, Object ... args) 
    {
        m_logService.log(LogService.LOG_ERROR, String.format(format, args));
    }
    
    public void error(String format, Throwable t, Object ... args) 
    {
        m_logService.log(LogService.LOG_ERROR, String.format(format, args), t);
    }
    
    public void warn(String format, Object ... args) 
    {
        m_logService.log(LogService.LOG_WARNING, String.format(format, args));
    }
    
    public void warn(String format, Throwable t, Object ... args) 
    {
        m_logService.log(LogService.LOG_WARNING, String.format(format, args), t);
    }
    
    public void info(String format, Object ... args) 
    {
        if (m_logEnabled)
            m_logService.log(LogService.LOG_INFO, String.format(format, args));
    }
    
    public void info(String format, Throwable t, Object ... args) 
    {
        if (m_logEnabled)
                    m_logService.log(LogService.LOG_INFO, String.format(format, args), t);
    }
    
    public void debug(String format, Object ... args) 
    {
        if (m_logEnabled)
                    m_logService.log(LogService.LOG_DEBUG, String.format(format, args));
    }
    
    public void debug(String format, Throwable t, Object ... args) 
    {
        if (m_logEnabled)
                    m_logService.log(LogService.LOG_DEBUG, String.format(format, args), t);
    }
}
