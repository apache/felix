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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
<<<<<<< HEAD
import java.util.Map;
import java.util.Properties;
=======
import java.util.Hashtable;
import java.util.Map;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.felix.scr.annotations.Component;
<<<<<<< HEAD
=======
import org.apache.felix.scr.annotations.Properties;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;

import static org.apache.felix.jaas.internal.Util.trimToNull;

@Component(label = "%jaas.name",
        description = "%jaas.description",
        metatype = true,
        ds = false,
        name = JaasConfigFactory.SERVICE_PID,
        configurationFactory = true)
<<<<<<< HEAD
=======
@Properties({
    @Property(name = LoginModuleFactory.JAAS_CONTROL_FLAG, value = "required", options = {
        @PropertyOption(name = "required", value = "%jaas.flag.required"),
        @PropertyOption(name = "requisite", value = "%jaas.flag.requisite"),
        @PropertyOption(name = "sufficient", value = "%jaas.flag.sufficient"),
        @PropertyOption(name = "optional", value = "%jaas.flag.optional")
    }),
    @Property(name = LoginModuleFactory.JAAS_RANKING, intValue = 0),
    @Property(name = LoginModuleFactory.JAAS_REALM_NAME),
    @Property(name = "webconsole.configurationFactory.nameHint",
            value = "{" + LoginModuleFactory.JAAS_RANKING + "} : {" + JaasConfigFactory.JAAS_CLASS_NAME + "}"
                    + " ({" + LoginModuleFactory.JAAS_CONTROL_FLAG + "})")
})
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class JaasConfigFactory implements ManagedServiceFactory
{

    public static final String SERVICE_PID = "org.apache.felix.jaas.Configuration.factory";

    @Property
    static final String JAAS_CLASS_NAME = "jaas.classname";

<<<<<<< HEAD
    @Property(value = "required", options = {
            @PropertyOption(name = "required", value = "%jaas.flag.required"),
            @PropertyOption(name = "requisite", value = "%jaas.flag.requisite"),
            @PropertyOption(name = "sufficient", value = "%jaas.flag.sufficient"),
            @PropertyOption(name = "optional", value = "%jaas.flag.optional") })
    static final String JAAS_CONTROL_FLAG = "jaas.controlFlag";

    @Property(intValue = 0)
    static final String JAAS_RANKING = "jaas.ranking";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    static final String JAAS_OPTIONS = "jaas.options";

    @Property
    static final String JAAS_REALM_NAME = "jaas.realmName";

=======
    @Property(unbounded = PropertyUnbounded.ARRAY)
    static final String JAAS_OPTIONS = "jaas.options";

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    private final Logger log;

    private final LoginModuleCreator factory;

    private final BundleContext context;

    private final ConcurrentMap<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();

    public JaasConfigFactory(BundleContext context, LoginModuleCreator factory, Logger log)
    {
        this.context = context;
        this.factory = factory;
        this.log = log;

<<<<<<< HEAD
        Properties props = new Properties();
        props.setProperty(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        props.setProperty(Constants.SERVICE_PID, SERVICE_PID);
=======
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        props.put(Constants.SERVICE_PID, SERVICE_PID);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        context.registerService(ManagedServiceFactory.class.getName(), this, props);
    }

    @Override
    public String getName()
    {
        return "JaasConfigFactory";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updated(String pid, Dictionary config) throws ConfigurationException
    {
        String className = trimToNull(PropertiesUtil.toString(config.get(JAAS_CLASS_NAME), null));
<<<<<<< HEAD
        String flag = trimToNull(PropertiesUtil.toString(config.get(JAAS_CONTROL_FLAG), "required"));
        int ranking = PropertiesUtil.toInteger(config.get(JAAS_RANKING), 0);
=======
        String flag = trimToNull(PropertiesUtil.toString(config.get(LoginModuleFactory.JAAS_CONTROL_FLAG), "required"));
        int ranking = PropertiesUtil.toInteger(config.get(LoginModuleFactory.JAAS_RANKING), 0);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        //TODO support system property substitution e.g. ${user.home}
        //in property values
        Map options = PropertiesUtil.toMap(config.get(JAAS_OPTIONS), new String[0]);
<<<<<<< HEAD
        String realmName = trimToNull(PropertiesUtil.toString(config.get(JAAS_REALM_NAME), null));
=======
        String realmName = trimToNull(PropertiesUtil.toString(config.get(LoginModuleFactory.JAAS_REALM_NAME), null));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        if (className == null)
        {
            log.log(LogService.LOG_WARNING,
                "Class name for the LoginModule is required. Configuration would be ignored"
                    + config);
            return;
        }

        //Combine the config. As the jaas.options is required for capturing config
        //via felix webconsole. However in normal usage people would like to provide
        //key=value pair directly in config. So merge both to provide a combined
        //view
        Map combinedOptions = convertToMap(config);
        combinedOptions.putAll(options);

        LoginModuleProvider lmf = new ConfigLoginModuleProvider(realmName, className,
            combinedOptions, ControlFlag.from(flag).flag(), ranking, factory);

<<<<<<< HEAD
        ServiceRegistration reg = context.registerService(
            LoginModuleFactory.class.getName(), lmf, new Properties());
=======
        ServiceRegistration reg = context.registerService(LoginModuleFactory.class.getName(), lmf, null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        ServiceRegistration oldReg = registrations.put(pid, reg);

        //Remove earlier registration if any
        if(oldReg != null)
        {
            oldReg.unregister();
        }
    }

    @Override
    public void deleted(String pid)
    {
        ServiceRegistration reg = registrations.remove(pid);
        if (reg != null)
        {
            reg.unregister();
        }
    }

    //~----------------------------------- Utility Methods
    @SuppressWarnings("unchecked")
    private static Map convertToMap(Dictionary config)
    {
        Map copy = new HashMap();
        Enumeration e = config.keys();
        while (e.hasMoreElements())
        {
            Object key = e.nextElement();
            Object value = config.get(key);
            copy.put(key, value);
        }
        return copy;
    }
}
