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
package org.apache.felix.webconsole.internal.core;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.framework.util.VersionRange;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.bundleinfo.BundleInfo;
import org.apache.felix.webconsole.bundleinfo.BundleInfoProvider;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>BundlesServlet</code> provides the bundles plugins, used to display
 * the list of bundles, installed on the framework. It also adds ability to control
 * the lifecycle of the bundles, like start, stop, uninstall, install.
 */
public class BundlesServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin, ConfigurationPrinter
{

    /** the label of the bundles plugin - used by other plugins to reference to plugin details */
    public static final String NAME = "bundles";
    private static final String TITLE = "%bundles.pluginTitle";
    private static final String CSS[] = { "/res/ui/bundles.css" };

    // an LDAP filter, that is used to search manifest headers, see FELIX-1441
    private static final String FILTER_PARAM = "filter";

    private static final String FIELD_STARTLEVEL = "bundlestartlevel";

    private static final String FIELD_START = "bundlestart";

    private static final String FIELD_BUNDLEFILE = "bundlefile";

    // set to ask for PackageAdmin.refreshPackages() after install/update
    private static final String FIELD_REFRESH_PACKAGES = "refreshPackages";

    // bootdelegation property entries. wildcards are converted to package
    // name prefixes. whether an entry is a wildcard or not is set as a flag
    // in the bootPkgWildcards array.
    // see #activate and #isBootDelegated
    private String[] bootPkgs;

    // a flag for each entry in bootPkgs indicating whether the respective
    // entry was declared as a wildcard or not
    // see #activate and #isBootDelegated
    private boolean[] bootPkgWildcards;

    private ServiceRegistration configurationPrinter;
    private ServiceTracker bundleInfoTracker;

    // templates
    private final String TEMPLATE_MAIN;

    /** Default constructor */
    public BundlesServlet()
    {
        super(NAME, TITLE, CATEGORY_OSGI, CSS);

        // load templates
        TEMPLATE_MAIN = readTemplateFile( "/templates/bundles.html" );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#activate(org.osgi.framework.BundleContext)
     */
    public void activate( BundleContext bundleContext )
    {
        super.activate( bundleContext );

        bundleInfoTracker = new ServiceTracker( bundleContext, BundleInfoProvider.class.getName(), null);
        bundleInfoTracker.open();

        // bootdelegation property parsing from Apache Felix R4SearchPolicyCore
        String bootDelegation = bundleContext.getProperty( Constants.FRAMEWORK_BOOTDELEGATION );
        bootDelegation = ( bootDelegation == null ) ? "java.*" : bootDelegation + ",java.*";
        StringTokenizer st = new StringTokenizer( bootDelegation, " ," );
        bootPkgs = new String[st.countTokens()];
        bootPkgWildcards = new boolean[bootPkgs.length];
        for ( int i = 0; i < bootPkgs.length; i++ )
        {
            bootDelegation = st.nextToken();
            if ( bootDelegation.endsWith( "*" ) )
            {
                bootPkgWildcards[i] = true;
                bootDelegation = bootDelegation.substring( 0, bootDelegation.length() - 1 );
            }
            bootPkgs[i] = bootDelegation;
        }

        Hashtable props = new Hashtable();
        props.put( WebConsoleConstants.CONFIG_PRINTER_MODES, new String[] { ConfigurationPrinter.MODE_TXT,
                ConfigurationPrinter.MODE_ZIP } );
        configurationPrinter = bundleContext.registerService( ConfigurationPrinter.SERVICE, this, props );
    }


    /**
     * @see org.apache.felix.webconsole.SimpleWebConsolePlugin#deactivate()
     */
    public void deactivate()
    {
        if ( configurationPrinter != null )
        {
            configurationPrinter.unregister();
            configurationPrinter = null;
        }

        if ( bundleInfoTracker != null)
        {
            bundleInfoTracker.close();
            bundleInfoTracker = null;
        }

        super.deactivate();
    }


    //---------- ConfigurationPrinter

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration( PrintWriter pw )
    {
        try
        {
            final Map map = createObjectStructure(null, null, null, true, Locale.ENGLISH, null, null );

            pw.println( "Status: " + map.get( "status" ) );
            pw.println();

            Object[] data = (Object[]) map.get( "data" );
            for ( int i = 0; i < data.length; i++ )
            {
                Map bundle = (Map) data[i];

                pw.println( MessageFormat.format( "Bundle {0} - {1} {2} (state: {3})", new Object[]
                        { bundle.get( "id" ), bundle.get( "name" ), bundle.get( "version" ), bundle.get( "state" ) } ) );

                Object[] props = (Object[]) bundle.get( "props" );
                for ( int pi = 0; pi < props.length; pi++ )
                {
                    Map entry = (Map) props[pi];
                    String key = ( String ) entry.get( "key" );
                    if ( "nfo".equals( key ) )
                    {
                        // BundleInfo (see #bundleInfo & #bundleInfoDetails
                        Map infos = ( Map ) entry.get( "value" );
                        Iterator infoKeys = infos.keySet().iterator();
                        while ( infoKeys.hasNext() )
                        {
                            String infoKey = ( String ) infoKeys.next();
                            pw.println( "    " + infoKey + ": " );

                            Object[] infoA = (Object[]) infos.get(infoKey);
                            for ( int iai = 0; iai < infoA.length; iai++ )
                            {
                                if ( infoA[iai] != null )
                                {
                                    Map info = (Map) infoA[iai];
                                    pw.println( "        " + info.get( "name" ) );
                                }
                            }
                        }
                    }
                    else
                    {
                        // regular data
                        pw.print( "    " + key + ": " );

                        Object entryValue = entry.get( "value" );
                        if ( entryValue.getClass().isArray() )
                        {
                            pw.println();
                            for ( int ei = 0; ei < Array.getLength(entryValue); ei++ )
                            {
                                Object o = Array.get(entryValue, ei);
                                if ( o != null )
                                {
                                    pw.println( "        " + o );
                                }
                            }
                        }
                        else
                        {
                            pw.println( entryValue );
                        }
                    }
                }

                pw.println();
            }
        }
        catch ( Exception e )
        {
            log( "Problem rendering Bundle details for configuration status", e );
        }
    }


    //---------- BaseWebConsolePlugin

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
    IOException
    {
        final RequestInfo reqInfo = new RequestInfo(request);
        if ( "upload".equals(reqInfo.pathInfo) )
        {
            super.doGet(request, response);
            return;
        }
        if ( reqInfo.bundle == null && reqInfo.bundleRequested )
        {
            response.sendError(404);
            return;
        }
        if ( reqInfo.extension.equals("json")  )
        {
            final String pluginRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
            final String servicesRoot = getServicesRoot( request );
            try
            {
                this.renderJSON(response, reqInfo.bundle, pluginRoot, servicesRoot, request.getLocale(), request.getParameter(FILTER_PARAM), null );
            }
            catch (InvalidSyntaxException e)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid LDAP filter specified");
            }

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        boolean success = false;
        BundleException bundleException = null;
        final String action = WebConsoleUtil.getParameter( req, "action" );
        if ( "refreshPackages".equals( action ) )
        {
            // refresh packages and give it most 15 seconds to finish
            BaseUpdateInstallHelper.refreshPackages( getPackageAdmin(), getBundleContext(), 15000L, null );
            success = true;
        }
        else if ( "install".equals( action ) )
        {
            installBundles( req );

            if (req.getRequestURI().endsWith( "/install" )) {
                // just send 200/OK, no content
                resp.setContentLength( 0 );
            } else {
                // redirect to URL
                resp.sendRedirect( req.getRequestURI() );
            }

            return;
        }
        else
        {
            final RequestInfo reqInfo = new RequestInfo( req );
            if ( reqInfo.bundle == null && reqInfo.bundleRequested )
            {
                resp.sendError( 404 );
                return;
            }

            final Bundle bundle = reqInfo.bundle;
            if ( bundle != null )
            {
                if ( "start".equals( action ) )
                {
                    // start bundle
                    try
                    {
                        bundle.start();
                    }
                    catch ( BundleException be )
                    {
                        bundleException = be;
                        log( "Cannot start", be );
                    }
                }
                else if ( "stop".equals( action ) )
                {
                    // stop bundle
                    try
                    {
                        bundle.stop();
                    }
                    catch ( BundleException be )
                    {
                        bundleException = be;
                        log( "Cannot stop", be );
                    }
                }
                else if ( "refresh".equals( action ) )
                {
                    // refresh bundle wiring and give at most 5 seconds to finish
                    BaseUpdateInstallHelper.refreshPackages( getPackageAdmin(), getBundleContext(), 5000L, bundle );
                }
                else if ( "update".equals( action ) )
                {
                    // update the bundle
                    update( bundle );
                }
                else if ( "uninstall".equals( action ) )
                {
                    // uninstall bundle
                    try
                    {
                        bundle.uninstall();
                    }
                    catch ( BundleException be )
                    {
                        bundleException = be;
                        log( "Cannot uninstall", be );
                    }
                }

                // write the state only
                resp.setContentType( "application/json" ); //$NON-NLS-1$
                resp.setCharacterEncoding( "UTF-8" ); //$NON-NLS-1$
                if ( null == getBundleContext() )
                {
                    // refresh package on the web console itself or some of it's dependencies
                    resp.getWriter().print("false"); //$NON-NLS-1$
                }
                else
                {
                    resp.getWriter().print( "{\"fragment\":" + isFragmentBundle( bundle ) //$NON-NLS-1$
                    + ",\"stateRaw\":" + bundle.getState() + "}" ); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return;
            }
        }

        if ( success && null != getBundleContext() )
        {
            final String pluginRoot = ( String ) req.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
            final String servicesRoot = getServicesRoot( req );
            try
            {
                this.renderJSON( resp, null, pluginRoot, servicesRoot, req.getLocale(), req.getParameter(FILTER_PARAM), bundleException );
            }
            catch (InvalidSyntaxException e)
            {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid LDAP filter specified");
            }
        }
        else
        {
            super.doPost( req, resp );
        }
    }

    private String getServicesRoot(HttpServletRequest request)
    {
        return ( ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT ) ) +
                "/" + ServicesServlet.LABEL + "/";
    }

    Bundle getBundle( String pathInfo )
    {
        // only use last part of the pathInfo
        pathInfo = pathInfo.substring( pathInfo.lastIndexOf( '/' ) + 1 );

        // assume bundle Id
        try
        {
            final long bundleId = Long.parseLong( pathInfo );
            if ( bundleId >= 0 )
            {
                return BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getBundle( bundleId );
            }
        }
        catch ( NumberFormatException nfe )
        {
            // check if this follows the pattern {symbolic-name}[:{version}]
            final int pos = pathInfo.indexOf(':');
            final String symbolicName;
            final String version;
            if ( pos == -1 ) {
                symbolicName = pathInfo;
                version = null;
            } else {
                symbolicName = pathInfo.substring(0, pos);
                version = pathInfo.substring(pos+1);
            }

            // search
            final Bundle[] bundles = BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getBundles();
            for(int i=0; i<bundles.length; i++)
            {
                final Bundle bundle = bundles[i];
                // check symbolic name first
                if ( symbolicName.equals(bundle.getSymbolicName()) )
                {
                    if ( version == null || version.equals(bundle.getHeaders().get(Constants.BUNDLE_VERSION)) )
                    {
                        return bundle;
                    }
                }
            }
        }


        return null;
    }


    private void appendBundleInfoCount( final StringBuffer buf, String msg, int count )
    {
        buf.append(count);
        buf.append(" bundle");
        if ( count != 1 )
            buf.append( 's' );
        buf.append(' ');
        buf.append(msg);
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);

        final int startLevel = getStartLevel().getInitialBundleStartLevel();

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "startLevel", String.valueOf(startLevel));
        vars.put( "drawDetails", reqInfo.bundleRequested ? Boolean.TRUE : Boolean.FALSE );
        vars.put( "currentBundle", (reqInfo.bundleRequested && reqInfo.bundle != null ? String.valueOf(reqInfo.bundle.getBundleId()) : "null"));

        final String pluginRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
        final String servicesRoot = getServicesRoot ( request );
        StringWriter w = new StringWriter();
        try
        {
            writeJSON(w, reqInfo.bundle, pluginRoot, servicesRoot, request.getLocale(), request.getParameter(FILTER_PARAM), null );
        }
        catch (InvalidSyntaxException e)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid LDAP filter specified");
            return;
        }
        vars.put( "__bundles__", w.toString());

        response.getWriter().print(TEMPLATE_MAIN);
    }

    private void renderJSON( final HttpServletResponse response, final Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale, final String filter, final BundleException be )
            throws IOException, InvalidSyntaxException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON(pw, bundle, pluginRoot, servicesRoot, locale, filter, be);
    }


    private void writeJSON( final Writer pw, final Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale, final String filter, final BundleException be )
            throws IOException, InvalidSyntaxException
    {
        final Map map = createObjectStructure( bundle, pluginRoot, servicesRoot, false, locale, filter, be );
        final JSONWriter writer = new JSONWriter(pw);

        writer.value(map);
    }

    private Map createObjectStructure( final Bundle bundle, final String pluginRoot,
            final String servicesRoot, final boolean fullDetails, final Locale locale, final String filter, final BundleException be ) throws IOException, InvalidSyntaxException
    {
        final Bundle[] allBundles = this.getBundles();
        final Object[] status = getStatusLine(allBundles);
        final String statusLine = (String) status[5];
        // filter bundles by headers
        final Bundle[] bundles;
        if (bundle != null)
        {
            bundles = new Bundle[] { bundle };
        }
        else if (filter != null)
        {
            Filter f = getBundleContext().createFilter(filter);
            ArrayList list = new ArrayList(allBundles.length);
            final String localeString = locale.toString();
            for (int i = 0, size = allBundles.length; i < size; i++)
            {
                if (f.match(allBundles[i].getHeaders(localeString)))
                {
                    list.add(allBundles[i]);
                }
            }
            bundles = new Bundle[list.size()];
            list.toArray(bundles);
        }
        else
        {
            bundles = allBundles;
        }

        Util.sort( bundles, locale );

        final Map map = new LinkedHashMap();

        if (null != be)
        {
            final StringWriter s = new StringWriter();
            final Throwable t = be.getNestedException() != null ? be.getNestedException() : be;
            t.printStackTrace( new PrintWriter(s) );
            map.put("error", s.toString());
        }

        map.put("status", statusLine);

        // add raw status
        map.put( "s", status );

        final Object[] bundlesArray = new Object[bundles.length];
        for ( int i = 0; i < bundles.length; i++ )
        {
            bundlesArray[i] =
                    bundleInfo( bundles[i], fullDetails || bundle != null, pluginRoot, servicesRoot, locale );
        }

        map.put("data", bundlesArray);
        return map;
    }

    private Object[] getStatusLine(final Bundle[] bundles)
    {
        Object[] ret = new Object[6];
        int active = 0, installed = 0, resolved = 0, fragments = 0;
        for ( int i = 0; i < bundles.length; i++ )
        {
            switch ( bundles[i].getState() )
            {
            case Bundle.ACTIVE:
                active++;
                break;
            case Bundle.INSTALLED:
                installed++;
                break;
            case Bundle.RESOLVED:
                if ( isFragmentBundle( bundles[i] ) )
                {
                    fragments++;
                }
                else
                {
                    resolved++;
                }
                break;
            }
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Bundle information: ");
        appendBundleInfoCount(buffer, "in total", bundles.length);
        if ( active == bundles.length || active + fragments == bundles.length )
        {
            buffer.append(" - all ");
            appendBundleInfoCount(buffer, "active.", bundles.length);
        }
        else
        {
            if ( active != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "active", active);
            }
            if ( fragments != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "active fragments", fragments);
            }
            if ( resolved != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "resolved", resolved);
            }
            if ( installed != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "installed", installed);
            }
            buffer.append('.');
        }
        ret[0] = new Integer(bundles.length);
        ret[1] = new Integer(active);
        ret[2] = new Integer(fragments);
        ret[3] = new Integer(resolved);
        ret[4] = new Integer(installed);
        ret[5] = buffer.toString();
        return ret;
    }

    private Map bundleInfo( Bundle bundle, boolean details, final String pluginRoot, final String servicesRoot, final Locale locale )
    {
        final Map result = new LinkedHashMap();
        result.put("id", String.valueOf(bundle.getBundleId()) );
        result.put("name", Util.getName( bundle, locale ) );
        result.put("fragment", String.valueOf(isFragmentBundle(bundle)) );
        result.put("stateRaw", String.valueOf(bundle.getState() ) );
        result.put("state", toStateString( bundle ) );
        result.put("version", Util.getHeaderValue(bundle, Constants.BUNDLE_VERSION) );
        if ( bundle.getSymbolicName() != null )
        {
            result.put("symbolicName",  bundle.getSymbolicName() );
        }
        result.put("category",  Util.getHeaderValue(bundle, Constants.BUNDLE_CATEGORY) );

        if ( details )
        {
            bundleDetails( result, bundle, pluginRoot, servicesRoot, locale );
        }

        return result;
    }


    private final Bundle[] getBundles()
    {
        return BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getBundles();
    }


    private String toStateString( final Bundle bundle )
    {
        switch ( bundle.getState() )
        {
        case Bundle.INSTALLED:
            return "Installed";
        case Bundle.RESOLVED:
            if ( isFragmentBundle(bundle) )
            {
                return "Fragment";
            }
            return "Resolved";
        case Bundle.STARTING:
            return "Starting";
        case Bundle.ACTIVE:
            return "Active";
        case Bundle.STOPPING:
            return "Stopping";
        case Bundle.UNINSTALLED:
            return "Uninstalled";
        default:
            return "Unknown: " + bundle.getState();
        }
    }


    private final boolean isFragmentBundle( Bundle bundle )
    {
        // Workaround for FELIX-3670
        if ( bundle.getState() == Bundle.UNINSTALLED )
        {
            return bundle.getHeaders().get( Constants.FRAGMENT_HOST ) != null;
        }

        return getPackageAdmin().getBundleType( bundle ) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }

    private void keyVal(final List props, final String key, final Object val)
    {
        if ( val != null )
        {
            final Map obj = new LinkedHashMap();
            obj.put("key", key);
            obj.put("value", val);
            props.add(obj);
        }
    }
    private final void bundleDetails( final Map result, Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale)
    {
        Dictionary headers = bundle.getHeaders( locale == null ? null : locale.toString() );

        final List props = new ArrayList();

        keyVal( props, "Symbolic Name", bundle.getSymbolicName() );
        keyVal( props, "Version", headers.get( Constants.BUNDLE_VERSION ) );
        keyVal( props, "Bundle Location", bundle.getLocation() );
        keyVal( props, "Last Modification", new Date( bundle.getLastModified() ) );

        String docUrl = ( String ) headers.get( Constants.BUNDLE_DOCURL );
        if ( docUrl != null )
        {
            keyVal( props, "Bundle Documentation", docUrl );
        }

        keyVal( props, "Vendor", headers.get( Constants.BUNDLE_VENDOR ) );
        keyVal( props, "Copyright", headers.get( Constants.BUNDLE_COPYRIGHT ) );
        keyVal( props, "Description", headers.get( Constants.BUNDLE_DESCRIPTION ) );

        keyVal( props, "Start Level", getStartLevel( bundle ) );

        keyVal( props, "Bundle Classpath", headers.get( Constants.BUNDLE_CLASSPATH ) );

        listFragmentInfo( props, bundle, pluginRoot );

        if ( bundle.getState() == Bundle.INSTALLED )
        {
            listImportExportsUnresolved( props, bundle, pluginRoot );
        }
        else
        {
            listImportExport( props, bundle, pluginRoot );
        }

        if ( bundle.getState() != Bundle.UNINSTALLED )
        {
            listServices( props, bundle, servicesRoot );
        }

        listHeaders( props, bundle );
        final String appRoot = ( pluginRoot == null ) ? "" : pluginRoot.substring( 0, pluginRoot.lastIndexOf( "/" ) );
        bundleInfoDetails( props, bundle, appRoot, locale );

        result.put( "props", props.toArray(new Object[props.size()]));
    }


    private final void bundleInfoDetails( List props, Bundle bundle, String appRoot, final Locale locale)
    {
        final Map val = new LinkedHashMap();
        val.put("key", "nfo");
        final Map value = new LinkedHashMap();
        final Object[] bundleInfoProviders = bundleInfoTracker.getServices();
        for ( int i = 0; bundleInfoProviders != null && i < bundleInfoProviders.length; i++ )
        {
            final BundleInfoProvider infoProvider = (BundleInfoProvider) bundleInfoProviders[i];
            final BundleInfo[] infos = infoProvider.getBundleInfo(bundle, appRoot, locale);
            if ( null != infos && infos.length > 0)
            {
                final Object[] infoArray = new Object[infos.length];
                for ( int j = 0; j < infos.length; j++ )
                {
                    infoArray[j] = bundleInfo( infos[j] );
                }
                value.put(infoProvider.getName(locale), infoArray);
            }
        }
        val.put("value", value);
        props.add(val);
    }


    private static final Object bundleInfo( BundleInfo info )
    {
        final Map val = new LinkedHashMap();
        val.put( "name", info.getName() );
        val.put( "description", info.getDescription() );
        val.put( "type", info.getType().getName() );
        val.put( "value", info.getValue() );
        return val;
    }


    private final Integer getStartLevel( Bundle bundle )
    {
        if ( bundle.getState() != Bundle.UNINSTALLED )
        {
            StartLevel sl = getStartLevel();
            if ( sl != null )
            {
                return new Integer( sl.getBundleStartLevel( bundle ) );
            }
        }

        // bundle has been uninstalled or StartLevel service is not available
        return null;
    }


    private void listImportExport( List props, Bundle bundle, final String pluginRoot )
    {
        PackageAdmin packageAdmin = getPackageAdmin();
        if ( packageAdmin == null )
        {
            return;
        }

        Map usingBundles = new TreeMap();

        ExportedPackage[] exports = packageAdmin.getExportedPackages( bundle );
        if ( exports != null && exports.length > 0 )
        {
            // do alphabetical sort
            Arrays.sort( exports, new Comparator()
            {
                public int compare( ExportedPackage p1, ExportedPackage p2 )
                {
                    return p1.getName().compareTo( p2.getName() );
                }


                public int compare( Object o1, Object o2 )
                {
                    return compare( ( ExportedPackage ) o1, ( ExportedPackage ) o2 );
                }
            } );

            Object[] val = new Object[exports.length];
            for ( int j = 0; j < exports.length; j++ )
            {
                ExportedPackage export = exports[j];
                val[j] = collectExport( export.getName(), export.getVersion() );
                Bundle[] ubList = export.getImportingBundles();
                if ( ubList != null )
                {
                    for ( int i = 0; i < ubList.length; i++ )
                    {
                        Bundle ub = ubList[i];
                        String name = ub.getSymbolicName();
                        if (name == null) name = ub.getLocation();
                        usingBundles.put( name, ub );
                    }
                }
            }
            keyVal( props, "Exported Packages", val );
        }
        else
        {
            keyVal( props, "Exported Packages", "---" );
        }

        exports = packageAdmin.getExportedPackages( ( Bundle ) null );
        if ( exports != null && exports.length > 0 )
        {
            // collect import packages first
            final List imports = new ArrayList();
            for ( int i = 0; i < exports.length; i++ )
            {
                final ExportedPackage ep = exports[i];
                final Bundle[] importers = ep.getImportingBundles();
                for ( int j = 0; importers != null && j < importers.length; j++ )
                {
                    if ( importers[j].getBundleId() == bundle.getBundleId() )
                    {
                        imports.add( ep );

                        break;
                    }
                }
            }
            // now sort
            Object[] val;
            if ( imports.size() > 0 )
            {
                final ExportedPackage[] packages = ( ExportedPackage[] ) imports.toArray( new ExportedPackage[imports
                                                                                                              .size()] );
                Arrays.sort( packages, new Comparator()
                {
                    public int compare( ExportedPackage p1, ExportedPackage p2 )
                    {
                        return p1.getName().compareTo( p2.getName() );
                    }


                    public int compare( Object o1, Object o2 )
                    {
                        return compare( ( ExportedPackage ) o1, ( ExportedPackage ) o2 );
                    }
                } );
                // and finally print out
                val = new Object[packages.length];
                for ( int i = 0; i < packages.length; i++ )
                {
                    ExportedPackage ep = packages[i];
                    val[i] = collectImport( ep.getName(), ep.getVersion(), false, ep, pluginRoot );
                }
            }
            else
            {
                // add description if there are no imports
                val = new Object[1];
                val[0] =  "None";
            }

            keyVal( props, "Imported Packages", val );
        }

        if ( !usingBundles.isEmpty() )
        {
            Object[] val = new Object[usingBundles.size()];
            int index = 0;
            for ( Iterator ui = usingBundles.values().iterator(); ui.hasNext(); )
            {
                Bundle usingBundle = ( Bundle ) ui.next();
                val[index] = getBundleDescriptor( usingBundle, pluginRoot );
                index++;
            }
            keyVal( props, "Importing Bundles", val );
        }
    }


    private void listImportExportsUnresolved( final List props, Bundle bundle, final String pluginRoot )
    {
        Dictionary dict = bundle.getHeaders();

        String target = ( String ) dict.get( Constants.EXPORT_PACKAGE );
        if ( target != null )
        {
            Clause[] pkgs = Parser.parseHeader( target );
            if ( pkgs != null && pkgs.length > 0 )
            {
                // do alphabetical sort
                Arrays.sort( pkgs, new Comparator()
                {
                    public int compare( Clause p1, Clause p2 )
                    {
                        return p1.getName().compareTo( p2.getName() );
                    }


                    public int compare( Object o1, Object o2 )
                    {
                        return compare( ( Clause) o1, ( Clause ) o2 );
                    }
                } );

                Object[] val = new Object[pkgs.length];
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    Clause export = new Clause( pkgs[i].getName(), pkgs[i].getDirectives(), pkgs[i].getAttributes() );
                    val[i] = collectExport( export.getName(), export.getAttribute( Constants.VERSION_ATTRIBUTE ) );
                }
                keyVal( props, "Exported Packages", val );
            }
            else
            {
                keyVal( props, "Exported Packages", "---" );
            }
        }

        target = ( String ) dict.get( Constants.IMPORT_PACKAGE );
        if ( target != null )
        {
            Clause[] pkgs = Parser.parseHeader( target );
            if ( pkgs != null && pkgs.length > 0 )
            {
                Map imports = new TreeMap();
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    Clause pkg = pkgs[i];
                    imports.put( pkg.getName(), new Clause( pkg.getName(), pkg.getDirectives(), pkg.getAttributes() ) );
                }

                // collect import packages first
                final Map candidates = new HashMap();
                PackageAdmin packageAdmin = getPackageAdmin();
                if ( packageAdmin != null )
                {
                    ExportedPackage[] exports = packageAdmin.getExportedPackages( ( Bundle ) null );
                    if ( exports != null && exports.length > 0 )
                    {

                        for ( int i = 0; i < exports.length; i++ )
                        {
                            final ExportedPackage ep = exports[i];

                            Clause imp = ( Clause ) imports.get( ep.getName() );
                            if ( imp != null && isSatisfied( imp, ep ) )
                            {
                                candidates.put( ep.getName(), ep );
                            }
                        }
                    }
                }

                // now sort
                Object[] val;
                if ( imports.size() > 0 )
                {
                    final List importList = new ArrayList();
                    for ( Iterator ii = imports.values().iterator(); ii.hasNext(); )
                    {
                        Clause r4Import = ( Clause ) ii.next();
                        ExportedPackage ep = ( ExportedPackage ) candidates.get( r4Import.getName() );

                        // if there is no matching export, check whether this
                        // bundle has the package, ignore the entry in this case
                        if ( ep == null )
                        {
                            String path = r4Import.getName().replace( '.', '/' );
                            if ( bundle.getEntry( path ) != null )
                            {
                                continue;
                            }
                        }

                        importList.add(collectImport(  r4Import.getName(), r4Import.getAttribute( Constants.VERSION_ATTRIBUTE ),
                                Constants.RESOLUTION_OPTIONAL.equals( r4Import
                                        .getDirective( Constants.RESOLUTION_DIRECTIVE ) ), ep, pluginRoot ));
                    }
                    val = importList.toArray(new Object[importList.size()]);
                }
                else
                {
                    // add description if there are no imports
                    val = new Object[1];
                    val[0] = "---" ;
                }

                keyVal( props, "Imported Packages", val );
            }
        }
    }

    private String getServiceID(ServiceReference ref, final String servicesRoot)
    {
        String id = ref.getProperty( Constants.SERVICE_ID ).toString();
        StringBuffer val = new StringBuffer();

        if ( servicesRoot != null )
        {
            val.append( "<a href='" ).append( servicesRoot ).append( id ).append( "'>" );
            val.append( id );
            val.append( "</a>" );
            return val.toString();
        }

        return id;
    }


    private void listServices( List props, Bundle bundle, final String servicesRoot )
    {
        ServiceReference[] refs = bundle.getRegisteredServices();
        if ( refs == null || refs.length == 0 )
        {
            return;
        }

        for ( int i = 0; i < refs.length; i++ )
        {


            String key = "Service ID " + getServiceID( refs[i], servicesRoot );

            List val = new ArrayList();

            appendProperty( val, refs[i], Constants.OBJECTCLASS, "Types" );
            appendProperty( val, refs[i], Constants.SERVICE_PID, "Service PID" );
            appendProperty( val, refs[i], "org.apache.felix.karaf.features.configKey", "Feature PID" );
            appendProperty( val, refs[i], ConfigurationAdmin.SERVICE_FACTORYPID, "Factory PID" );
            appendProperty( val, refs[i], "component.name", "Component Name" );
            appendProperty( val, refs[i], "component.id", "Component ID" );
            appendProperty( val, refs[i], "component.factory", "Component Factory" );
            appendProperty( val, refs[i], Constants.SERVICE_DESCRIPTION, "Description" );
            appendProperty( val, refs[i], Constants.SERVICE_VENDOR, "Vendor" );

            keyVal( props, key, val.toArray(new Object[val.size()]));
        }
    }


    private void listHeaders( List props, Bundle bundle )
    {
        List val = new ArrayList();

        Dictionary headers = bundle.getHeaders(""); // don't localize at all - raw headers
        Enumeration he = headers.keys();
        while ( he.hasMoreElements() )
        {
            Object header = he.nextElement();
            String value = String.valueOf(headers.get( header ));
            // Package headers may be long, support line breaking by
            // ensuring blanks after comma and semicolon.
            value = enableLineWrapping(value);
            val.add( header + ": " + value );
        }

        keyVal( props, "Manifest Headers", val.toArray(new Object[val.size()]) );
    }

    private static final String enableLineWrapping(final String value)
    {
        StringBuffer sb = new StringBuffer(value.length() * 2 / 3);
        synchronized (sb)
        { // faster
            for (int i = 0; i < value.length(); i++)
            {
                final char ch = value.charAt( i );
                sb.append( ch );
                if ( ch == ';' || ch == ',' )
                {
                    sb.append( ' ' );
                }
            }
            return sb.toString();
        }
    }

    private void listFragmentInfo( final List props, final Bundle bundle, final String pluginRoot )
    {

        if ( isFragmentBundle( bundle ) )
        {
            Bundle[] hostBundles = getPackageAdmin().getHosts( bundle );
            if ( hostBundles != null )
            {
                final Object[] val = new Object[hostBundles.length];
                for ( int i = 0; i < hostBundles.length; i++ )
                {
                    val[i] = getBundleDescriptor( hostBundles[i], pluginRoot );
                }
                keyVal( props, "Host Bundles", val );
            }
        }
        else
        {
            Bundle[] fragmentBundles = getPackageAdmin().getFragments( bundle );
            if ( fragmentBundles != null )
            {
                final Object[] val = new Object[fragmentBundles.length];
                for ( int i = 0; i < fragmentBundles.length; i++ )
                {
                    val[i] = getBundleDescriptor( fragmentBundles[i], pluginRoot );
                }
                keyVal( props, "Fragments Attached", val );
            }
        }

    }


    private void appendProperty( final List props, ServiceReference ref, String name, String label )
    {
        StringBuffer dest = new StringBuffer();
        Object value = ref.getProperty( name );
        if ( value instanceof Object[] )
        {
            Object[] values = ( Object[] ) value;
            dest.append( label ).append( ": " );
            for ( int j = 0; j < values.length; j++ )
            {
                if ( j > 0 )
                    dest.append( ", " );
                dest.append( values[j] );
            }
            props.add(dest.toString());
        }
        else if ( value != null )
        {
            dest.append( label ).append( ": " ).append( value );
            props.add(dest.toString());
        }
    }


    private Object collectExport( String name, Version version )
    {
        return collectExport( name, ( version == null ) ? null : version.toString() );
    }


    private Object collectExport( String name, String version )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );
        if ( bootDel )
        {
            val.append( "!! " );
        }

        val.append( name );

        if ( version != null )
        {
            val.append( ",version=" ).append( version );
        }

        if ( bootDel )
        {
            val.append( " -- Overwritten by Boot Delegation" );
        }

        return val.toString();
    }


    private Object collectImport(String name, Version version, boolean optional,
            ExportedPackage export, final String pluginRoot )
    {
        return collectImport( name, ( version == null ) ? null : version.toString(), optional, export, pluginRoot );
    }


    private Object collectImport( String name, String version, boolean optional, ExportedPackage export,
            final String pluginRoot )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );

        String marker = null;
        val.append( name );

        if ( version != null )
        {
            val.append( ",version=" ).append( version );
        }

        if ( export != null )
        {
            val.append( " from " );
            val.append( getBundleDescriptor( export.getExportingBundle(), pluginRoot ) );

            if ( bootDel )
            {
                val.append( " -- Overwritten by Boot Delegation" );
                marker = "INFO";
            }
        }
        else
        {
            val.append( " -- Cannot be resolved" );
            marker = "ERROR";

            if ( optional )
            {
                val.append( " but is not required" );
            }

            if ( bootDel )
            {
                val.append( " and overwritten by Boot Delegation" );
            }
        }

        if ( marker != null ) {
            val.insert(0, ": ");
            val.insert(0, marker);
        }

        return val;
    }


    // returns true if the package is listed in the bootdelegation property
    private boolean isBootDelegated( String pkgName )
    {

        // bootdelegation analysis from Apache Felix R4SearchPolicyCore

        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if ( pkgName.length() > 0 )
        {

            // Delegate any packages listed in the boot delegation
            // property to the parent class loader.
            for ( int i = 0; i < bootPkgs.length; i++ )
            {

                // A wildcarded boot delegation package will be in the form of
                // "foo.", so if the package is wildcarded do a startsWith() or
                // a regionMatches() to ignore the trailing "." to determine if
                // the request should be delegated to the parent class loader.
                // If the package is not wildcarded, then simply do an equals()
                // test to see if the request should be delegated to the parent
                // class loader.
                if ( ( bootPkgWildcards[i] && ( pkgName.startsWith( bootPkgs[i] ) || bootPkgs[i].regionMatches( 0,
                        pkgName, 0, pkgName.length() ) ) )
                        || ( !bootPkgWildcards[i] && bootPkgs[i].equals( pkgName ) ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    private boolean isSatisfied( Clause imported, ExportedPackage exported )
    {
        if ( imported.getName().equals( exported.getName() ) )
        {
            String versionAttr = imported.getAttribute( Constants.VERSION_ATTRIBUTE );
            if ( versionAttr == null )
            {
                // no specific version required, this export surely satisfies it
                return true;
            }

            VersionRange required = VersionRange.parse( versionAttr );
            return required.isInRange( exported.getVersion() );
        }

        // no this export does not satisfy the import
        return false;
    }


    private String getBundleDescriptor( Bundle bundle, final String pluginRoot )
    {
        StringBuffer val = new StringBuffer();

        if ( pluginRoot != null )
        {
            val.append( "<a href='" ).append( pluginRoot ).append( '/' ).append( bundle.getBundleId() ).append( "'>" );
        }

        if ( bundle.getSymbolicName() != null )
        {
            // list the bundle name if not null
            val.append( bundle.getSymbolicName() );
            val.append( " (" ).append( bundle.getBundleId() );
            val.append( ")" );
        }
        else if ( bundle.getLocation() != null )
        {
            // otherwise try the location
            val.append( bundle.getLocation() );
            val.append( " (" ).append( bundle.getBundleId() );
            val.append( ")" );
        }
        else
        {
            // fallback to just the bundle id
            // only append the bundle
            val.append( bundle.getBundleId() );
        }
        if ( pluginRoot != null )
        {
            val.append( "</a>" );
        }
        return val.toString();
    }

    private void update( final Bundle bundle )
    {
        UpdateHelper t = new UpdateHelper( this, bundle, false );
        t.start();
    }

    private final class RequestInfo
    {
        public final String extension;
        public final Bundle bundle;
        public final boolean bundleRequested;
        public final String pathInfo;

        protected RequestInfo( final HttpServletRequest request )
        {
            String info = request.getPathInfo();
            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);

            // get extension
            if ( info.endsWith(".json") )
            {
                extension = "json";
                info = info.substring(0, info.length() - 5);
            }
            else
            {
                extension = "html";
            }

            // we only accept direct requests to a bundle if they have a slash after the label
            String bundleInfo = null;
            if (info.startsWith("/") )
            {
                bundleInfo = info.substring(1);
            }
            if ( bundleInfo == null || bundleInfo.length() == 0 )
            {
                bundle = null;
                bundleRequested = false;
                pathInfo = null;
            }
            else
            {
                bundle = getBundle(bundleInfo);
                bundleRequested = true;
                pathInfo = bundleInfo;
            }
            request.setAttribute(BundlesServlet.class.getName(), this);
        }

    }

    static final RequestInfo getRequestInfo(final HttpServletRequest request)
    {
        return (RequestInfo)request.getAttribute( BundlesServlet.class.getName() );
    }

    private final PackageAdmin getPackageAdmin()
    {
        return ( PackageAdmin ) getService( PackageAdmin.class.getName() );
    }

    private final StartLevel getStartLevel()
    {
        return ( StartLevel ) getService( StartLevel.class.getName() );
    }


    //---------- Bundle Installation handler (former InstallAction)

    private void installBundles( HttpServletRequest request ) throws IOException
    {

        // get the uploaded data
        final Map params = ( Map ) request.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
        if ( params == null )
        {
            return;
        }

        final FileItem startItem = getParameter( params, FIELD_START );
        final FileItem startLevelItem = getParameter( params, FIELD_STARTLEVEL );
        final FileItem[] bundleItems = getFileItems( params, FIELD_BUNDLEFILE );
        final FileItem refreshPackagesItem = getParameter( params, FIELD_REFRESH_PACKAGES );

        // don't care any more if no bundle item
        if ( bundleItems.length == 0 )
        {
            return;
        }

        // default values
        // it exists
        int startLevel = -1;
        String bundleLocation = "inputstream:";

        // convert the start level value
        if ( startLevelItem != null )
        {
            try
            {
                startLevel = Integer.parseInt( startLevelItem.getString() );
            }
            catch ( NumberFormatException nfe )
            {
                log( LogService.LOG_INFO, "Cannot parse start level parameter " + startLevelItem
                        + " to a number, not setting start level" );
            }
        }

        for ( int i = 0; i < bundleItems.length; i++ )
        {
            final FileItem bundleItem = bundleItems[i];
            // write the bundle data to a temporary file to ease processing
            File tmpFile = null;
            try
            {
                // copy the data to a file for better processing
                tmpFile = File.createTempFile( "install", ".tmp" );
                bundleItem.write( tmpFile );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, "Problem accessing uploaded bundle file: " + bundleItem.getName(), e );

                // remove the tmporary file
                if ( tmpFile != null )
                {
                    tmpFile.delete();
                    tmpFile = null;
                }
            }

            // install or update the bundle now
            if ( tmpFile != null )
            {
                // start, refreshPackages just needs to exist, don't care for value
                final boolean start = startItem != null;
                final boolean refreshPackages = refreshPackagesItem != null;

                bundleLocation = "inputstream:" + bundleItem.getName();
                installBundle( bundleLocation, tmpFile, startLevel, start, refreshPackages );
            }
        }
    }


    private FileItem getParameter( Map params, String name )
    {
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( items[i].isFormField() )
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }


    private FileItem[] getFileItems( Map params, String name )
    {
        final List files = new ArrayList();
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( !items[i].isFormField() && items[i].getSize() > 0 )
                {
                    files.add( items[i] );
                }
            }
        }

        return ( FileItem[] ) files.toArray( new FileItem[files.size()] );
    }


    private void installBundle( String location, File bundleFile, int startLevel, boolean start, boolean refreshPackages )
            throws IOException
    {
        if ( bundleFile != null )
        {

            // try to get the bundle name, fail if none
            String symbolicName = getSymbolicName( bundleFile );
            if ( symbolicName == null )
            {
                bundleFile.delete();
                throw new IOException( Constants.BUNDLE_SYMBOLICNAME + " header missing, cannot install bundle" );
            }

            // check for existing bundle first
            Bundle updateBundle = null;
            if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals( symbolicName ) )
            {
                updateBundle = getBundleContext().getBundle( 0 );
            }
            else
            {
                Bundle[] bundles = BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getBundles();
                for ( int i = 0; i < bundles.length; i++ )
                {
                    if ( ( bundles[i].getLocation() != null && bundles[i].getLocation().equals( location ) )
                            || ( bundles[i].getSymbolicName() != null && bundles[i].getSymbolicName().equals( symbolicName ) ) )
                    {
                        updateBundle = bundles[i];
                        break;
                    }
                }
            }

            if ( updateBundle != null )
            {

                updateBackground( updateBundle, bundleFile, refreshPackages );

            }
            else
            {

                installBackground( bundleFile, location, startLevel, start, refreshPackages );

            }
        }
    }


    private String getSymbolicName( File bundleFile )
    {
        JarFile jar = null;
        try
        {
            jar = new JarFile( bundleFile );
            Manifest m = jar.getManifest();
            if ( m != null )
            {
                String sn = m.getMainAttributes().getValue( Constants.BUNDLE_SYMBOLICNAME );
                if ( sn != null )
                {
                    final int paramPos = sn.indexOf(';');
                    if ( paramPos != -1 )
                    {
                        sn = sn.substring(0, paramPos);
                    }
                }
                return sn;
            }
        }
        catch ( IOException ioe )
        {
            log( LogService.LOG_WARNING, "Cannot extract symbolic name of bundle file " + bundleFile, ioe );
        }
        finally
        {
            if ( jar != null )
            {
                try
                {
                    jar.close();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }
        }

        // fall back to "not found"
        return null;
    }


    private void installBackground( final File bundleFile, final String location, final int startlevel,
            final boolean doStart, final boolean refreshPackages )
    {

        InstallHelper t = new InstallHelper( this, getBundleContext(), bundleFile, location, startlevel, doStart,
                refreshPackages );
        t.start();
    }


    private void updateBackground( final Bundle bundle, final File bundleFile, final boolean refreshPackages )
    {
        UpdateHelper t = new UpdateHelper( this, bundle, bundleFile, refreshPackages );
        t.start();
    }
}
