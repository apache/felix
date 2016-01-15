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
package org.apache.felix.deployment.rp.autoconf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Dictionary;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class AutoConfResource implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String m_pid;
    private final String m_factoryPid;
    private final Dictionary m_properties;
    private final String m_bundleLoc;
    private final boolean m_merge;
    private final String m_name;

    private transient Filter m_filter;
    private String m_alias = null;

    public AutoConfResource(String name, String pid, String factoryPid, String bundleLocation, boolean merge, Dictionary properties, Filter filter)
    {
        m_name = name;
        m_pid = pid;
        m_filter = filter;
        m_factoryPid = (factoryPid == null) ? "" : factoryPid;
        m_bundleLoc = bundleLocation;
        m_merge = merge;
        m_properties = properties;
    }

    public String getName()
    {
        return m_name;
    }

    public String getPid()
    {
        return m_pid;
    }

    public Filter getFilter()
    {
        return m_filter;
    }

    /**
     * Returns empty string if this configuration is not a factory configuration, otherwise the factory
     * PID is returned.
     * 
     * @return Empty string if this is not a factory configuration resource, else the factory PID is returned.
     */
    public String getFactoryPid()
    {
        return m_factoryPid;
    }

    public Dictionary getProperties()
    {
        return m_properties;
    }

    public String getBundleLocation()
    {
        return m_bundleLoc;
    }

    public boolean isMerge()
    {
        return m_merge;
    }

    public boolean isFactoryConfig()
    {
        return !(m_factoryPid == null || "".equals(m_factoryPid));
    }

    public void setGeneratedPid(String alias)
    {
        m_alias = alias;
    }

    public String getGeneratedPid()
    {
        if (m_alias == null)
        {
            throw new IllegalStateException("Must set an alias first.");
        }
        return m_alias;
    }

    /**
     * Determine if the specified <code>AutoConfResource</code> is meant to be used for the same <code>Configuration</code> as this object.
     *  
     * @param resource The <code>AutoConfResource</code> to compare with.
     * @return Returns <code>true</code> if the two resources are meant to be used for the same <code>Configuration</code> object, false otherwise.
     */
    public boolean equalsTargetConfiguration(AutoConfResource resource)
    {
        if (isFactoryConfig())
        {
            return m_pid.equals(resource.getPid()) && m_factoryPid.equals(resource.getFactoryPid());
        }
        else
        {
            return m_pid.equals(resource.getPid());
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        if (m_filter != null)
        {
            out.writeUTF(m_filter.toString());
        }
        else
        {
            out.writeUTF("");
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        String filter = in.readUTF();
        if (!"".equals(filter))
        {
            try
            {
                m_filter = FrameworkUtil.createFilter(filter);
            }
            catch (InvalidSyntaxException e)
            {
                throw new IOException("Unable to parse serialized filter: " + e.getMessage());
            }
        }
    }
}
