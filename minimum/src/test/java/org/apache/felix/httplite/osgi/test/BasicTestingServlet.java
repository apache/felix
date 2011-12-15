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
package org.apache.felix.httplite.osgi.test;


import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet for tests.
 *
 */
public class BasicTestingServlet extends HttpServlet implements Servlet
{

    boolean m_getCalled, m_postCalled, m_putCalled, m_deleteCalled = false;
    private final String m_responseStringContent;
    private final boolean m_asWriter;
    private final byte[] m_responseBinaryContent;
    private Map m_requestParams;
    private Map m_queryStringMap;
    private String m_pathInfo;
    private HttpServletRequest m_request;


    /**
     * Most basic constructor.
     */
    public BasicTestingServlet()
    {
        this.m_responseStringContent = null;
        this.m_responseBinaryContent = null;
        this.m_asWriter = false;
    }


    /**
     * Pass back content in GET.
     * @param content
     * @param asWriter
     */
    public BasicTestingServlet( String content, boolean asWriter )
    {
        this.m_responseStringContent = content;
        this.m_responseBinaryContent = null;
        this.m_asWriter = asWriter;
    }


    public BasicTestingServlet( byte[] content, boolean asWriter )
    {
        this.m_responseStringContent = null;
        this.m_responseBinaryContent = content;
        this.m_asWriter = asWriter;
    }


    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        m_getCalled = true;
        m_requestParams = req.getParameterMap();
        m_queryStringMap = parseQueryStringMap( req.getQueryString() );
        m_pathInfo = req.getPathInfo();
        m_request = req;

        if ( m_responseStringContent != null )
        {
            if ( m_asWriter )
            {
                resp.getWriter().print( m_responseStringContent );
            }
            else
            {
                resp.getOutputStream().print( m_responseStringContent );
            }
        }
        else if ( m_responseBinaryContent != null )
        {
           
            resp.getOutputStream().write( m_responseBinaryContent );

        }
    }


    private Map parseQueryStringMap( String queryString )
    {
        if ( queryString == null || queryString.length() == 0 )
        {
            return Collections.EMPTY_MAP;
        }

        Map m = new HashMap();

        String[] kvp = queryString.split( "&" );

        for ( int i = 0; i < kvp.length; ++i )
        {
            String elem[] = kvp[i].split( "=" );
            m.put( elem[0], elem[1] );
        }

        return m;
    }


    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        m_postCalled = true;
        m_requestParams = req.getParameterMap();
        m_queryStringMap = parseQueryStringMap( req.getQueryString() );
        m_pathInfo = req.getPathInfo();
        m_request = req;
    }


    protected void doDelete( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        m_deleteCalled = true;
        m_requestParams = req.getParameterMap();
        m_queryStringMap = parseQueryStringMap( req.getQueryString() );
        m_pathInfo = req.getPathInfo();
        m_request = req;
    }


    protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        m_putCalled = true;
        m_requestParams = req.getParameterMap();
        m_queryStringMap = parseQueryStringMap( req.getQueryString() );
        m_pathInfo = req.getPathInfo();
        m_request = req;
    }


    public boolean isGetCalled()
    {
        return m_getCalled;
    }


    public boolean isPostCalled()
    {
        return m_postCalled;
    }


    public boolean isPutCalled()
    {
        return m_putCalled;
    }


    public boolean isDeleteCalled()
    {
        return m_deleteCalled;
    }


    public Map getRequestParameters()
    {
        return m_requestParams;
    }


    public Map getQueryStringMap()
    {
        return m_queryStringMap;
    }


    public String getPathInfo()
    {
        return m_pathInfo;
    }

    public Enumeration getHeaderNames()
    {
        return m_request.getHeaderNames();
    }

    public String getHeader(String name) 
    {
        return m_request.getHeader( name );
    }
    
    public Enumeration getHeaders(String name) 
    {
        return m_request.getHeaders( name );
    }
}
