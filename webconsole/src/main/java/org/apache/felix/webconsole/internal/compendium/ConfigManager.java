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
package org.apache.felix.webconsole.internal.compendium;


import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>ConfigManager</code>
 */
public class ConfigManager extends ConfigManagerBase
{

    private static final long serialVersionUID = 5021174538498622428L;

    private static final String LABEL = "configMgr"; // was name //$NON-NLS-1$
    private static final String TITLE = "%configMgr.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = { "/res/ui/config.css" }; //$NON-NLS-1$

    private static final String PID_FILTER = "pidFilter"; //$NON-NLS-1$
    private static final String PID = "pid"; //$NON-NLS-1$
    private static final String factoryPID = "factoryPid"; //$NON-NLS-1$

    private static final String PLACEHOLDER_PID = "[Temporary PID replaced by real PID upon save]";

    /**
     * Attribute type code for PASSWORD attributes as defined in
     * Metatype Service Specification 1.2. Since we cannot yet refer
     * to the 1.2 API package we just replicate the type code here. Once
     * the API is available and can be referred to, we should use it.
     */
    private static final int ATTRIBUTE_TYPE_PASSWORD = 12;

    /**
     * Marker value of password fields used as dummy values and
     * indicating unmodified values.
     */
    private static final String PASSWORD_PLACEHOLDER_VALUE = "unmodified"; //$NON-NLS-1$

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    public ConfigManager()
    {
        super(LABEL, TITLE, CATEGORY_OSGI, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/config.html" ); //$NON-NLS-1$
    }

    private static final boolean isAllowedPid(final String pid)
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

        final ConfigurationAdmin ca = this.getConfigurationAdmin();

        // ignore this request if the PID and/or configuration admin is missing
        if ( pid == null || pid.length() == 0 || ca == null )
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
            config = new PlaceholderConfiguration( pid ); // ca.createFactoryConfiguration( pid, null );
            pid = config.getPid();
        }
        else if ( request.getParameter( "apply" ) != null ) //$NON-NLS-1$
        {
            String redirect = applyConfiguration( request, ca, pid );
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
            config = getConfiguration( ca, pid );
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
        printConfigurationJson( response.getWriter(), pid, config, pidFilter, locale );
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

            final ConfigurationAdmin ca = this.getConfigurationAdmin();

            final Locale loc = getLocale( request );
            final String locale = ( loc != null ) ? loc.toString() : null;

            final PrintWriter pw = response.getWriter();

            try
            {
                pw.write( "[" ); //$NON-NLS-1$
                final Map services = this.getServices( pid, pidFilter, locale, false );
                boolean printColon = false;
                for ( Iterator spi = services.keySet().iterator(); spi.hasNext(); )
                {
                    final String servicePid = ( String ) spi.next();
                    final Configuration config = getConfiguration( ca, servicePid );
                    if ( config != null )
                    {
                        if ( printColon )
                        {
                            pw.print( ',' );
                        }
                        this.printConfigurationJson( pw, servicePid, config, pidFilter, locale );
                        printColon = true;
                    }
                }
                pw.write( "]" ); //$NON-NLS-1$
            }
            catch ( InvalidSyntaxException e )
            {
                // this should not happened as we checked the filter before
            }

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
        final ConfigurationAdmin ca = this.getConfigurationAdmin();

        final Locale loc = getLocale( request );
        final String locale = ( loc != null ) ? loc.toString() : null;


        JSONObject json = new JSONObject();
        try
        {
            json.put("status", ca != null ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$
            if ( ca != null )
            {
                listConfigurations( json, ca, pidFilter, locale, loc );
                listFactoryConfigurations( json, pidFilter, locale );
            }
        }
        catch (JSONException e)
        {
            throw new IOException(e.toString());
        }

        // if a configuration is addressed, display it immediately
        if ( request.getParameter( "create" ) != null && pid != null ) //$NON-NLS-1$
        {
            pid = new PlaceholderConfiguration( pid ).getPid();
        }


        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", json.toString() ); //$NON-NLS-1$
        vars.put( "selectedPid", pid != null ? pid : ""); //$NON-NLS-1$ //$NON-NLS-2$

        response.getWriter().print(TEMPLATE);
    }


    private static final Configuration getConfiguration( ConfigurationAdmin ca, String pid )
    {
        if ( ca != null && pid != null )
        {
            try
            {
                // we use listConfigurations to not create configuration
                // objects persistently without the user providing actual
                // configuration
                String filter = '(' + Constants.SERVICE_PID + '=' + pid + ')';
                Configuration[] configs = ca.listConfigurations( filter );
                if ( configs != null && configs.length > 0 )
                {
                    return configs[0];
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // should print message
            }
            catch ( IOException ioe )
            {
                // should print message
            }
        }

        // fallback to no configuration at all
        return null;
    }


    private final void listFactoryConfigurations(JSONObject json, String pidFilter,
        String locale)
    {
        try
        {
            Map optionsFactory = getServices(ManagedServiceFactory.class.getName(),
                pidFilter, locale, true);
            addMetaTypeNames(optionsFactory, getFactoryPidObjectClasses(locale),
                pidFilter, ConfigurationAdmin.SERVICE_FACTORYPID);
            for ( Iterator ii = optionsFactory.keySet().iterator(); ii.hasNext(); )
            {
                String id = ( String ) ii.next();
                Object name = optionsFactory.get( id );
                json.append( "fpids", new JSONObject() //$NON-NLS-1$
                    .put( "id", id ) //$NON-NLS-1$
                    .put( "name", name ) ); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            log("listFactoryConfigurations: Unexpected problem encountered", e);
        }
    }

    private final void listConfigurations(JSONObject json, ConfigurationAdmin ca,
        String pidFilter, String locale, Locale loc)
    {
        try
        {
            // start with ManagedService instances
            Map optionsPlain = getServices(ManagedService.class.getName(), pidFilter,
                locale, true);

            // next are the MetaType informations without ManagedService
            addMetaTypeNames(optionsPlain, getPidObjectClasses(locale), pidFilter,
                Constants.SERVICE_PID);

            // add in existing configuration (not duplicating ManagedServices)
            Configuration[] cfgs = ca.listConfigurations(pidFilter);
            for (int i = 0; cfgs != null && i < cfgs.length; i++)
            {

                // ignore configuration object if an entry already exists in the map
                // or if it is invalid
                final String pid = cfgs[i].getPid();
                if (optionsPlain.containsKey(pid) || !isAllowedPid(pid) )
                {
                    continue;
                }

                // insert and entry for the PID
                ObjectClassDefinition ocd = null;
                try
                {
                    ocd = this.getObjectClassDefinition(cfgs[i], locale);
                }
                catch (IllegalArgumentException t)
                {
                    // MetaTypeProvider.getObjectClassDefinition might throw illegal
                    // argument exception. So we must catch it here, otherwise the
                    // other configurations will not be shown
                    // See https://issues.apache.org/jira/browse/FELIX-2390
                }

                if (ocd != null)
                {
                    optionsPlain.put(pid, ocd.getName());
                }
                else
                {
                	optionsPlain.put(pid, pid);
                }
            }

            for ( Iterator ii = optionsPlain.keySet().iterator(); ii.hasNext(); )
            {
                String id = ( String ) ii.next();
                Object name = optionsPlain.get( id );

                final Configuration config = getConfiguration( ca, id );
                JSONObject data = new JSONObject() //
                    .put( "id", id ) //$NON-NLS-1$
                    .put( "name", name ); //$NON-NLS-1$
                if ( null != config )
                {
                    final String fpid = config.getFactoryPid();
                    if ( null != fpid )
                    {
                        data.put( "fpid", fpid ); //$NON-NLS-1$
                    }

                    final Bundle bundle = getBoundBundle( config );
                    if ( null != bundle )
                    {
                        data.put( "bundle", bundle.getBundleId() ); //$NON-NLS-1$
                        data.put( "bundle_name", Util.getName( bundle, loc ) ); //$NON-NLS-1$
                    }
                }

                json.append( "pids", data ); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            log("listConfigurations: Unexpected problem encountered", e);
        }
    }

    private final Bundle getBoundBundle(Configuration config)
    {
        if (null == config)
            return null;
        final String location = config.getBundleLocation();
        if (null == location)
            return null;

        final Bundle bundles[] = getBundleContext().getBundles();
        for (int i = 0; bundles != null && i < bundles.length; i++)
        {
            if (bundles[i].getLocation().equals(location))
                return bundles[i];

        }
        return null;
    }


    private SortedMap getServices( String serviceClass, String serviceFilter, String locale,
        boolean ocdRequired ) throws InvalidSyntaxException
    {
        // sorted map of options
        SortedMap optionsFactory = new TreeMap( String.CASE_INSENSITIVE_ORDER );

        // find all ManagedServiceFactories to get the factoryPIDs
        ServiceReference[] refs = this.getBundleContext().getServiceReferences( serviceClass, serviceFilter );
        for ( int i = 0; refs != null && i < refs.length; i++ )
        {
            Object pidObject = refs[i].getProperty( Constants.SERVICE_PID );
            // only include valid PIDs
            if ( pidObject instanceof String && isAllowedPid((String)pidObject) )
            {
                String pid = ( String ) pidObject;
                String name;
                final ObjectClassDefinition ocd = this.getObjectClassDefinition( refs[i].getBundle(), pid, locale );
                if ( ocd != null )
                {
                    name = ocd.getName();
                }
                else
                {
                    name = pid;
                }

                if ( !ocdRequired || ocd != null ) {
                    optionsFactory.put( pid, name );
                }
            }
        }

        return optionsFactory;
    }


    private void addMetaTypeNames( final Map pidMap, final Map ocdCollection, final String filterSpec, final String type )
    {
        Filter filter = null;
        if ( filterSpec != null )
        {
            try
            {
                filter = getBundleContext().createFilter( filterSpec );
            }
            catch ( InvalidSyntaxException not_expected )
            {
                /* filter is correct */
            }
        }

        for ( Iterator ei = ocdCollection.entrySet().iterator(); ei.hasNext(); )
        {
            Entry ociEntry = ( Entry ) ei.next();
            final String pid = ( String ) ociEntry.getKey();
            final ObjectClassDefinition ocd = ( ObjectClassDefinition ) ociEntry.getValue();
            if ( filter == null )
            {
                pidMap.put( pid, ocd.getName() );
            }
            else
            {
                final Dictionary props = new Hashtable();
                props.put( type, pid );
                if ( filter.match( props ) )
                {
                    pidMap.put( pid, ocd.getName() );
                }
            }
        }
    }


    private void printConfigurationJson( PrintWriter pw, String pid, Configuration config, String pidFilter,
        String locale )
    {

        JSONWriter result = new JSONWriter( pw );

        if ( pid != null )
        {
            try
            {
                result.object();
                this.configForm( result, pid, config, pidFilter, locale );
                result.endObject();
            }
            catch ( Exception e )
            {
                log( "Error reading configuration PID " + pid, e );
            }
        }

    }


    private void configForm( JSONWriter json, String pid, Configuration config, String pidFilter, String locale )
        throws JSONException
    {

        json.key( ConfigManager.PID );
        json.value( pid );

        if ( pidFilter != null )
        {
            json.key( PID_FILTER );
            json.value( pidFilter );
        }

        Dictionary props = null;
        ObjectClassDefinition ocd = null;
        if ( config != null )
        {
            props = config.getProperties(); // unchecked
            ocd = this.getObjectClassDefinition( config, locale );
        }
        if (ocd == null)
        {
            ocd = this.getObjectClassDefinition( pid, locale );
        }

        if ( props == null )
        {
            props = new Hashtable();
        }

        if ( ocd != null )
        {
            mergeWithMetaType( props, ocd, json );
        }
        else
        {
            json.key( "title" ).value( pid ); //$NON-NLS-1$
            json.key( "description" ).value( //$NON-NLS-1$
                "This form is automatically generated from existing properties because no property "
                    + "descriptors are available for this configuration. This may be cause by the absence "
                    + "of the OSGi Metatype Service or the absence of a MetaType descriptor for this configuration." );

            json.key( "properties" ).object(); //$NON-NLS-1$
            for ( Enumeration pe = props.keys(); pe.hasMoreElements(); )
            {
                final String id = ( String ) pe.nextElement();

                // ignore well known special properties
                if ( !id.equals( Constants.SERVICE_PID ) && !id.equals( Constants.SERVICE_DESCRIPTION )
                    && !id.equals( Constants.SERVICE_ID ) && !id.equals( Constants.SERVICE_VENDOR )
                    && !id.equals( ConfigurationAdmin.SERVICE_BUNDLELOCATION )
                    && !id.equals( ConfigurationAdmin.SERVICE_FACTORYPID ) )
                {
                    final Object value = props.get( id );
                    final AttributeDefinition ad = getAttributeDefinition( id, value );
                    json.key( id );
                    attributeToJson( json, ad, value );
                }
            }
            json.endObject();
        }

        if ( config != null )
        {
            this.addConfigurationInfo( config, json, locale );
        }
    }


    private static final void mergeWithMetaType( Dictionary props, ObjectClassDefinition ocd, JSONWriter json ) throws JSONException
    {
        json.key( "title" ).value( ocd.getName() ); //$NON-NLS-1$

        if ( ocd.getDescription() != null )
        {
            json.key( "description" ).value( ocd.getDescription() ); //$NON-NLS-1$
        }

        AttributeDefinition[] ad = ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
        if ( ad != null )
        {
            json.key( "properties" ).object(); //$NON-NLS-1$
            for ( int i = 0; i < ad.length; i++ )
            {
                final AttributeDefinition adi = ad[i];
                final String attrId = adi.getID();
                json.key( attrId );
                attributeToJson( json, adi, props.get( attrId ) );
            }
            json.endObject();
        }
    }


    private void addConfigurationInfo( Configuration config, JSONWriter json, String locale ) throws JSONException
    {

        if ( config.getFactoryPid() != null )
        {
            json.key( factoryPID );
            json.value( config.getFactoryPid() );
        }

        String location;
        if ( config.getBundleLocation() == null )
        {
            location = ""; //$NON-NLS-1$
        }
        else
        {
            // if the configuration is bound to a bundle location which
            // is not related to an installed bundle, we just print the
            // raw bundle location binding
            Bundle bundle = this.getBundle( config.getBundleLocation() );
            if ( bundle == null )
            {
                location = config.getBundleLocation();
            }
            else
            {
                Dictionary headers = bundle.getHeaders( locale );
                String name = ( String ) headers.get( Constants.BUNDLE_NAME );
                if ( name == null )
                {
                    location = bundle.getSymbolicName();
                }
                else
                {
                    location = name + " (" + bundle.getSymbolicName() + ')'; //$NON-NLS-1$
                }

                Version v = Version.parseVersion( ( String ) headers.get( Constants.BUNDLE_VERSION ) );
                location += ", Version " + v.toString();
            }
        }
        json.key( "bundleLocation" ); //$NON-NLS-1$
        json.value( location );
        // raw bundle location and service locations
        final String pid = config.getPid();
        String serviceLocation = ""; //$NON-NLS-1$
        try
        {
            final ServiceReference[] refs = getBundleContext().getServiceReferences(
                null,
                "(&(" + Constants.OBJECTCLASS + '=' + ManagedService.class.getName() //$NON-NLS-1$
                    + ")(" + Constants.SERVICE_PID + '=' + pid + "))"); //$NON-NLS-1$ //$NON-NLS-2$
            if ( refs != null && refs.length > 0 )
            {
                serviceLocation = refs[0].getBundle().getLocation();
            }
        }
        catch (Throwable t)
        {
            log( "Error getting service associated with configuration " + pid, t );
        }
        json.key( "bundle_location" ); //$NON-NLS-1$
        json.value ( config.getBundleLocation() );
        json.key( "service_location" ); //$NON-NLS-1$
        json.value ( serviceLocation );
    }


    private String applyConfiguration( HttpServletRequest request, ConfigurationAdmin ca, String pid )
        throws IOException
    {
        if ( request.getParameter( "delete" ) != null ) //$NON-NLS-1$
        {
            // only delete if the PID is not our place holder
            if ( !PLACEHOLDER_PID.equals( pid ) )
            {
                log( "applyConfiguration: Deleting configuration " + pid );
                Configuration config = ca.getConfiguration( pid, null );
                config.delete();
            }
            return null; // return request.getHeader( "Referer" );
        }

        String factoryPid = request.getParameter( ConfigManager.factoryPID );
        Configuration config = null;

        String propertyList = request.getParameter( "propertylist" ); //$NON-NLS-1$
        if ( propertyList == null )
        {
            // FIXME: this would be a bug !!
        }
        else
        {
            config = getConfiguration( ca, pid, factoryPid );

            Dictionary props = config.getProperties();
            if ( props == null )
            {
                props = new Hashtable();
            }

            Map adMap = this.getAttributeDefinitionMap( config, null );
            StringTokenizer propTokens = new StringTokenizer( propertyList, "," ); //$NON-NLS-1$
            while ( propTokens.hasMoreTokens() )
            {
                String propName = propTokens.nextToken();
                AttributeDefinition ad = (AttributeDefinition) adMap.get( propName );

                // try to derive from current value
                if (ad == null) {
                    Object currentValue = props.get( propName );
                    ad = getAttributeDefinition( propName, currentValue );
                }

                int attributeType = getAttributeType( ad );

                if ( ad == null
                    || ( ad.getCardinality() == 0 && ( attributeType == AttributeDefinition.STRING || attributeType == ATTRIBUTE_TYPE_PASSWORD ) ) )
                {
                    String prop = request.getParameter( propName );
                    if ( prop != null
                        && ( attributeType != ATTRIBUTE_TYPE_PASSWORD || !PASSWORD_PLACEHOLDER_VALUE.equals( prop ) ) )
                    {
                        props.put( propName, prop );
                    }
                }
                else if ( ad.getCardinality() == 0 )
                {
                    // scalar of non-string
                    String prop = request.getParameter( propName );
                    if ( prop != null )
                    {
                        try
                        {
                            props.put( propName, toType( attributeType, prop ) );
                        }
                        catch ( NumberFormatException nfe )
                        {
                            // don't care
                        }
                    }
                }
                else
                {
                    // array or vector of any type
                    Vector vec = new Vector();

                    String[] properties = request.getParameterValues( propName );
                    if ( properties != null )
                    {
                        if ( attributeType == ATTRIBUTE_TYPE_PASSWORD )
                        {
                            setPasswordProps( vec, properties, props.get( propName ) );
                        }
                        else
                        {
                            for ( int i = 0; i < properties.length; i++ )
                            {
                                try
                                {
                                    vec.add( toType( attributeType, properties[i] ) );
                                }
                                catch ( NumberFormatException nfe )
                                {
                                    // don't care
                                }
                            }
                        }
                    }

                    // but ensure size (check for positive value since
                    // abs(Integer.MIN_VALUE) is still INTEGER.MIN_VALUE)
                    int maxSize = Math.abs( ad.getCardinality() );
                    if ( vec.size() > maxSize && maxSize > 0 )
                    {
                        vec.setSize( maxSize );
                    }

                    if ( ad.getCardinality() < 0 )
                    {
                        // keep the vector, but only add if not empty
                        if ( vec.isEmpty() )
                        {
                            props.remove( propName );
                        }
                        else
                        {
                            props.put( propName, vec );
                        }
                    }
                    else
                    {
                        // convert to an array
                        props.put( propName, toArray( attributeType, vec ) );
                    }
                }
            }

            config.update( props );
        }

        // redirect to the new configuration (if existing)
        return (config != null) ? config.getPid() : ""; //$NON-NLS-1$
    }


    private static final Configuration getConfiguration( ConfigurationAdmin ca, String pid, String factoryPid ) throws IOException
    {
        if ( factoryPid != null && ( pid == null || pid.equals( PLACEHOLDER_PID ) ) )
        {
            return ca.createFactoryConfiguration( factoryPid, null );
        }

        return ca.getConfiguration( pid, null );
    }


    private static void attributeToJson( final JSONWriter json, final AttributeDefinition ad, final Object propValue )
        throws JSONException
    {
        json.object();

        Object value;
        if ( propValue != null )
        {
            value = propValue;
        }
        else if ( ad.getDefaultValue() != null )
        {
            value = ad.getDefaultValue();
        }
        else if ( ad.getCardinality() == 0 )
        {
            value = ""; //$NON-NLS-1$
        }
        else
        {
            value = new String[0];
        }

        json.key( "name" ); //$NON-NLS-1$
        json.value( ad.getName() );

        // attribute type - overwrite metatype provided type
        // if the property name contains "password" and the
        // type is string
        int propertyType = getAttributeType( ad );

        json.key( "type" ); //$NON-NLS-1$
        if ( ad.getOptionLabels() != null && ad.getOptionLabels().length > 0 )
        {
            json.object();
            json.key( "labels" ); //$NON-NLS-1$
            json.value( Arrays.asList( ad.getOptionLabels() ) );
            json.key( "values" ); //$NON-NLS-1$
            json.value( Arrays.asList( ad.getOptionValues() ) );
            json.endObject();
        }
        else
        {
            json.value( propertyType );
        }

        // unless the property is of password type, send it
        final boolean isPassword = propertyType == ATTRIBUTE_TYPE_PASSWORD;
        if ( ad.getCardinality() == 0 )
        {
            // scalar
            if ( isPassword )
            {
                value = PASSWORD_PLACEHOLDER_VALUE;
            }
            else if ( value instanceof Vector )
            {
                value = ( ( Vector ) value ).get( 0 );
            }
            else if ( value.getClass().isArray() )
            {
                value = Array.get( value, 0 );
            }
            json.key( "value" ); //$NON-NLS-1$
            json.value( value );
        }
        else
        {
            value = new JSONArray( toList( value ) );
            if ( isPassword )
            {
                JSONArray tmp = ( JSONArray ) value;
                for ( int tmpI = 0; tmpI < tmp.length(); tmpI++ )
                {
                    tmp.put( tmpI, PASSWORD_PLACEHOLDER_VALUE );
                }
            }
            json.key( "values" ); //$NON-NLS-1$
            json.value( value );
        }

        if ( ad.getDescription() != null )
        {
            json.key( "description" ); //$NON-NLS-1$
            json.value( ad.getDescription() + " (" + ad.getID() + ")" ); //$NON-NLS-1$ //$NON-NLS-2$
        }

        json.endObject();
    }


    private static AttributeDefinition getAttributeDefinition( final String id, final Object value )
    {
        int attrType;
        int attrCardinality;
        Class type;

        if ( value == null )
        {
            attrCardinality = 0;
            type = String.class;
        }
        else if ( value instanceof Collection )
        {
            attrCardinality = Integer.MIN_VALUE;
            Collection coll = ( Collection ) value;
            if ( coll.isEmpty() )
            {
                type = String.class;
            }
            else
            {
                type = coll.iterator().next().getClass();
            }
        }
        else if ( value.getClass().isArray() )
        {
            attrCardinality = Integer.MAX_VALUE;
            type = value.getClass().getComponentType();
        }
        else
        {
            attrCardinality = 0;
            type = value.getClass();
        }

        if ( type == Boolean.class || type == Boolean.TYPE )
        {
            attrType = AttributeDefinition.BOOLEAN;
        }
        else if ( type == Byte.class || type == Byte.TYPE )
        {
            attrType = AttributeDefinition.BYTE;
        }
        else if ( type == Character.class || type == Character.TYPE )
        {
            attrType = AttributeDefinition.CHARACTER;
        }
        else if ( type == Double.class || type == Double.TYPE )
        {
            attrType = AttributeDefinition.DOUBLE;
        }
        else if ( type == Float.class || type == Float.TYPE )
        {
            attrType = AttributeDefinition.FLOAT;
        }
        else if ( type == Long.class || type == Long.TYPE )
        {
            attrType = AttributeDefinition.LONG;
        }
        else if ( type == Integer.class || type == Integer.TYPE )
        {
            attrType = AttributeDefinition.INTEGER;
        }
        else if ( type == Short.class || type == Short.TYPE )
        {
            attrType = AttributeDefinition.SHORT;
        }
        else
        {
            attrType = AttributeDefinition.STRING;
        }

        return new PlaceholderAttributeDefinition( id, attrType, attrCardinality );
   }

    private static boolean isPasswordProperty(String name)
    {
        return name == null ? false : name.toLowerCase().indexOf("password") != -1; //$NON-NLS-1$
    }

    private static int getAttributeType( final AttributeDefinition ad )
    {
        if ( ad.getType() == AttributeDefinition.STRING && isPasswordProperty( ad.getID() ) )
        {
            return ATTRIBUTE_TYPE_PASSWORD;
        }
        return ad.getType();
    }


    /**
     * @throws NumberFormatException If the value cannot be converted to
     *      a number and type indicates a numeric type
     */
    private static final Object toType( int type, String value )
    {
        switch ( type )
        {
            case AttributeDefinition.BOOLEAN:
                return Boolean.valueOf( value );
            case AttributeDefinition.BYTE:
                return Byte.valueOf( value );
            case AttributeDefinition.CHARACTER:
                char c = ( value.length() > 0 ) ? value.charAt( 0 ) : 0;
                return new Character( c );
            case AttributeDefinition.DOUBLE:
                return Double.valueOf( value );
            case AttributeDefinition.FLOAT:
                return Float.valueOf( value );
            case AttributeDefinition.LONG:
                return Long.valueOf( value );
            case AttributeDefinition.INTEGER:
                return Integer.valueOf( value );
            case AttributeDefinition.SHORT:
                return Short.valueOf( value );
            default:
                // includes AttributeDefinition.STRING
                // includes ATTRIBUTE_TYPE_PASSWORD/AttributeDefinition.PASSWORD
                return value;
        }
    }


    private static List toList( Object value )
    {
        if ( value instanceof Vector )
        {
            return ( Vector ) value;
        }
        else if ( value.getClass().isArray() )
        {
            if ( value.getClass().getComponentType().isPrimitive() )
            {
                final int len = Array.getLength( value );
                final Object[] tmp = new Object[len];
                for ( int j = 0; j < len; j++ )
                {
                    tmp[j] = Array.get( value, j );
                }
                value = tmp;
            }
            return Arrays.asList( ( Object[] ) value );
        }
        else
        {
            return Collections.singletonList( value );
        }
    }


    private static void setPasswordProps( final Vector vec, final String[] properties, Object props )
    {
        List propList = toList( props );
        for ( int i = 0; i < properties.length; i++ )
        {
            if ( PASSWORD_PLACEHOLDER_VALUE.equals( properties[i] ) )
            {
                if ( i < propList.size() && propList.get( i ) != null )
                {
                    vec.add( propList.get( i ) );
                }
            }
            else
            {
                vec.add( properties[i] );
            }
        }
    }


    private static final Object toArray( int type, Vector values )
    {
        int size = values.size();

        // short cut for string array
        if ( type == AttributeDefinition.STRING || type == ATTRIBUTE_TYPE_PASSWORD )
        {
            return values.toArray( new String[size] );
        }

        Object array;
        switch ( type )
        {
            case AttributeDefinition.BOOLEAN:
                array = new boolean[size];
                break;
            case AttributeDefinition.BYTE:
                array = new byte[size];
                break;
            case AttributeDefinition.CHARACTER:
                array = new char[size];
                break;
            case AttributeDefinition.DOUBLE:
                array = new double[size];
                break;
            case AttributeDefinition.FLOAT:
                array = new float[size];
                break;
            case AttributeDefinition.LONG:
                array = new long[size];
                break;
            case AttributeDefinition.INTEGER:
                array = new int[size];
                break;
            case AttributeDefinition.SHORT:
                array = new short[size];
                break;
            default:
                // unexpected, but assume string
                array = new String[size];
        }

        for ( int i = 0; i < size; i++ )
        {
            Array.set( array, i, values.get( i ) );
        }

        return array;
    }

    private static class PlaceholderConfiguration implements Configuration
    {

        private final String factoryPid;
        private String bundleLocation;


        PlaceholderConfiguration( String factoryPid )
        {
            this.factoryPid = factoryPid;
        }


        public String getPid()
        {
            return PLACEHOLDER_PID;
        }


        public String getFactoryPid()
        {
            return factoryPid;
        }


        public void setBundleLocation( String bundleLocation )
        {
            this.bundleLocation = bundleLocation;
        }


        public String getBundleLocation()
        {
            return bundleLocation;
        }


        public Dictionary getProperties()
        {
            // dummy configuration has no properties
            return null;
        }


        public void update()
        {
            // dummy configuration cannot be updated
        }


        public void update( Dictionary properties )
        {
            // dummy configuration cannot be updated
        }


        public void delete()
        {
            // dummy configuration cannot be deleted
        }

    }

    private static class PlaceholderAttributeDefinition implements AttributeDefinition
    {

        final String id;
        final int type;
        final int cardinality;


        public PlaceholderAttributeDefinition( final String id, int type, int cardinality )
        {
            this.id = id;
            this.type = type;
            this.cardinality = cardinality;
        }


        public String getName()
        {
            return id;
        }


        public String getID()
        {
            return id;
        }


        public String getDescription()
        {
            // no description
            return null;
        }


        public int getCardinality()
        {
            return cardinality;
        }


        public int getType()
        {
            return type;
        }


        public String[] getOptionValues()
        {
            return null;
        }


        public String[] getOptionLabels()
        {
            return null;
        }


        public String validate( String value )
        {
            // no validation
            return null;
        }


        public String[] getDefaultValue()
        {
            return null;
        }
    }
}
