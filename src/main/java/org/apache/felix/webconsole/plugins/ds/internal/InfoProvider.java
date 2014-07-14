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
package org.apache.felix.webconsole.plugins.ds.internal;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.felix.webconsole.bundleinfo.BundleInfo;
import org.apache.felix.webconsole.bundleinfo.BundleInfoProvider;
import org.apache.felix.webconsole.bundleinfo.BundleInfoType;
import org.apache.felix.webconsole.i18n.LocalizationHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

class InfoProvider implements BundleInfoProvider
{

    private final LocalizationHelper localization;

    private final ScrService scrService;

    InfoProvider(Bundle bundle, Object scrService)
    {
        this.scrService = (ScrService) scrService;
        localization = new LocalizationHelper(bundle);
    }

    /**
     * @see org.apache.felix.webconsole.bundleinfo.BundleInfoProvider#getName(java.util.Locale)
     */
    public String getName(Locale locale)
    {
        return localization.getResourceBundle(locale).getString("info.name"); //$NON-NLS-1$;;
    }

    /**
    * @see org.apache.felix.webconsole.bundleinfo.BundleInfoProvider#getBundleInfo(org.osgi.framework.Bundle,
    *      java.lang.String, java.util.Locale)
    */
    public BundleInfo[] getBundleInfo(Bundle bundle, String webConsoleRoot, Locale locale)
    {

        final Component[] components = scrService.getComponents(bundle);
        if (null == components || components.length == 0)
        {
            return NO_INFO;
        }

        BundleInfo[] ret = new BundleInfo[components.length];
        for (int i = 0; i < components.length; i++)
        {
            ret[i] = toInfo(components[i], webConsoleRoot, locale);
        }
        return ret;
    }

    private BundleInfo toInfo(Component component, String webConsoleRoot, Locale locale)
    {
        final ResourceBundle bundle = localization.getResourceBundle(locale);
        final String state = ComponentConfigurationPrinter.toStateString(component.getState());
        final String name = component.getName();
        final String descr = bundle.getString("info.descr"); //$NON-NLS-1$;
        String key = bundle.getString("info.key"); //$NON-NLS-1$;
        // Component #{0} {1}, state {2}
        key = MessageFormat.format(key, new Object[] { String.valueOf(component.getId()), //
                name != null ? name : "", //$NON-NLS-1$
                state, //
        });
        return new BundleInfo(key, webConsoleRoot + "/components/" + component.getId(), //$NON-NLS-1$
            BundleInfoType.LINK, descr);
    }

    ServiceRegistration register(BundleContext context)
    {
        return context.registerService(BundleInfoProvider.class.getName(), this, null);
    }

}
