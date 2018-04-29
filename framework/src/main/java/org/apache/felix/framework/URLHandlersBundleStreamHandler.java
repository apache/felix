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
package org.apache.felix.framework;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.*;

import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;

class URLHandlersBundleStreamHandler extends URLStreamHandler
{
    private final Felix m_framework;
    private final SecureAction m_action;

    public URLHandlersBundleStreamHandler(Felix framework)
    {
        m_framework = framework;
        m_action = null;
    }

    public URLHandlersBundleStreamHandler(SecureAction action)
    {
        m_framework = null;
        m_action = action;
    }

    protected URLConnection openConnection(URL url) throws IOException
    {
        if (!"felix".equals(url.getAuthority()))
        {
            checkPermission(url);
        }
        if (m_framework != null)
        {
            return new URLHandlersBundleURLConnection(url, m_framework);
        }

        Object framework = URLHandlers.getFrameworkFromContext();

        if (framework != null)
        {
            if (framework instanceof Felix)
            {
                return new URLHandlersBundleURLConnection(url, (Felix) framework);
            }
            try
            {
                Class targetClass = framework.getClass().getClassLoader().loadClass(
                    URLHandlersBundleURLConnection.class.getName());
<<<<<<< HEAD
                
                Constructor constructor = m_action.getConstructor(targetClass, 
=======

                Constructor constructor = m_action.getConstructor(targetClass,
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        new Class[]{URL.class, framework.getClass().getClassLoader().loadClass(
                                Felix.class.getName())});
                m_action.setAccesssible(constructor);
                return (URLConnection) m_action.invoke(constructor, new Object[]{url, framework});
            }
            catch (Exception ex)
            {
                throw new IOException(ex.getMessage());
            }
        }
        throw new IOException("No framework context found");
    }

<<<<<<< HEAD
    protected void parseURL(URL u, String spec, int start, int limit) 
=======
    protected void parseURL(URL u, String spec, int start, int limit)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        super.parseURL(u, spec, start, limit);

        if (checkPermission(u))
        {
            super.setURL(u, u.getProtocol(), u.getHost(), u.getPort(), "felix", u.getUserInfo(), u.getPath(), u.getQuery(), u.getRef());
        }
    }

<<<<<<< HEAD
    protected String toExternalForm(URL u) 
    {
        StringBuffer result = new StringBuffer();
=======
    protected String toExternalForm(URL u)
    {
        StringBuilder result = new StringBuilder();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        result.append(u.getProtocol());
        result.append("://");
        result.append(u.getHost());
        result.append(':');
        result.append(u.getPort());
<<<<<<< HEAD
        if (u.getPath() != null) 
        {
            result.append(u.getPath());
        }
        if (u.getQuery() != null) 
=======
        if (u.getPath() != null)
        {
            result.append(u.getPath());
        }
        if (u.getQuery() != null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            result.append('?');
            result.append(u.getQuery());
        }
<<<<<<< HEAD
        if (u.getRef() != null) 
=======
        if (u.getRef() != null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            result.append("#");
            result.append(u.getRef());
        }
        return result.toString();
    }
<<<<<<< HEAD
    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    protected java.net.InetAddress getHostAddress(URL u)
    {
        return null;
    }

    private boolean checkPermission(URL u)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            Object framework = m_framework;
            if (framework == null)
            {
                framework = URLHandlers.getFrameworkFromContext();
                if (!(framework instanceof Felix))
                {
                    return false;
                }
            }
            Felix felix = (Felix) framework;
            long bundleId = Util.getBundleIdFromRevisionId(u.getHost());
            Bundle bundle = felix.getBundle(bundleId);
            if (bundle != null)
            {
                sm.checkPermission(new AdminPermission(bundle, AdminPermission.RESOURCE));
                return true;
            }
        }
        else
        {
            return true;
        }
        return false;
    }
}
