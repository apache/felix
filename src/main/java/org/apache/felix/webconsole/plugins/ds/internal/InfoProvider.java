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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.webconsole.bundleinfo.BundleInfo;
import org.apache.felix.webconsole.bundleinfo.BundleInfoProvider;
import org.apache.felix.webconsole.bundleinfo.BundleInfoType;
import org.apache.felix.webconsole.i18n.LocalizationHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

class InfoProvider implements BundleInfoProvider
{

    private final LocalizationHelper localization;

    private final ServiceComponentRuntime scrService;

    InfoProvider(Bundle bundle, Object scrService)
    {
        this.scrService = (ServiceComponentRuntime) scrService;
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
        final List<ComponentDescriptionDTO> disabled = new ArrayList<ComponentDescriptionDTO>();
        final List<ComponentConfigurationDTO> configurations = new ArrayList<ComponentConfigurationDTO>();

        final Collection<ComponentDescriptionDTO> descs = scrService.getComponentDescriptionDTOs(bundle);
        for(final ComponentDescriptionDTO d : descs)
        {
            if ( !scrService.isComponentEnabled(d))
            {
                disabled.add(d);
            }
            else
            {
                final Collection<ComponentConfigurationDTO> configs = scrService.getComponentConfigurationDTOs(d);
                if ( configs.isEmpty() )
                {
                    disabled.add(d);
                }
                else
                {
                    configurations.addAll(configs);
                }
            }
        }
        Collections.sort(configurations, Util.COMPONENT_COMPARATOR);

        if (configurations.isEmpty())
        {
            return NO_INFO;
        }

        BundleInfo[] ret = new BundleInfo[configurations.size() + disabled.size()];
        int i=0;
        for (final ComponentDescriptionDTO cfg : disabled)
        {
            ret[i] = toInfo(cfg, webConsoleRoot, locale);
            i++;
        }
        for (final ComponentConfigurationDTO cfg : configurations)
        {
            ret[i] = toInfo(cfg, webConsoleRoot, locale);
            i++;
        }
        return ret;
    }

    private BundleInfo toInfo(final ComponentDescriptionDTO cfg, String webConsoleRoot, Locale locale)
    {
        final ResourceBundle bundle = localization.getResourceBundle(locale);
        final String state = "disabled";
        final String name = cfg.name;
        final String descr = bundle.getString("info.descr"); //$NON-NLS-1$;
        String key = bundle.getString("info.key"); //$NON-NLS-1$;
        // Component #{0} {1}, state {2}
        key = MessageFormat.format(key, new Object[] { "", //$NON-NLS-1$
                name,
                state
        });
        return new BundleInfo(key,
                (webConsoleRoot == null ? "" : webConsoleRoot) + "/components", //$NON-NLS-1$
                BundleInfoType.LINK,
                descr);
    }

    private BundleInfo toInfo(final ComponentConfigurationDTO cfg, String webConsoleRoot, Locale locale)
    {
        final ResourceBundle bundle = localization.getResourceBundle(locale);
        final String state = ComponentConfigurationPrinter.toStateString(cfg.state);
        final String name = cfg.description.name;
        final String descr = bundle.getString("info.descr"); //$NON-NLS-1$;
        String key = bundle.getString("info.key"); //$NON-NLS-1$;
        // Component #{0} {1}, state {2}
        key = MessageFormat.format(key, new Object[] { String.valueOf(cfg.id),
                name,
                state
        });
        return new BundleInfo(key, (webConsoleRoot == null ? "" : webConsoleRoot) + "/components/" + cfg.id, //$NON-NLS-1$
                BundleInfoType.LINK, descr);
    }

    ServiceRegistration register(BundleContext context)
    {
        return context.registerService(BundleInfoProvider.class.getName(), this, null);
    }

}
