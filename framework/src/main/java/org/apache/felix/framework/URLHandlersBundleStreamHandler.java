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
import java.lang.reflect.Method;
import java.net.*;
import java.security.Permission;

import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;

class URLHandlersBundleStreamHandler extends URLStreamHandler
{
    private final Object m_framework;
    private final SecureAction m_action;

    public URLHandlersBundleStreamHandler(Object framework, SecureAction action)
    {
        m_framework = framework;
        m_action = action;
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
        Object framework = m_framework;

        if (framework == null)
        {
            framework = URLHandlers.getFrameworkFromContext(Util.getFrameworkUUIDFromURL(url.getHost()));
        }

        if (framework != null)
        {
            if (framework instanceof Felix)
            {
                return new URLHandlersBundleURLConnection(url, (Felix) framework);
            }
            try
            {
                ClassLoader loader = m_action.getClassLoader(framework.getClass());

                Class targetClass = loader.loadClass(
                    URLHandlersBundleURLConnection.class.getName());

                Constructor constructor = m_action.getConstructor(targetClass,
                        new Class[]{URL.class, loader.loadClass(
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

    protected void parseURL(URL u, String spec, int start, int limit)
    {
        super.parseURL(u, spec, start, limit);

        if (checkPermission(u))
        {
            super.setURL(u, u.getProtocol(), u.getHost(), u.getPort(), "felix", u.getUserInfo(), u.getPath(), u.getQuery(), u.getRef());
        }
    }

    protected String toExternalForm(URL u)
    {
        StringBuilder result = new StringBuilder();
        result.append(u.getProtocol());
        result.append("://");
        result.append(u.getHost());
        result.append(':');
        result.append(u.getPort());
        if (u.getPath() != null)
        {
            result.append(u.getPath());
        }
        if (u.getQuery() != null)
        {
            result.append('?');
            result.append(u.getQuery());
        }
        if (u.getRef() != null)
        {
            result.append("#");
            result.append(u.getRef());
        }
        return result.toString();
    }

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
                framework = URLHandlers.getFrameworkFromContext(Util.getFrameworkUUIDFromURL(u.getHost()));
            }
            try {
                long bundleId = Util.getBundleIdFromRevisionId(Util.getRevisionIdFromURL(u.getHost()));

                if (framework instanceof Felix)
                {
                    Bundle bundle = ((Felix) framework).getBundle(bundleId);
                    if (bundle != null)
                    {
                        sm.checkPermission(new AdminPermission(bundle, AdminPermission.RESOURCE));
                        return true;
                    }
                }
                else if (framework != null)
                {
                    Method method = m_action.getDeclaredMethod(framework.getClass(), "getBundle", new Class[]{long.class});
                    m_action.setAccesssible(method);
                    Object bundle = method.invoke(framework, bundleId);
                    if (bundle != null)
                    {
                        ClassLoader loader = m_action.getClassLoader(framework.getClass());

                        sm.checkPermission((Permission) m_action.getConstructor(
                            loader.loadClass(AdminPermission.class.getName()),
                            new Class[] {loader.loadClass(Bundle.class.getName()), String.class}).newInstance(bundle, AdminPermission.RESOURCE));
                        return true;
                    }
                }
                else
                {
                    throw new IOException("No framework context found");
                }
            }
            catch (SecurityException ex)
            {
                throw ex;
            }
            catch (Exception ex)
            {
                throw new SecurityException(ex);
            }
        }
        else
        {
            return true;
        }
        return false;
    }
}
