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


/**
 * The <code>BundleContextUtil</code> class.
 */
public class BundleContextUtil
{
    /**
     * If this property is specified (regardless of it's value), the system bundle is used
     * as a working bundle context. Otherwise the web console bundle context is used.
     */
    public static final String FWK_PROP_USE_SYSTEM_BUNDLE = "webconsole.use.systembundle";

    /**
     * Get the working bundle context: the bundle context to lookup bundles and
     * services.
     */
    public static BundleContext getWorkingBundleContext( final BundleContext bc)
    {
        if ( bc.getProperty(FWK_PROP_USE_SYSTEM_BUNDLE) != null )
        {
            return bc.getBundle(0).getBundleContext();
        }
        return bc;
    }
}