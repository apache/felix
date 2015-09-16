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
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
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
    static final String FACTORY_PID = "factoryPid"; //$NON-NLS-1$
    static final String PLACEHOLDER_PID = "[Temporary PID replaced by real PID upon save]"; //$NON-NLS-1$
    static final String REFERER = "referer"; //$NON-NLS-1$
    static final String FACTORY_CREATE = "factoryCreate"; //$NON-NLS-1$

    static final String ACTION_CREATE = "create"; //$NON-NLS-1$
    static final String ACTION_DELETE = "delete"; //$NON-NLS-1$
    static final String ACTION_APPLY = "apply"; //$NON-NLS-1$
    static final String ACTION_UNBIND = "unbind"; //$NON-NLS-1$
    static final String PROPERTY_LIST = "propertylist"; //$NON-NLS-1$
    static final String LOCATION = "$location"; //$NON-NLS-1$

    static final String CONFIGURATION_ADMIN_NAME = "org.osgi.service.cm.ConfigurationAdmin"; //$NON-NLS-1$
    static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService"; //$NON-NLS-1$

    public static final String UNBOUND_LOCATION = "??unbound:bundle/location"; //$NON-NLS-1$

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
        if ( request.getParameter( ACTION_CREATE ) != null )
        {
            config = cas.getPlaceholderConfiguration( pid ); // ca.createFactoryConfiguration( pid, null );
            pid = config.getPid();
        }
        else if ( request.getParameter( ACTION_APPLY ) != null )
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
                response.setContentType( "application/json" ); //$NON-NLS-1$
                response.setCharacterEncoding( "UTF-8" ); //$NON-NLS-1$
                response.getWriter().print( "{ \"status\": true }" ); //$NON-NLS-1$
            }

            return;
        }

        if ( config == null )
        {
            config = cas.getConfiguration( pid );
        }

        // check for configuration unbinding
        if ( request.getParameter( ACTION_UNBIND ) != null )
        {
            if ( config != null && config.getBundleLocation() != null )
            {
                config.setBundleLocation( UNBOUND_LOCATION );

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
        // check for "post" requests from previous versions
        if ( "true".equals(request.getParameter("post")) ) { //$NON-NLS-1$ //$NON-NLS-2$
            this.doPost(request, response);
            return;
        }
        final String info = request.getPathInfo();
        // let's check for a JSON request
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
                // create filter
                final StringBuffer sb = new StringBuffer();
                if ( pid != null && pidFilter != null)
                {
                    sb.append("(&"); //$NON-NLS-1$
                }
                if ( pid != null )
                {
                    sb.append('(');
                    sb.append(Constants.SERVICE_PID);
                    sb.append('=');
                    sb.append(pid);
                    sb.append(')');
                }
                if ( pidFilter != null )
                {
                    sb.append(pidFilter);
                }
                if ( pid != null && pidFilter != null)
                {
                    sb.append(')');
                }
                final String filter = sb.toString();
                try
                {
                    // we use listConfigurations to not create configuration
                    // objects persistently without the user providing actual
                    // configuration
                    final Configuration[] configs = ca.listConfigurations( filter );
                    boolean printComma = false;
                    for(int i=0; configs != null && i<configs.length; i++)
                    {
                        final Configuration config = configs[i];
                        if ( config != null )
                        {
                            if ( printComma )
                            {
                                pw.print( ',' );
                            }
                            ca.printConfigurationJson( pw, config.getPid(), config, null, locale );
                            printComma = true;
                        }
                    }
                }
                catch ( final InvalidSyntaxException ise )
                {
                    // should print message
                    // however this should not happen as we checked the filter before
                }
                catch ( final IOException ioe )
                {
                    // should print message
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
        if ( request.getParameter( ACTION_CREATE ) != null && pid != null )
        {
            pid = PLACEHOLDER_PID; // new PlaceholderConfiguration( pid ).getPid();
        }


        // prepare variables
        final String referer = request.getParameter( REFERER );
        final boolean factoryCreate = "true".equals( request.getParameter(FACTORY_CREATE) ); //$NON-NLS-1$
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", json.toString() ); //$NON-NLS-1$
        vars.put( "selectedPid", pid != null ? pid : "" ); //$NON-NLS-1$ //$NON-NLS-2$
        vars.put( "configurationReferer", referer != null ? referer : "" ); //$NON-NLS-1$ //$NON-NLS-2$
        vars.put( "factoryCreate", Boolean.valueOf(factoryCreate) ); //$NON-NLS-1$
        vars.put( "param.apply", ACTION_APPLY ); //$NON-NLS-1$
        vars.put( "param.create", ACTION_CREATE ); //$NON-NLS-1$
        vars.put( "param.unbind", ACTION_UNBIND ); //$NON-NLS-1$
        vars.put( "param.delete", ACTION_DELETE ); //$NON-NLS-1$
        vars.put( "param.propertylist", PROPERTY_LIST ); //$NON-NLS-1$
        vars.put( "param.pidFilter", PID_FILTER ); //$NON-NLS-1$

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

