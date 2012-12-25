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
package org.apache.felix.webconsole.internal.configuration;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;


/**
 * The <code>ConfigManager</code> class is the Web Console plugin to
 * manage configurations.
 */
public class ConfigManager extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{

    private static final long serialVersionUID = 5021174538498622428L;

    private static final String LABEL = "configMgr"; // was name //$NON-NLS-1$
    private static final String TITLE = "%configMgr.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = { "/res/ui/config.css" }; //$NON-NLS-1$

    static final String PID_FILTER = "pidFilter"; //$NON-NLS-1$
    static final String PID = "pid"; //$NON-NLS-1$
    static final String factoryPID = "factoryPid"; //$NON-NLS-1$

    static final String CONFIGURATION_ADMIN_NAME = "org.osgi.service.cm.ConfigurationAdmin";

    static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService";

    static final String PLACEHOLDER_PID = "[Temporary PID replaced by real PID upon save]";

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    public ConfigManager()
    {
        super(LABEL, TITLE, CATEGORY_OSGI, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/config.html" ); //$NON-NLS-1$
    }

    static final boolean isAllowedPid(final String pid)
    {
        for(int i = 0; i < pid.length(); i++)
        {
            final char c = pid.charAt(i);
            if ( c == '&' || c == '<' || c == '>' || c == '"' || c == '\'' )
            {
                return false;
            }
        }
        return true;
    }


    private static final Locale getLocale( HttpServletRequest request )
    {
        try
        {
            return request.getLocale();
        }
        catch ( Throwable t )
        {
            // expected in standard OSGi Servlet 2.1 environments
            // fallback to using the default locale
            return Locale.getDefault();
        }
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // needed multiple times below
        String pid = request.getParameter( ConfigManager.PID );
        if ( pid == null )
        {
            String info = request.getPathInfo();
            pid = WebConsoleUtil.urlDecode( info.substring( info.lastIndexOf( '/' ) + 1 ) );
        }

        // the filter to select part of the configurations
        String pidFilter = request.getParameter( PID_FILTER );

        final ConfigAdminSupport cas = getConfigurationAdminSupport();

        // ignore this request if the PID and/or configuration admin is missing
        if ( pid == null || pid.length() == 0 || cas == null )
        {
            // should log this here !!
            return;
        }

        // ignore this request, if the PID is invalid
        if ( ! isAllowedPid(pid) )
        {
            response.sendError(500);
            return;
        }
        if ( pidFilter != null && ! isAllowedPid(pidFilter) )
        {
            response.sendError(500);
            return;
        }

        // the configuration to operate on (to be created or "missing")
        Configuration config = null;

        // should actually apply the configuration before redirecting
        if ( request.getParameter( "create" ) != null ) //$NON-NLS-1$
        {
            config = cas.getPlaceholderConfiguration( pid ); // ca.createFactoryConfiguration( pid, null );
            pid = config.getPid();
        }
        else if ( request.getParameter( "apply" ) != null ) //$NON-NLS-1$
        {
            String redirect = cas.applyConfiguration( request, pid );
            if ( redirect != null )
            {
                if (pidFilter != null) {
                    redirect += '?' + PID_FILTER + '=' + pidFilter;
                }

                WebConsoleUtil.sendRedirect(request, response, redirect);
            }
            else
            {
                response.setContentType("text/plain"); //$NON-NLS-1$
                response.getWriter().print("true"); //$NON-NLS-1$
            }

            return;
        }

        if ( config == null )
        {
            config = cas.getConfiguration( pid );
        }

        // check for configuration unbinding
        if ( request.getParameter( "unbind" ) != null ) //$NON-NLS-1$
        {
            if ( config != null && config.getBundleLocation() != null )
            {
                config.setBundleLocation( null );

                // workaround for Felix Config Admin 1.2.8 not clearing dynamic
                // bundle location when clearing static bundle location. In
                // this case we first set the static bundle location to the
                // dynamic bundle location and then try to set both to null
                if ( config.getBundleLocation() != null )
                {
                    config.setBundleLocation( "??invalid:bundle/location" ); //$NON-NLS-1$
                    config.setBundleLocation( null );
                }
            }
            response.setContentType( "application/json" ); //$NON-NLS-1$
            response.setCharacterEncoding( "UTF-8" ); //$NON-NLS-1$
            response.getWriter().print( "{ \"status\": true }" ); //$NON-NLS-1$
            return;
        }

        // send the result
        response.setContentType( "application/json" ); //$NON-NLS-1$
        response.setCharacterEncoding( "UTF-8" ); //$NON-NLS-1$
        final Locale loc = getLocale( request );
        final String locale = ( loc != null ) ? loc.toString() : null;
        cas.printConfigurationJson( response.getWriter(), pid, config, pidFilter, locale );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        // let's check for a JSON request
        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) ) //$NON-NLS-1$
        {
            response.setContentType( "application/json" ); //$NON-NLS-1$
            response.setCharacterEncoding( "UTF-8" ); //$NON-NLS-1$

            // after last slash and without extension
            String pid = info.substring( info.lastIndexOf( '/' ) + 1, info.length() - 5 );
            // check whether the PID is actually a filter for the selection
            // of configurations to display, if the filter correctly converts
            // into an OSGi filter, we use it to select configurations
            // to display
            String pidFilter = request.getParameter( PID_FILTER );
            if ( pidFilter == null )
            {
                pidFilter = pid;
            }
            try
            {
                getBundleContext().createFilter( pidFilter );

                // if the pidFilter was set from the PID, clear the PID
                if ( pid == pidFilter )
                {
                    pid = null;
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // its OK, if the PID is just a single PID
                pidFilter = null;
            }

            // check both PID and PID filter
            if ( pid != null && !isAllowedPid(pid) )
            {
                response.sendError(500);
            }
            if ( pidFilter != null && !isAllowedPid(pidFilter) )
            {
                response.sendError(500);
            }


            final Locale loc = getLocale( request );
            final String locale = ( loc != null ) ? loc.toString() : null;

            final PrintWriter pw = response.getWriter();
            pw.write( "[" ); //$NON-NLS-1$
            final ConfigAdminSupport ca = this.getConfigurationAdminSupport();
            if ( ca != null )
            {
                try
                {
                    final Map services = ca.getServices( pid, pidFilter, locale, false );
                    boolean printComma = false;
                    for ( Iterator spi = services.keySet().iterator(); spi.hasNext(); )
                    {
                        final String servicePid = ( String ) spi.next();
                        final Configuration config = ca.getConfiguration( servicePid );
                        if ( config != null )
                        {
                            if ( printComma )
                            {
                                pw.print( ',' );
                            }
                            ca.printConfigurationJson( pw, servicePid, config, pidFilter, locale );
                            printComma = true;
                        }
                    }
                }
                catch ( InvalidSyntaxException e )
                {
                    // this should not happened as we checked the filter before
                }
            }
            pw.write( "]" ); //$NON-NLS-1$

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        // extract the configuration PID from the request path
        String pid = request.getPathInfo().substring(this.getLabel().length() + 1);
        if ( pid.length() == 0 ) {
            pid = null;
        } else {
            pid = pid.substring( pid.lastIndexOf( '/' ) + 1 );
        }
        // check whether the PID is actually a filter for the selection
        // of configurations to display, if the filter correctly converts
        // into an OSGi filter, we use it to select configurations
        // to display
        String pidFilter = request.getParameter( PID_FILTER );
        if ( pidFilter == null )
        {
            pidFilter = pid;
        }
        if ( pidFilter != null )
        {
            try
            {
                getBundleContext().createFilter( pidFilter );

                // if the pidFilter was set from the PID, clear the PID
                if ( pid == pidFilter )
                {
                    pid = null;
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // its OK, if the PID is just a single PID
                pidFilter = null;
            }
        }

        // check both PID and PID filter
        if ( pid != null && !isAllowedPid(pid) )
        {
            response.sendError(500);
        }
        if ( pidFilter != null && !isAllowedPid(pidFilter) )
        {
            response.sendError(500);
        }

        final Locale loc = getLocale( request );
        final String locale = ( loc != null ) ? loc.toString() : null;


        JSONObject json = new JSONObject();
        try
        {
            final ConfigAdminSupport ca = getConfigurationAdminSupport();
            json.put("status", ca != null ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$
            if ( ca != null )
            {
                ca.listConfigurations( json, pidFilter, locale, loc );
                ca.listFactoryConfigurations( json, pidFilter, locale );
            }
        }
        catch (JSONException e)
        {
            throw new IOException(e.toString());
        }

        // if a configuration is addressed, display it immediately
        if ( request.getParameter( "create" ) != null && pid != null ) //$NON-NLS-1$
        {
            pid = PLACEHOLDER_PID; // new PlaceholderConfiguration( pid ).getPid();
        }


        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", json.toString() ); //$NON-NLS-1$
        vars.put( "selectedPid", pid != null ? pid : ""); //$NON-NLS-1$ //$NON-NLS-2$

        response.getWriter().print(TEMPLATE);
    }

    private ConfigAdminSupport getConfigurationAdminSupport()
    {
        Object configurationAdmin = getService( CONFIGURATION_ADMIN_NAME );
        if ( configurationAdmin != null )
        {
            return new ConfigAdminSupport( this, this.getBundleContext(), configurationAdmin );
        }
        return null;
    }


}

