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
package org.apache.felix.webconsole.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.webconsole.internal.i18n.ResourceBundleCache;
import org.osgi.framework.Bundle;

/**
 * The localization helper is supposed to be used from the bundle info
 * providers. It will allow them to provide locale-specific names and
 * descriptions of the bundle information entries.
 * 
 * @author Valentin Valchev
 */
public class LocalizationHelper {

    private final ResourceBundleCache cache;

    /**
     * Creates a new helper instance.
     * 
     * @param bundle
     *            the bundle that provides the localization resources. See the
     *            standard OSGi-type localization support.
     */
    public LocalizationHelper(Bundle bundle) {
	if (null == bundle)
	    throw new NullPointerException();
	this.cache = new ResourceBundleCache(bundle);
    }

    /**
     * Used to retrieve the resource bundle for the specified locale.
     * 
     * @param locale
     *            the requested locale.
     * @return the resource bundle (could be empty, but never <code>null</code>)
     */
    public ResourceBundle getResourceBundle(final Locale locale) {
	return cache.getResourceBundle(locale);
    }
}
