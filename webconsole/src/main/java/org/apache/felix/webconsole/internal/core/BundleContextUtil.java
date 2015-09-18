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


import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;


/**
 * The <code>BundleContextUtil</code> class.
 */
public class BundleContextUtil
{
    /**
     * This property defines which bundle context the web console plugins use to
     * get the list of bundles and services. It defaults to {@link #WORK_CTX_OWN}.
     * If {@link #WORK_CTX_SYSTEM} is specified, the web console plugins use the
     * system bundle context. If an invalid value is specified, the default is used.
     * This setting effects only the built-in plugins.
     */
    public static final String FWK_PROP_WORK_CONTEXT = "felix.webconsole.work.context";

    /** The web console uses the own bundle context. (default) */
    public static final String WORK_CTX_OWN = "own";

    /** The web console uses the system bundle context. */
    public static final String WORK_CTX_SYSTEM = "system";

    /**
     * Get the working bundle context: the bundle context to lookup bundles and
     * services.
     */
    public static BundleContext getWorkingBundleContext( final BundleContext bc)
    {
        if ( WORK_CTX_SYSTEM.equalsIgnoreCase(bc.getProperty(FWK_PROP_WORK_CONTEXT)) )
        {
            return bc.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        }
        return bc;
    }
}