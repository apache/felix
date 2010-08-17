/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.shell.remote;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements a mediator pattern class for services from the OSGi container.
 */
class ServiceMediator
{
    private final String m_bundleName;
    private final long m_bundleId;
    private final BundleContext m_context;
    private final ServiceTracker m_logTracker;
    private final ServiceTracker m_shellTracker;
    private final ServiceTracker m_cpTracker;

    ServiceMediator(BundleContext context)
    {
        m_context = context;
        m_bundleName = (m_context.getBundle().getSymbolicName() == null)
            ? m_context.getBundle().getLocation()
            : m_context.getBundle().getSymbolicName();
        m_bundleId = m_context.getBundle().getBundleId();
        m_logTracker = new ServiceTracker(
            m_context, "org.osgi.service.log.LogService", null);
        m_logTracker.open();
        m_shellTracker = new ServiceTracker(
            m_context, "org.apache.felix.shell.ShellService", null);
        m_shellTracker.open();
        m_cpTracker = new ServiceTracker(
            m_context, "org.apache.felix.service.command.CommandProcessor", null);
        m_cpTracker.open();
    }

    public Object getCommandProcessor(long wait)
    {
        Object svcObj = null;
        try
        {
            if (wait < 0)
            {
                svcObj = m_cpTracker.getService();
            }
            else
            {
                svcObj = m_cpTracker.waitForService(wait);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(System.err);
        }
        return svcObj;
    }

    /**
     * Returns a reference to the <tt>ShellService</tt> (Felix).
     *
     * @param wait time in milliseconds to wait for the reference if it isn't available.
     * @return the reference to the <tt>ShellService</tt> as obtained from the OSGi service layer.
     */
    public Object getShellService(long wait)
    {
        Object svcObj = null;
        try
        {
            if (wait < 0)
            {
                svcObj = m_shellTracker.getService();
            }
            else
            {
                svcObj = m_shellTracker.waitForService(wait);
            }

            return svcObj;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(System.err);
        }
        return svcObj;
    }//getFelixShellService

    public Object getLogService(long wait)
    {
        Object svcObj = null;
        try
        {
            if (wait < 0)
            {
                svcObj = m_logTracker.getService();
            }
            else
            {
                svcObj = m_logTracker.waitForService(wait);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(System.err);
        }
        return svcObj;
    }//getLogService

    public void info(String msg)
    {
        Object log = getLogService(NO_WAIT);
        if (log != null)
        {
            ((LogService) log).log(LogService.LOG_INFO, msg);
        }
        else
        {
            sysout(msg);
        }
    }//info

    public void error(String msg, Throwable t)
    {
        Object log = getLogService(NO_WAIT);
        if (log != null)
        {
            ((LogService) log).log(LogService.LOG_ERROR, msg);
        }
        else
        {
            syserr(msg, t);
        }
    }//error

    public void error(String msg)
    {
        Object log = getLogService(NO_WAIT);
        if (log != null)
        {
            ((LogService) log).log(LogService.LOG_ERROR, msg);
        }
        else
        {
            syserr(msg, null);
        }
    }//error

    public void debug(String msg)
    {
        Object log = getLogService(NO_WAIT);
        if (log != null)
        {
            ((LogService) log).log(LogService.LOG_DEBUG, msg);
        }
        else
        {
            sysout(msg);
        }
    }//debug

    public void warn(String msg)
    {
        Object log = getLogService(NO_WAIT);
        if (log != null)
        {
            ((LogService) log).log(LogService.LOG_WARNING, msg);
        }
        else
        {
            syserr(msg, null);
        }
    }//warn

    private void sysout(String msg)
    {
        //Assemble String
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(m_bundleName);
        sbuf.append(" [");
        sbuf.append(m_bundleId);
        sbuf.append("] ");
        sbuf.append(msg);
        System.out.println(sbuf.toString());
    }//sysout

    private void syserr(String msg, Throwable t)
    {
        //Assemble String
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(m_bundleName);
        sbuf.append(" [");
        sbuf.append(m_bundleId);
        sbuf.append("] ");
        sbuf.append(msg);
        System.err.println(sbuf.toString());
        if (t != null)
        {
            t.printStackTrace(System.err);
        }
    }//logToSystem

    /**
     * Deactivates this mediator, nulling out all references.
     * If called when the bundle is stopped, the framework should actually take
     * care of unregistering the <tt>ServiceListener</tt>.
     */
    public void deactivate()
    {
        if (m_logTracker != null)
        {
            m_logTracker.close();
        }
        m_shellTracker.close();
        m_cpTracker.close();
    }//deactivate

    public static long WAIT_UNLIMITED = 0;
    public static long NO_WAIT = -1;
}//class ServiceMediator