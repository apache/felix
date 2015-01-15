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
package org.apache.felix.http.jetty.internal;

import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.URLResource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

class WebAppBundleContext extends WebAppContext
{
    public WebAppBundleContext(String contextPath, final Bundle bundle, final ClassLoader parent)
    {
        super(null, contextPath.substring(1), contextPath);

        this.setBaseResource(new BundleURLResource(bundle.getEntry("/")));
        this.setClassLoader(new ClassLoader(parent)
        {
            @Override
            protected Class<?> findClass(String s) throws ClassNotFoundException
            {
                // Don't try to load classes from the bundle when it is not active
                if (bundle.getState() == Bundle.ACTIVE)
                {
                    try
                    {
                        return bundle.loadClass(s);
                    }
                    catch (ClassNotFoundException e)
                    {
                    }
                }
                return super.findClass(s);
            }

            @Override
            protected URL findResource(String name)
            {
                // Don't try to load resources from the bundle when it is not active
                if (bundle.getState() == Bundle.ACTIVE)
                {
                    URL url = bundle.getResource(name);
                    if (url != null)
                    {
                        return url;
                    }
                }
                return super.findResource(name);
            }

            @Override
            @SuppressWarnings({ "unchecked" })
            protected Enumeration<URL> findResources(String name) throws IOException
            {
                // Don't try to load resources from the bundle when it is not active
                if (bundle.getState() == Bundle.ACTIVE)
                {
                    Enumeration<URL> urls = (Enumeration<URL>) bundle.getResources(name);
                    if (urls != null)
                    {
                        return urls;
                    }
                }
                return super.findResources(name);
            }
        });
        this.setThrowUnavailableOnStartupException(true);
    }

    @Override
    public Resource newResource(URL url) throws IOException
    {
        if (url == null)
        {
            return null;
        }
        return new BundleURLResource(url);
    }

    static class BundleURLResource extends URLResource
    {
        BundleURLResource(URL url)
        {
            super(url, null);
        }

        @Override
        public synchronized void close()
        {
            if (this._in != null)
            {
                // Do not close this input stream: it would invalidate
                // the associated zipfile's inflater and every future access
                // to some bundle entry leads to an NPE with message
                // "Inflater has been closed"
                this._in = null;
            }
            super.close();
        }

        @Override
        public Resource addPath(String path) throws MalformedURLException
        {
            if (path == null)
            {
                return null;
            }
            path = URIUtil.canonicalPath(path);

            URL url = new URL(URIUtil.addPaths(this._url.toExternalForm(), path));
            return new BundleURLResource(url);
        }

        @Override
        public File getFile() throws IOException
        {
            // not available as a file
            return null;
        }
    }
}
