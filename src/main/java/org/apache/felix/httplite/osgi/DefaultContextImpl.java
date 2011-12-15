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
package org.apache.felix.httplite.osgi;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * This class was adapted from the Jetty HTTP Service: http://felix.apache.org/site/apache-felix-http-service.html.
 * 
 * Implementation of default HttpContext as per OSGi specification.
 *
 * Notes
 *
 *      - no current inclusion/support for permissions
 *      - security allows all request. Spec leaves security handling to be
 *        implementation specific, but does outline some suggested handling.
 *        Deeper than my understanding of HTTP at this stage, so left for now.
 */
public class DefaultContextImpl implements HttpContext
{
    /**
     * Reference to bundle that registered with the Http Service.
     */
    private Bundle m_bundle;

    /**
     * @param bundle bundle that registered with the Http Service.
     */
    public DefaultContextImpl(final Bundle bundle)
    {
        m_bundle = bundle;
    }

    /* (non-Javadoc)
     * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
     */
    public String getMimeType(final String name)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
     */
    public URL getResource(final String name)
    {
        //TODO: temp measure for name. Bundle classloading doesn't seem to find
        // resources which have a leading "/". This code should be removed
        // if the bundle classloader is changed to allow a leading "/"
        String resource = name;

        if (name.startsWith("/"))
        {
            resource = name.substring(1);
        }

        URL url = m_bundle.getResource(resource);

        if (url == null)
        {
            url = this.getClass().getResource(resource);
        }

        return url;
    }

    /* (non-Javadoc)
     * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean handleSecurity(final HttpServletRequest request,
        final HttpServletResponse response)
    {
        //By default all security is "handled".
        return true;
    }
}