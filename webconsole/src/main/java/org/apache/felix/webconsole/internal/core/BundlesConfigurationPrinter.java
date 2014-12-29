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


import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>BundlesConfigurationPrinter</code> prints out the bundle list.
 */
public class BundlesConfigurationPrinter
    extends AbstractConfigurationPrinter
{

    private ServiceTracker packageAdminTracker;

    /**
     * @see org.apache.felix.webconsole.internal.AbstractConfigurationPrinter#activate(org.osgi.framework.BundleContext)
     */
    public void activate(final BundleContext bundleContext)
    {
        super.activate(bundleContext);
        this.packageAdminTracker = new ServiceTracker(bundleContext, PackageAdmin.class.getName(), null);
        this.packageAdminTracker.open();
    }

    /**
     * @see org.apache.felix.webconsole.internal.AbstractConfigurationPrinter#deactivate()
     */
    public void deactivate()
    {
        if ( this.packageAdminTracker != null )
        {
            this.packageAdminTracker.close();
            this.packageAdminTracker = null;
        }
        super.deactivate();
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public String getTitle()
    {
        return "Bundlelist";
    }

    private String getHeaderValue(final Bundle b, final String name)
    {
        String val = (String)b.getHeaders().get(name);
        if ( val == null )
        {
            val = "";
        }
        return val;
    }

    private String getState(final int state)
    {
        switch (state)
        {
            case Bundle.ACTIVE : return "active";
            case Bundle.INSTALLED : return "installed";
            case Bundle.RESOLVED : return "resolved";
            case Bundle.STARTING : return "starting";
            case Bundle.STOPPING : return "stopping";
            case Bundle.UNINSTALLED : return "uninstalled";
        }
        return String.valueOf(state);
    }

    private final boolean isFragmentBundle( final Bundle bundle)
    {
        return ((PackageAdmin)this.packageAdminTracker.getService()).getBundleType( bundle ) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration( final PrintWriter pw )
    {
        final Bundle[] bundles = BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getBundles();
        // create a map for sorting first
        final TreeMap bundlesMap = new TreeMap();
        int active = 0, installed = 0, resolved = 0, fragments = 0;
        for( int i =0; i<bundles.length; i++)
        {
            final Bundle bundle = bundles[i];
            final String symbolicName = bundle.getSymbolicName();
            final String version = (String)bundle.getHeaders().get(Constants.BUNDLE_VERSION);

            // count states and calculate prefix
            switch ( bundle.getState() )
            {
                case Bundle.ACTIVE:
                    active++;
                    break;
                case Bundle.INSTALLED:
                    installed++;
                    break;
                case Bundle.RESOLVED:
                    if ( isFragmentBundle( bundle ) )
                    {
                        fragments++;
                    }
                    else
                    {
                        resolved++;
                    }
                    break;
            }

            final String key = symbolicName + ':' + version;
            final String value = MessageFormat.format( "{0} ({1}) \"{2}\" [{3}, {4}] {5}", new Object[]
                  { symbolicName,
                    version,
                    getHeaderValue(bundle, Constants.BUNDLE_NAME),
                    getState(bundle.getState()),
                    String.valueOf(bundle.getBundleId()),
                    isFragmentBundle(bundle) ? "(fragment)" : ""} );
            bundlesMap.put(key, value);

        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Status: ");
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
        }
        pw.println(buffer.toString());
        pw.println();
        final Iterator i = bundlesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry entry = (Map.Entry)i.next();
            pw.println(entry.getValue());

        }
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
}
