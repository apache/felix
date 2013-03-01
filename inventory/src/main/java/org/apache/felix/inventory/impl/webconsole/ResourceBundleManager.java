/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.inventory.impl.webconsole;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;

/**
 * The ResourceBundleManager manages resource bundle instance per OSGi Bundle.
 * It contains a local cache, for bundles, but when a bundle is being
 * unistalled,
 * its resources stored in the cache are cleaned up.
 */
public class ResourceBundleManager implements BundleListener
{

    private final BundleContext bundleContext;

    private final Map resourceBundleCaches;

    /**
     * Creates a new object and adds self as a bundle listener
     * 
     * @param bundleContext the bundle context of the Web Console.
     */
    public ResourceBundleManager(final BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        this.resourceBundleCaches = new HashMap();

        bundleContext.addBundleListener(this);
    }

    /**
     * Removes the bundle lister.
     */
    public void dispose()
    {
        bundleContext.removeBundleListener(this);
    }

    /**
     * This method is used to retrieve a /cached/ instance of the i18n resource
     * associated
     * with a given bundle.
     * 
     * @param provider the bundle, provider of the resources
     * @param locale the requested locale.
     */
    public ResourceBundle getResourceBundle(final Bundle provider)
    {
        ResourceBundle cache;
        final Long key = new Long(provider.getBundleId());
        synchronized (resourceBundleCaches)
        {
            cache = (ResourceBundle) resourceBundleCaches.get(key);
            if (cache == null && !resourceBundleCaches.containsKey(key))
            {
                cache = this.loadResourceBundle(provider);
                resourceBundleCaches.put(key, cache);
            }
        }

        return cache;
    }

    // ---------- BundleListener

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public final void bundleChanged(BundleEvent event)
    {
        if (event.getType() == BundleEvent.STOPPED)
        {
            final Long key = new Long(event.getBundle().getBundleId());
            synchronized (resourceBundleCaches)
            {
                resourceBundleCaches.remove(key);
            }
        }
    }

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private ResourceBundle loadResourceBundle(final Bundle bundle)
    {
        final String path = "_" + DEFAULT_LOCALE.toString(); //$NON-NLS-1$
        final URL source = (URL) getResourceBundleEntries(bundle).get(path);
        if (source != null)
        {
            try
            {
                return new PropertyResourceBundle(source.openStream());
            }
            catch (final IOException ignore)
            {
                // ignore
            }
        }
        return null;
    }

    // TODO : Instead of getting all property files, we could just get the one
    // for the default locale
    private synchronized Map getResourceBundleEntries(final Bundle bundle)
    {
        String file = (String) bundle.getHeaders().get(Constants.BUNDLE_LOCALIZATION);
        if (file == null)
        {
            file = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
        }

        // remove leading slash
        if (file.startsWith("/")) //$NON-NLS-1$
        {
            file = file.substring(1);
        }

        // split path and base name
        int slash = file.lastIndexOf('/');
        String fileName = file.substring(slash + 1);
        String path = (slash <= 0) ? "/" : file.substring(0, slash); //$NON-NLS-1$

        HashMap resourceBundleEntries = new HashMap();

        Enumeration locales = bundle.findEntries(path, fileName + "*.properties", false); //$NON-NLS-1$
        if (locales != null)
        {
            while (locales.hasMoreElements())
            {
                URL entry = (URL) locales.nextElement();

                // calculate the key
                String entryPath = entry.getPath();
                final int start = entryPath.lastIndexOf('/') + 1 + fileName.length(); // path,
                                                                                      // slash
                                                                                      // and
                                                                                      // base
                                                                                      // name
                final int end = entryPath.length() - 11; // .properties suffix
                entryPath = entryPath.substring(start, end);

                // the default language is "name.properties" thus the entry
                // path is empty and must default to "_"+DEFAULT_LOCALE
                if (entryPath.length() == 0)
                {
                    entryPath = "_" + DEFAULT_LOCALE; //$NON-NLS-1$
                }

                // only add this entry, if the "language" is not provided
                // by the main bundle or an earlier bound fragment
                if (!resourceBundleEntries.containsKey(entryPath))
                {
                    resourceBundleEntries.put(entryPath, entry);
                }
            }
        }

        return resourceBundleEntries;
    }
}
