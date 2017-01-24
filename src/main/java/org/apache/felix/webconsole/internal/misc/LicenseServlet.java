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
package org.apache.felix.webconsole.internal.misc;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.json.JSONWriter;
import org.osgi.framework.Bundle;


/**
 * LicenseServlet provides the licenses plugin that browses through the bundles,
 * searching for common license files.
 *
 */
public final class LicenseServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{
    public static final class Entry {
        String url;
        String path;
        String jar;
    }

    // common names (without extension) of the license files.
    static final String LICENSE_FILES[] =
        { "README", "DISCLAIMER", "LICENSE", "NOTICE", "DEPENDENCIES" };

    static final String LABEL = "licenses";
    static final String TITLE = "%licenses.pluginTitle";
    static final String CSS[] = { "/res/ui/license.css" };

    // templates
    private final String TEMPLATE;

    /**
     * Default constructor
     */
    public LicenseServlet()
    {
        super(LABEL, TITLE, CATEGORY_OSGI_MANAGER, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/license.html" );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        final PathInfo pathInfo = PathInfo.parse( request.getPathInfo() );
        if ( pathInfo != null )
        {
            if ( !sendResource( pathInfo, response ) )
            {
                response.sendError( HttpServletResponse.SC_NOT_FOUND, "Cannot send data .." );
            }
        }
        else
        {
            super.doGet( request, response );
        }
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse res ) throws IOException
    {
        Bundle[] bundles = getBundleContext().getBundles();
        Util.sort( bundles, request.getLocale() );

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", getBundleData( bundles, request.getLocale() ));

        res.getWriter().print(TEMPLATE);
    }

    private static final String getBundleData(Bundle[] bundles, Locale locale) throws IOException
    {
        final StringWriter json = new StringWriter();
        final JSONWriter jw = new JSONWriter(json);
        jw.array();

        for (int i = 0; i < bundles.length; i++)
        {
            Bundle bundle = bundles[i];

            List files = findResource(bundle, LICENSE_FILES);
            addLicensesFromHeader(bundle, files);
            if (!files.isEmpty())
            { // has resources
                jw.object();
                jw.key( "bid").value( bundle.getBundleId() );
                jw.key( "title").value( Util.getName( bundle, locale ) );
                jw.key( "files");
                jw.object();
                jw.key("__res__");
                jw.array();
                Iterator iter = files.iterator();
                while ( iter.hasNext() ) {
                    jw.object();
                    Entry entry = (Entry) iter.next();
                    jw.key("path").value(entry.path);
                    jw.key("url").value(entry.url);
                    if ( entry.jar != null )
                    {
                        jw.key("jar").value(entry.jar);
                    }
                    jw.endObject();
                }
                jw.endArray();
                jw.endObject();
                jw.endObject();
            }
        }

        jw.endArray();
        return json.toString();
    }


    private static final String getName( String path )
    {
        return path.substring( path.lastIndexOf( '/' ) + 1 );
    }

    private static final void addLicensesFromHeader(Bundle bundle, List files)
    {
        String target = (String) bundle.getHeaders("").get("Bundle-License");
        if (target != null)
        {
            Clause[] licenses = Parser.parseHeader(target);
            for (int i = 0; licenses != null && i < licenses.length; i++)
            {
                final String name = licenses[i].getName();
                if (!"<<EXTERNAL>>".equals(name))
                {
                    final String link = licenses[i].getAttribute("link");
                    final String path;
                    final String url;
                    if (link == null)
                    {
                        path = name;
                        url = getName(name);
                    }
                    else
                    {
                        path = link;
                        url = name;
                    }

                    // skip entry URL is bundle resources, but doesn't exists
                    if (path.indexOf("://") == -1 && null == bundle.getEntry(path))
                        continue;

                    Entry entry = new Entry();
                    entry.path = path;
                    entry.url = url;

                    files.add(entry);
                }
            }
        }
    }

    private static final List findResource( Bundle bundle, String[] patterns ) throws IOException
    {
        final List files = new ArrayList();

        for ( int i = 0; i < patterns.length; i++ )
        {
            Enumeration entries = bundle.findEntries( "/", patterns[i] + "*", true );
            if ( entries != null )
            {
                while ( entries.hasMoreElements() )
                {
                    URL url = ( URL ) entries.nextElement();
                    Entry entry = new Entry();
                    entry.path = url.getPath();
                    entry.url = getName( url.getPath() ) ;
                    files.add(entry);
                }
            }
        }

        Enumeration entries = bundle.findEntries( "/", "*.jar", true );
        if ( entries != null )
        {
            while ( entries.hasMoreElements() )
            {
                URL url = ( URL ) entries.nextElement();

                InputStream ins = null;
                try
                {
                    ins = url.openStream();
                    ZipInputStream zin = new ZipInputStream( ins );
                    for ( ZipEntry zentry = zin.getNextEntry(); zentry != null; zentry = zin.getNextEntry() )
                    {
                        String name = zentry.getName();

                        // ignore directory entries
                        if ( name.endsWith( "/" ) )
                        {
                            continue;
                        }

                        // cut off path and use file name for checking against patterns
                        name = name.substring( name.lastIndexOf( '/' ) + 1 );
                        for ( int i = 0; i < patterns.length; i++ )
                        {
                            if ( name.startsWith( patterns[i] ) )
                            {
                                Entry entry = new Entry();
                                entry.path = zentry.getName();
                                entry.url = getName( name ) ;
                                entry.jar = url.getPath();
                                files.add(entry);
                            }
                        }
                    }
                }
                finally
                {
                    IOUtils.closeQuietly( ins );
                }

            }
        }

        return files;
    }


    private boolean sendResource( final PathInfo pathInfo, final HttpServletResponse response ) throws IOException
    {

        final String name = pathInfo.licenseFile.substring( pathInfo.licenseFile.lastIndexOf( '/' ) + 1 );
        boolean isLicense = false;
        for ( int i = 0; !isLicense && i < LICENSE_FILES.length; i++ )
        {
            isLicense = name.startsWith( LICENSE_FILES[i] );
        }

        final Bundle bundle = getBundleContext().getBundle( pathInfo.bundleId );
        if ( bundle == null )
        {
            return false;
        }

        // prepare the response
        WebConsoleUtil.setNoCache( response );
        response.setContentType( "text/plain" );

        if ( pathInfo.innerJar == null )
        {
            URL resource = bundle.getEntry( pathInfo.licenseFile );
            if ( resource == null)
            {
                resource = bundle.getResource( pathInfo.licenseFile );
            }


            if ( resource != null )
            {
                final InputStream input = resource.openStream();
                try
                {
                    IOUtils.copy( input, response.getWriter() );
                    return true;
                }
                finally
                {
                    IOUtils.closeQuietly( input );
                }
            }
        }
        else
        {
            // license is in a nested JAR
            final URL zipResource = bundle.getResource( pathInfo.innerJar );
            if ( zipResource != null )
            {
                final InputStream input = zipResource.openStream();
                ZipInputStream zin = null;
                try
                {
                    zin = new ZipInputStream( input );
                    for ( ZipEntry zentry = zin.getNextEntry(); zentry != null; zentry = zin.getNextEntry() )
                    {
                        if ( pathInfo.licenseFile.equals( zentry.getName() ) )
                        {
                            IOUtils.copy( zin, response.getWriter() );
                            return true;
                        }
                    }
                }
                finally
                {

                    IOUtils.closeQuietly( zin );
                    IOUtils.closeQuietly( input );
                }
            }
        }

        // throw new ServletException("License file:" + url + " not found!");
        return false;
    }

    // package private for unit testing of the parse method
    static class PathInfo
    {
        final long bundleId;
        final String innerJar;
        final String licenseFile;


        static PathInfo parse( final String pathInfo )
        {
            if ( pathInfo == null || pathInfo.length() == 0 || !pathInfo.startsWith( "/" + LABEL + "/" ) )
            {
                return null;
            }

            // cut off label prefix including slashes around the label
            final String parts = pathInfo.substring( LABEL.length() + 2 );

            int slash = parts.indexOf( '/' );
            if ( slash <= 0 )
            {
                return null;
            }

            final long bundleId;
            try
            {
                bundleId = Long.parseLong( parts.substring( 0, slash ) );
                if ( bundleId < 0 )
                {
                    return null;
                }
            }
            catch ( NumberFormatException nfe )
            {
                // illegal bundle id
                return null;
            }

            final String innerJar;
            int jarSep = parts.indexOf( "!/", slash );
            if ( jarSep < 0 )
            {
                innerJar = null;
            }
            else
            {
                innerJar = parts.substring( slash, jarSep );
                slash = jarSep + 2; // ignore bang-slash
            }

            final String licenseFile = parts.substring( slash );

            return new PathInfo( bundleId, innerJar, licenseFile );
        }


        private PathInfo( final long bundleId, final String innerJar, final String licenseFile )
        {
            this.bundleId = bundleId;
            this.innerJar = innerJar;
            this.licenseFile = licenseFile;
        }
    }
}
