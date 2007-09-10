/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.console.web.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * The <code>ConfigManagerBase</code> TODO
 */
abstract class ConfigManagerBase {

    private BundleContext bundleContext;

    /** @scr.reference */
    private ConfigurationAdmin configurationAdmin;

    /** @scr.reference */
    private MetaTypeService metaTypeService;

    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    protected ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    protected MetaTypeService getMetaTypeService() {
        return metaTypeService;
    }

    protected Map<String, Bundle> getMetadataPids() {
        Map<String, Bundle> pids = new HashMap<String, Bundle>();
        MetaTypeService mts = getMetaTypeService();
        if (mts != null) {
            Bundle[] bundles = getBundleContext().getBundles();
            for (int i=0; i < bundles.length; i++) {
                MetaTypeInformation mti = mts.getMetaTypeInformation(bundles[i]);
                if (mti != null) {
                    String[] pidList = mti.getPids();
                    for (int j = 0; pidList != null && j < pidList.length; j++) {
                        pids.put(pidList[j], bundles[i]);
                    }
                }
            }
        }
        return pids;
    }
    
    protected ObjectClassDefinition getObjectClassDefinition(
            Configuration config, String locale) {
        
        // if the configuration is not bound, search in the bundles
        if (config.getBundleLocation() == null) {
            ObjectClassDefinition ocd = getObjectClassDefinition(config.getPid(), locale);
            if (ocd != null) {
                return ocd;
            }
            
            // if none, check whether there might be one for the factory PID
            if (config.getFactoryPid() != null) {
                return getObjectClassDefinition(config.getFactoryPid(), locale);
            }
        }
        
        MetaTypeService mts = getMetaTypeService();
        if (mts != null) {
            Bundle bundle = getBundle(config.getBundleLocation());
            if (bundle != null) {
                MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);
                if (mti != null) {
                    // try OCD by PID first
                    ObjectClassDefinition ocd = mti.getObjectClassDefinition(config.getPid(), locale);
                    if (ocd != null) {
                        return ocd;
                    }
                    
                    // if none, check whether there might be one for the factory PID
                    if (config.getFactoryPid() != null) {
                        return mti.getObjectClassDefinition(config.getFactoryPid(), locale);
                    }
                }
            }
        }

        // fallback to nothing found
        return null;
    }

    protected ObjectClassDefinition getObjectClassDefinition(Bundle bundle,
            String pid, String locale) {
        if (bundle != null) {
            MetaTypeService mts = getMetaTypeService();
            if (mts != null) {
                MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);
                if (mti != null) {
                    return mti.getObjectClassDefinition(pid, locale);
                }
            }
        }

        // fallback to nothing found
        return null;
    }
    
    protected ObjectClassDefinition getObjectClassDefinition(String pid, String locale) {
        Bundle[] bundles = getBundleContext().getBundles();
        for (int i=0; i < bundles.length; i++) {
            try {
                ObjectClassDefinition ocd = getObjectClassDefinition(bundles[i], pid, locale);
                if (ocd != null) {
                    return ocd;
                }
            } catch (IllegalArgumentException iae) {
                // don't care
            }
        }
        return null;
    }
    
    protected Map getAttributeDefinitionMap(Configuration config, String locale) {
        ObjectClassDefinition ocd = getObjectClassDefinition(config, locale);
        if (ocd != null) {
            AttributeDefinition[] ad = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
            if (ad != null) {
                Map adMap = new HashMap();
                for (int i = 0; i < ad.length; i++) {
                    adMap.put(ad[i].getID(), ad[i]);
                }
                return adMap;
            }
        }
        
        // fallback to nothing found
        return null;
    }
    
    protected Bundle getBundle(String bundleLocation) {
        if (bundleLocation == null) {
            return null;
        }

        Bundle[] bundles = getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if (bundleLocation.equals(bundles[i].getLocation())) {
                return bundles[i];
            }
        }

        return null;
    }

    //--------- SCR Integration -----------------------------------------------
    
    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
    }

    protected void deactivate(ComponentContext context) {
        bundleContext = null;
    }
    
    protected void bindConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
    
    protected void unbindConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }
    
    protected void bindMetaTypeService(MetaTypeService metaTypeService) {
        this.metaTypeService = metaTypeService;
    }

    protected void unbindMetaTypeService(MetaTypeService metaTypeService) {
        this.metaTypeService = null;
    }
}
