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
package org.apache.felix.webconsole.bundleinfo;


import java.util.Locale;

import org.osgi.framework.Bundle;


/**
 * The bundle info provider allows the user to supply additional information
 * that will be used by the Web Console bundle plugin.
 *
 * The API allows the user to register a special service, that could bind a
 * custom, implementation-specific information to a bundle.
 *
 * A typical use-case for that API would be the Declarative Services, that could
 * provide information about the components provided by this bundle (and link to
 * the component plugin too). Another usage could be the ProSyst resource
 * manager, that would provide information about the memory and CPU usage of the
 * bundle.
 *
 * @author Valentin Valchev
 */
public interface BundleInfoProvider
{

    /**
     * This is just an utility - empty array, that could be returned when there
     * is no additional information for a specific bundle.
     */
    public static final BundleInfo[] NO_INFO = new BundleInfo[0];


    /**
     * Gets the name of the bundle info provider as localized string.
     *
     * @param locale
     *            the locale in which the name should be returned
     * @return the name of the bundle info provider.
     */
    String getName( Locale locale );


    /**
     * Gets the associated bundle information with the specified bundle (by it's
     * ID)
     *
     * The Service may also be called outside through the new Inventory bundle
     * due to mapping the BundlesServlet to an InventoryPrinter and for example
     * calling it from a Gogo Shell. In this case the {@code webConsoleRoot}
     * parameter will be null a {@link BundleInfo} objects of type
     * {@link BundleInfoType#LINK} must not be generated.
     *
     * @param bundle
     *            the bundle, for which additional information is requested.
     * @param webConsoleRoot
     *            the root alias of the web console itself or {@code null}
     *            if this method is not called through the Web Console itself.
     * @param locale
     *            the locale in which the key-value pair should be returned.
     * @return array of available {@link BundleInfo} or empty array if none.
     */
    BundleInfo[] getBundleInfo( Bundle bundle, String webConsoleRoot, Locale locale );
}
