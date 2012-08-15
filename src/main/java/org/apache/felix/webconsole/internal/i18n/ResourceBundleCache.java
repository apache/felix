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
package org.apache.felix.webconsole.internal.i18n;


import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;


/**
 * The <code>ResourceBundleCache</code> caches resource bundles per OSGi bundle.
 */
public class ResourceBundleCache
{

    /**
     * The default locale corresponding to the default language in the
     * bundle.properties file, which is English.
     * (FELIX-1957 The Locale(String) constructor used before is not available
     * in the OSGi/Minimum-1.1 profile and should be prevented)
     */
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final Bundle bundle;

    private final Map resourceBundles;

    private Map resourceBundleEntries;


    /**
     * Creates a new object
     * 
     * @param bundle the bundle which resources should be loaded.
     */
    public ResourceBundleCache( final Bundle bundle )
    {
        this.bundle = bundle;
        this.resourceBundles = new HashMap();
    }


    /**
     * Gets the resource bundle for the specified locale.
     * 
     * @param locale the requested locale
     * @return the resource bundle for the requested locale
     */
    public ResourceBundle getResourceBundle( final Locale locale )
    {
        if ( locale == null )
        {
            return getResourceBundleInternal( DEFAULT_LOCALE );
        }

        return getResourceBundleInternal( locale );
    }


    ResourceBundle getResourceBundleInternal( final Locale locale )
    {
        if ( locale == null )
        {
            return null;
        }

        synchronized ( resourceBundles )
        {
            ResourceBundle bundle = ( ResourceBundle ) resourceBundles.get( locale );
            if ( bundle != null )
            {
                return bundle;
            }
        }

        ResourceBundle parent = getResourceBundleInternal( getParentLocale( locale ) );
        ResourceBundle bundle = loadResourceBundle( parent, locale );
        synchronized ( resourceBundles )
        {
            resourceBundles.put( locale, bundle );
        }

        return bundle;
    }


    private ResourceBundle loadResourceBundle( final ResourceBundle parent, final Locale locale )
    {
        final String path = "_" + locale.toString(); //$NON-NLS-1$
        final URL source = ( URL ) getResourceBundleEntries().get( path );
        return new ConsolePropertyResourceBundle( parent, source );
    }


    private synchronized Map getResourceBundleEntries()
    {
        if ( this.resourceBundleEntries == null )
        {
            String file = ( String ) bundle.getHeaders().get( Constants.BUNDLE_LOCALIZATION );
            if ( file == null )
            {
                file = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
            }

            // remove leading slash
            if ( file.startsWith( "/" ) ) //$NON-NLS-1$
            {
                file = file.substring( 1 );
            }

            // split path and base name
            int slash = file.lastIndexOf( '/' );
            String fileName = file.substring( slash + 1 );
            String path = ( slash <= 0 ) ? "/" : file.substring( 0, slash ); //$NON-NLS-1$

            HashMap resourceBundleEntries = new HashMap();

            Enumeration locales = bundle.findEntries( path, fileName + "*.properties", false ); //$NON-NLS-1$
            if ( locales != null )
            {
                while ( locales.hasMoreElements() )
                {
                    URL entry = ( URL ) locales.nextElement();

                    // calculate the key
                    String entryPath = entry.getPath();
                    final int start = entryPath.lastIndexOf( '/' ) + 1 + fileName.length(); // path, slash and base name
                    final int end = entryPath.length() - 11; // .properties suffix
                    entryPath = entryPath.substring( start, end );

                    // the default language is "name.properties" thus the entry
                    // path is empty and must default to "_"+DEFAULT_LOCALE
                    if (entryPath.length() == 0) {
                        entryPath = "_" + DEFAULT_LOCALE; //$NON-NLS-1$
                    }

                    // only add this entry, if the "language" is not provided
                    // by the main bundle or an earlier bound fragment
                    if (!resourceBundleEntries.containsKey( entryPath )) {
                        resourceBundleEntries.put( entryPath, entry );
                    }
                }
            }

            this.resourceBundleEntries = resourceBundleEntries;
        }

        return this.resourceBundleEntries;
    }


    private static final Locale getParentLocale( Locale locale )
    {
        if ( locale.getVariant().length() != 0 )
        {
            return new Locale( locale.getLanguage(), locale.getCountry() );
        }
        else if ( locale.getCountry().length() != 0 )
        {
            return new Locale( locale.getLanguage(), "" ); //$NON-NLS-1$
        }
        else if ( !locale.getLanguage().equals( DEFAULT_LOCALE.getLanguage() ) )
        {
            return DEFAULT_LOCALE;
        }

        // no more parents
        return null;
    }

}
