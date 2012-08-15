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
package org.apache.felix.webconsole.internal.core;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

import org.apache.felix.webconsole.bundleinfo.BundleInfo;
import org.apache.felix.webconsole.bundleinfo.BundleInfoProvider;
import org.apache.felix.webconsole.bundleinfo.BundleInfoType;
import org.apache.felix.webconsole.i18n.LocalizationHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

final class ServicesUsedInfoProvider implements
	BundleInfoProvider {

    // TODO: add i18n for those entries
    private static final String SERVICE_DESCRIPTION = "%services.info.descr.";
    
    private final LocalizationHelper localization;
    
    ServicesUsedInfoProvider(Bundle bundle) 
    {
	localization = new LocalizationHelper(bundle);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.felix.webconsole.bundleinfo.BundleInfoProvider#getName(java
     * .util.Locale)
     */
    public String getName(Locale locale) 
    {
	return localization.getResourceBundle(locale).getString("services.info.name"); //$NON-NLS-1$;
    }

    public BundleInfo[] getBundleInfo(Bundle bundle, String webConsoleRoot,
	    Locale locale) 
    {
	final ServiceReference[] refs = bundle.getServicesInUse();
	if (null == refs || refs.length == 0)
	    return NO_INFO;
	
	BundleInfo[] ret = new BundleInfo[refs.length];
	for ( int i=0; i < refs.length; i++ )
	{
	    ret[i] = toInfo(refs[i], webConsoleRoot, locale);
	}
	return ret;
    }

    private BundleInfo toInfo(ServiceReference ref, String webConsoleRoot, Locale locale) 
    {
	final String[] classes = (String[]) ref
		.getProperty(Constants.OBJECTCLASS);
	final Object id = ref.getProperty(Constants.SERVICE_ID);
	final String descr =  localization.getResourceBundle(locale).getString("services.info.descr"); //$NON-NLS-1$;
	String name = localization.getResourceBundle(locale).getString("services.info.key"); //$NON-NLS-1$;
	name = MessageFormat.format(name, new Object[] {
		id, Arrays.asList(classes).toString()
	});
	return new BundleInfo(name, webConsoleRoot + "/services/" + id, //$NON-NLS-1$
		BundleInfoType.LINK, descr);
    }
    
    ServiceRegistration register( BundleContext context )
    {
	return context.registerService(BundleInfoProvider.class.getName(), this, null);
    }

}
