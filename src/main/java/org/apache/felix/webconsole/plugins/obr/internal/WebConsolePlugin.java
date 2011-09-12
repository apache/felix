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
package org.apache.felix.webconsole.plugins.obr.internal;


import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;


/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
class WebConsolePlugin extends SimpleWebConsolePlugin 
{
    private static final String LABEL = "obr"; //$NON-NLS-1$
    private static final String TITLE = "%obr.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = { "/" + LABEL + "/res/plugin.css" }; //$NON-NLS-1$ //$NON-NLS-2$

    // templates
    private final String TEMPLATE;

    private AbstractBundleRepositoryRenderHelper helper;


    /**
     *
     */
    public WebConsolePlugin()
    {
        super( LABEL, TITLE, CSS );

        // load templates
        TEMPLATE = readTemplateFile("/res/plugin.html"); //$NON-NLS-1$
    }


    /**
     * @see org.apache.felix.webconsole.SimpleWebConsolePlugin#deactivate()
     */
    public void deactivate()
    {
        if ( helper != null )
        {
            helper.dispose();
            helper = null;
        }

        super.deactivate();
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", getData( request ) ); //$NON-NLS-1$

        response.getWriter().print( TEMPLATE );
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        if ( !hasRepositoryAdmin() )
        {
            response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "RepositoryAdmin service is missing" );
            return;
        }

        final String action = request.getParameter( "action" ); //$NON-NLS-1$
        final String deploy = request.getParameter( "deploy" ); //$NON-NLS-1$
        final String deploystart = request.getParameter( "deploystart" ); //$NON-NLS-1$
        final String optional = request.getParameter( "optional" ); //$NON-NLS-1$

        if ( action != null )
        {
            doAction( action, request.getParameter( "url" ) ); //$NON-NLS-1$
            response.getWriter().print( getData( request ) );
            return;
        }

        if ( deploy != null || deploystart != null )
        {
            doDeploy( request.getParameterValues( "bundle" ), deploystart != null, optional != null ); //$NON-NLS-1$
            doGet( request, response );
            return;
        }

        super.doPost( request, response );
    }


    AbstractBundleRepositoryRenderHelper getHelper()
    {
        if ( helper == null )
        {
            try
            {
                helper = new FelixBundleRepositoryRenderHelper( this, getBundleContext() );
            }
            catch ( Throwable felixt )
            {
                // ClassNotFoundException, ClassDefNotFoundError

                try
                {
                    helper = new OsgiBundleRepositoryRenderHelper( this, getBundleContext() );
                }
                catch ( Throwable osgit )
                {
                    // ClassNotFoundException, ClassDefNotFoundError
                }
            }
        }

        return helper;
    }


    private boolean hasRepositoryAdmin()
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        return helper != null && helper.hasRepositoryAdmin();
    }


    private String getData( final HttpServletRequest request )
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        if ( helper == null || !helper.hasRepositoryAdmin() )
        {
            return "{}"; //$NON-NLS-1$
        }

        RequestInfo info = new RequestInfo( request );

        final String filter;
        String list = info.getList();
        if ( list != null )
        {
            if ( "-".equals( list ) ) //$NON-NLS-1$
            {
                StringBuffer sb = new StringBuffer( "(!(|" ); //$NON-NLS-1$
                for ( int c = 0; c < 26; c++ )
                {
                    sb.append( "(presentationname=" ).append( ( char ) ( 'a' + c ) ) //$NON-NLS-1$
                      .append( "*)(presentationname=" ).append( ( char ) ( 'A' + c ) ) //$NON-NLS-1$
                      .append( "*)" ); //$NON-NLS-1$
                }
                sb.append( "))" ); //$NON-NLS-1$
                filter = sb.toString();
            }
            else
            {
                filter = "(|(presentationname=" + list.toLowerCase() //$NON-NLS-1$
                    + "*)(presentationname=" + list.toUpperCase() + "*))"; //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        else
        {
            String query = info.getQuery();
            if ( query != null )
            {
                if ( query.indexOf( '=' ) > 0 )
                {
                    if ( query.startsWith( "(" ) ) //$NON-NLS-1$
                    {
                        filter = query;
                    }
                    else
                    {
                        filter = "(" + query + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                else
                {
                    filter = "(|(presentationame=*" + query + "*)(symbolicname=*" + query + "*))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
            }
            else
            {
                StringBuffer sb = new StringBuffer( "(&" ); //$NON-NLS-1$
                for ( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
                {
                    String k = ( String ) e.nextElement();
                    String v = request.getParameter( k );
                    if ( v != null && v.length() > 0 
                        && !"details".equals( k )  //$NON-NLS-1$
                        && !"deploy".equals( k ) //$NON-NLS-1$
                        && !"deploystart".equals( k )  //$NON-NLS-1$
                        && !"bundle".equals( k )  //$NON-NLS-1$
                        && !"optional".equals( k ) ) //$NON-NLS-1$
                    {
                        sb.append( '(' ).append( k ).append( '=' ).append( v ).append( ')' );
                    }
                }
                sb.append( ')' );
                filter = (sb.length() > 3) ? sb.toString() : null;
            }
        }

        return helper.getData( filter, info.showDetails(), getBundleContext().getBundles() );
    }


    private void doAction( String action, String urlParam ) throws IOException, ServletException
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        if ( helper != null )
        {
            helper.doAction( action, urlParam );
        }
    }


    private void doDeploy( String[] bundles, boolean start, boolean optional )
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        if ( helper != null )
        {
            helper.doDeploy( bundles, start, optional );
        }
    }

    private static class RequestInfo {

        private final HttpServletRequest request;

        private boolean details;
        private String query;
        private String list;


        RequestInfo( final HttpServletRequest request )
        {
            this.request = request;
        }


        boolean showDetails()
        {
            getQuery();
            return details;
        }


        String getQuery()
        {
            if ( query == null )
            {
                String query = WebConsoleUtil.urlDecode( request.getParameter( "query" ) ); //$NON-NLS-1$
                boolean details = false;
                if ( query == null && request.getPathInfo().length() > 5 )
                {
                    // cut off "/obr/" prefix (might want to use getTitle ??)
                    String path = request.getPathInfo().substring( 5 );
                    int slash = path.indexOf( '/' );
                    if ( slash < 0 )
                    {
                        // symbolic name only, version ??
                        query = "(symbolicname=" + path + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    else
                    {
                        query = "(&(symbolicname=" + path.substring( 0, slash )  //$NON-NLS-1$
                            + ")(version=" + path.substring( slash + 1 ) + "))"; //$NON-NLS-1$ //$NON-NLS-2$
                        details = true;
                    }
                }

                this.query = query;
                this.details = details;
            }
            return query;
        }


        String getList()
        {
            if ( list == null )
            {
                list = WebConsoleUtil.urlDecode( request.getParameter( "list" ) ); //$NON-NLS-1$
                if ( list == null && !request.getParameterNames().hasMoreElements() && getQuery() == null )
                {
                    list = "a"; //$NON-NLS-1$
                }
            }
            return list;
        }
    }
}
