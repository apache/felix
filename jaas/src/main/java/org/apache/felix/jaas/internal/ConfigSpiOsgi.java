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

package org.apache.felix.jaas.internal;

import org.apache.felix.jaas.LoginContextFactory;
import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.felix.jaas.boot.ProxyLoginModule;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.*;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component(label = "%jaas.spi.name",
        description = "%jaas.spi.description",
        metatype = true,
        ds = false,
        name = "org.apache.felix.jaas.ConfigurationSpi",
        policy = ConfigurationPolicy.REQUIRE)
public class ConfigSpiOsgi extends ConfigurationSpi implements ManagedService,
        ServiceTrackerCustomizer, LoginContextFactory
{
    /**
     * Name of the algorithm to use to fetch JAAS Config
     */
    public static final String JAAS_CONFIG_ALGO_NAME = "JavaLoginConfig";

    public static final String SERVICE_PID = "org.apache.felix.jaas.ConfigurationSpi";

    private static enum GlobalConfigurationPolicy
    {
        DEFAULT, REPLACE, PROXY
    }

    private Map<String, Realm> configs = Collections.emptyMap();

    private final Logger log;

    /**
     * This is the name of application/realm used by LoginContext if no
     * AppConfigurationEntry is found for the appName which is passed while constructing it.
     *
     * In case it does not find any config it looks for config entry for an app named 'other'
     */
    private static final String DEFAULT_REALM_NAME = "other";

    @Property
    private static final String JAAS_DEFAULT_REALM_NAME = "jaas.defaultRealmName";
    private String defaultRealmName;

    private static final String DEFAULT_CONFIG_PROVIDER_NAME = "FelixJaasProvider";
    @Property(value = DEFAULT_CONFIG_PROVIDER_NAME)
    private static final String JAAS_CONFIG_PROVIDER_NAME = "jaas.configProviderName";

    @Property(value = "default", options = {
            @PropertyOption(name = "default", value = "%jaas.configPolicy.default"),
            @PropertyOption(name = "replace", value = "%jaas.configPolicy.replace"),
            @PropertyOption(name = "proxy", value = "%jaas.configPolicy.proxy") })
    static final String JAAS_CONFIG_POLICY = "jaas.globalConfigPolicy";

    private final Configuration osgiConfig = new OsgiConfiguration();

    private final Configuration originalConfig;

    private final Configuration proxyConfig;

    private volatile GlobalConfigurationPolicy globalConfigPolicy = GlobalConfigurationPolicy.DEFAULT;

    private final Map<ServiceReference, LoginModuleProvider> providerMap = new ConcurrentHashMap<ServiceReference, LoginModuleProvider>();

    private volatile String jaasConfigProviderName;

    private final Object lock = new Object();

    private final BundleContext context;

    private final ServiceTracker tracker;

    private ServiceRegistration spiReg;

    public ConfigSpiOsgi(BundleContext context, Logger log) throws ConfigurationException {
        this.context = context;
        this.log = log;
        this.originalConfig = getGlobalConfiguration();
        this.proxyConfig = new DelegatingConfiguration(osgiConfig, originalConfig);

        updated(getDefaultConfig());
        this.tracker = new ServiceTracker(context, LoginModuleFactory.class.getName(),
            this);

        Properties props = new Properties();
        props.setProperty(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        props.setProperty(Constants.SERVICE_PID, SERVICE_PID);

        this.context.registerService(ManagedService.class.getName(), this, props);

        //TODO Should this registration be made conditional i.e. service is only registered
        //only if there active LoginModules present
        this.context.registerService(LoginContextFactory.class.getName(), this, new Properties());
    }

    @Override
    public LoginContext createLoginContext(String realm, Subject subject,
        CallbackHandler handler) throws LoginException
    {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader cl = currentThread.getContextClassLoader();
        try
        {
            currentThread.setContextClassLoader(ProxyLoginModule.class.getClassLoader());
            Configuration config = Configuration.getInstance("JavaLoginConfig", null,
                jaasConfigProviderName);
            return new LoginContext(realm, subject, handler, config);
        }
        catch (NoSuchProviderException e)
        {
            throw new LoginException(e.getMessage());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new LoginException(e.getMessage());
        }
        finally
        {
            currentThread.setContextClassLoader(cl);
        }
    }

    @Override
    protected AppConfigurationEntry[] engineGetAppConfigurationEntry(String name)
    {
        Realm realm = configs.get(name);
        if (realm == null)
        {
            log.log(LogService.LOG_WARNING, "No JAAS module configured for realm " + name);
            return null;
        }

        return realm.engineGetAppConfigurationEntry();
    }

    Map<String, Realm> getAllConfiguration()
    {
        return configs;
    }

    private void recreateConfigs()
    {
        Map<String, Realm> realmToConfigMap = new HashMap<String, Realm>();
        for (LoginModuleProvider lmp : providerMap.values())
        {
            String realmName = lmp.realmName();
            if (realmName == null)
            {
                realmName = defaultRealmName;
            }

            Realm realm = realmToConfigMap.get(realmName);
            if (realm == null)
            {
                realm = new Realm(realmName);
                realmToConfigMap.put(realmName, realm);
            }

            realm.add(new AppConfigurationHolder(lmp));
        }

        for (Realm realm : realmToConfigMap.values())
        {
            realm.afterPropertiesSet();
        }

        //We also register the Spi with OSGI SR if any configuration is available
        //This would allow any client component to determine when it should start
        //and use the config
        if (!realmToConfigMap.isEmpty() && spiReg == null)
        {
            Properties props = new Properties();
            props.setProperty("providerName", "felix");

            synchronized (lock)
            {
                spiReg = context.registerService(ConfigurationSpi.class.getName(), this,
                    props);
            }
        }

        synchronized (lock)
        {
            this.configs = Collections.unmodifiableMap(realmToConfigMap);
        }
    }

    //--------------LifeCycle methods -------------------------------------

    void open()
    {
        this.configs = Collections.emptyMap();
        this.tracker.open();
    }

    void close()
    {
        this.tracker.close();
        deregisterProvider(jaasConfigProviderName);

        synchronized (lock)
        {
            providerMap.clear();
            configs = null; //Cannot call clear as its an unmodifiable map
        }

        if (globalConfigPolicy != GlobalConfigurationPolicy.DEFAULT)
        {
            restoreOriginalConfiguration();
        }
    }

    // --------------Config handling ----------------------------------------

    @Override
    public synchronized void updated(Dictionary properties) throws ConfigurationException
    {
        //TODO Do not know but for fresh install it is null
        if (properties == null)
        {
            return;
        }
        String newDefaultRealmName = PropertiesUtil.toString(
                properties.get(JAAS_DEFAULT_REALM_NAME), DEFAULT_REALM_NAME);

        if (!newDefaultRealmName.equals(defaultRealmName))
        {
            defaultRealmName = newDefaultRealmName;
            recreateConfigs();
        }

        String newProviderName = PropertiesUtil.toString(properties.get(JAAS_CONFIG_PROVIDER_NAME),
            DEFAULT_CONFIG_PROVIDER_NAME);

        deregisterProvider(jaasConfigProviderName);
        registerProvider(newProviderName);
        jaasConfigProviderName = newProviderName;

        manageGlobalConfiguration(properties);
    }

    private void manageGlobalConfiguration(Dictionary props)
    {
        String configPolicy = PropertiesUtil.toString(props.get(JAAS_CONFIG_POLICY),
                GlobalConfigurationPolicy.DEFAULT.name());
        configPolicy = Util.trimToNull(configPolicy);

        GlobalConfigurationPolicy policy = GlobalConfigurationPolicy.DEFAULT;
        if (configPolicy != null)
        {
            policy = GlobalConfigurationPolicy.valueOf(configPolicy.toUpperCase());
        }

        this.globalConfigPolicy = policy;

        if (policy == GlobalConfigurationPolicy.REPLACE)
        {
            Configuration.setConfiguration(osgiConfig);
            log.log(LogService.LOG_INFO,
                "Replacing the global JAAS configuration with OSGi based configuration");
        }
        else if (policy == GlobalConfigurationPolicy.PROXY)
        {
            Configuration.setConfiguration(proxyConfig);
            log.log(
                LogService.LOG_INFO,
                "Replacing the global JAAS configuration with OSGi based proxy configuration. "
                    + "It would look first in the OSGi based configuration and if not found would use the default global "
                    + "configuration");
        }
        else if (policy == GlobalConfigurationPolicy.DEFAULT)
        {
            restoreOriginalConfiguration();
        }
    }

    private void restoreOriginalConfiguration()
    {
        if (originalConfig == null)
        {
            return;
        }

        Configuration current = Configuration.getConfiguration();
        if (current != originalConfig)
        {
            Configuration.setConfiguration(originalConfig);
        }
    }

    private Dictionary<String,String> getDefaultConfig() throws ConfigurationException
    {
        //Determine default config. Value can still be overridden by BundleContext properties
        Dictionary<String,String> dict = new Hashtable<String,String>();
        put(dict,JAAS_DEFAULT_REALM_NAME,DEFAULT_REALM_NAME);
        put(dict,JAAS_CONFIG_PROVIDER_NAME,DEFAULT_CONFIG_PROVIDER_NAME);
        put(dict, JAAS_CONFIG_POLICY, GlobalConfigurationPolicy.DEFAULT.name());
        return dict;
    }

    private void put(Dictionary<String,String> dict, String key, String defaultValue)
    {
        dict.put(key,PropertiesUtil.toString(context.getProperty(key),defaultValue));
    }

    // --------------JAAS/JCA/Security ----------------------------------------

    private void registerProvider(String providerName)
    {
        Security.addProvider(new OSGiProvider(providerName));
        log.log(LogService.LOG_INFO, "Registered provider " + providerName
            + " for managing JAAS config with type " + JAAS_CONFIG_ALGO_NAME);
    }

    private void deregisterProvider(String providerName)
    {
        Security.removeProvider(providerName);
        log.log(LogService.LOG_INFO, "Removed provider " + providerName + " type "
            + JAAS_CONFIG_ALGO_NAME + " from Security providers list");
    }

    // ---------- ServiceTracker ----------------------------------------------

    @Override
    public Object addingService(ServiceReference reference)
    {
        LoginModuleFactory lmf = (LoginModuleFactory) context.getService(reference);
        registerFactory(reference, lmf);
        recreateConfigs();
        return lmf;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service)
    {
        LoginModuleFactory lmf = providerMap.get(reference);
        if (lmf instanceof OsgiLoginModuleProvider) {
            // refresh to update configs
            ((OsgiLoginModuleProvider) lmf).configure();
        }
        recreateConfigs();
    }

    @Override
    public void removedService(ServiceReference reference, Object service)
    {
        deregisterFactory(reference);
        recreateConfigs();
        context.ungetService(reference);
    }

    private void deregisterFactory(ServiceReference ref)
    {
        LoginModuleProvider lmp = providerMap.remove(ref);
        if (lmp != null)
        {
            log.log(LogService.LOG_INFO, "Deregistering LoginModuleFactory " + lmp);
        }
    }

    private void registerFactory(ServiceReference ref, LoginModuleFactory lmf)
    {
        LoginModuleProvider lmfExt;
        if (lmf instanceof LoginModuleProvider)
        {
            lmfExt = (LoginModuleProvider) lmf;
        }
        else
        {
            lmfExt = new OsgiLoginModuleProvider(ref, lmf);
        }
        log.log(LogService.LOG_INFO, "Registering LoginModuleFactory " + lmf);
        providerMap.put(ref, lmfExt);
    }

    private static Configuration getGlobalConfiguration()
    {
        try
        {
            return Configuration.getConfiguration();
        }
        catch (Exception e)
        {
            // means no JAAS configuration file OR no permission to read it
        }
        return null;
    }

    private class OSGiProvider extends Provider
    {
        public static final String TYPE_CONFIGURATION = "Configuration";

        OSGiProvider(String providerName)
        {
            super(providerName, 1.0, "OSGi based provider for Jaas configuration");
        }

        @Override
        public synchronized Service getService(String type, String algorithm)
        {
            if (TYPE_CONFIGURATION.equals(type)
                && JAAS_CONFIG_ALGO_NAME.equals(algorithm))
            {
                return new ConfigurationService(this);
            }
            return super.getService(type, algorithm);
        }
    }

    private class ConfigurationService extends Provider.Service
    {

        public ConfigurationService(Provider provider)
        {
            super(provider, OSGiProvider.TYPE_CONFIGURATION, //the type of this service
                JAAS_CONFIG_ALGO_NAME, //the algorithm name
                ConfigSpiOsgi.class.getName(), //the name of the class implementing this service
                Collections.<String> emptyList(), //List of aliases or null if algorithm has no aliases
                Collections.<String, String> emptyMap()); //Map of attributes or null if this implementation
        }

        @Override
        public Object newInstance(Object constructorParameter)
            throws NoSuchAlgorithmException
        {
            //constructorParameter is the one which is passed as Configuration.Parameters params
            //for now we do not make use of that
            return ConfigSpiOsgi.this;
        }
    }

    //---------------------------- Global Configuration Handling

    private class OsgiConfiguration extends Configuration
    {

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name)
        {
            return ConfigSpiOsgi.this.engineGetAppConfigurationEntry(name);
        }
    }

    private class DelegatingConfiguration extends Configuration
    {
        private final Configuration primary;
        private final Configuration secondary;

        private DelegatingConfiguration(Configuration primary, Configuration secondary)
        {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name)
        {
            // check if jaas-loginModule or fallback is configured
            AppConfigurationEntry[] result = null;
            try
            {
                result = primary.getAppConfigurationEntry(name);
            }
            catch (Exception e)
            {
                // means no JAAS configuration file OR no permission to read it
            }

            if (result == null)
            {
                try
                {
                    result = secondary.getAppConfigurationEntry(name);
                }
                catch (Exception e)
                {
                    // WLP 9.2.0 throws IllegalArgumentException for unknown appName
                }
            }

            return result;
        }
    }

    //-------------------------------------OSGi Config Management

    static final class Realm
    {
        private final String realmName;
        private AppConfigurationEntry[] configArray;
        private List<AppConfigurationHolder> configs = new ArrayList<AppConfigurationHolder>();

        Realm(String realmName)
        {
            this.realmName = realmName;
        }

        public void add(AppConfigurationHolder config)
        {
            configs.add(config);
        }

        public void afterPropertiesSet()
        {
            Collections.sort(configs);
            configArray = new AppConfigurationEntry[configs.size()];
            for (int i = 0; i < configs.size(); i++)
            {
                configArray[i] = configs.get(i).getEntry();
            }
            configs = Collections.unmodifiableList(configs);
        }

        public String getRealmName()
        {
            return realmName;
        }

        public List<AppConfigurationHolder> getConfigs()
        {
            return configs;
        }

        public AppConfigurationEntry[] engineGetAppConfigurationEntry()
        {
            return Arrays.copyOf(configArray, configArray.length);
        }

        @Override
        public String toString()
        {
            return "Realm{" + "realmName='" + realmName + '\'' + '}';
        }
    }

    static final class AppConfigurationHolder implements Comparable<AppConfigurationHolder>
    {
        private static final String LOGIN_MODULE_CLASS = ProxyLoginModule.class.getName();
        private final LoginModuleProvider provider;
        private final int ranking;
        private final AppConfigurationEntry entry;

        public AppConfigurationHolder(LoginModuleProvider provider)
        {
            this.provider = provider;
            this.ranking = provider.ranking();

            Map<String, Object> options = new HashMap<String, Object>(provider.options());
            options.put(ProxyLoginModule.PROP_LOGIN_MODULE_FACTORY, provider);
            this.entry = new AppConfigurationEntry(LOGIN_MODULE_CLASS,
                provider.getControlFlag(), Collections.unmodifiableMap(options));
        }

        public int compareTo(AppConfigurationHolder that)
        {
            if (this.ranking == that.ranking)
            {
                return 0;
            }
            return this.ranking > that.ranking ? -1 : 1;
        }

        public AppConfigurationEntry getEntry()
        {
            return entry;
        }

        public LoginModuleProvider getProvider()
        {
            return provider;
        }
    }
}
