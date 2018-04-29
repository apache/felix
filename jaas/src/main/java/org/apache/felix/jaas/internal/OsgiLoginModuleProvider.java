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

<<<<<<< HEAD
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Collections;
import java.util.Map;

import javax.security.auth.spi.LoginModule;

import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

<<<<<<< HEAD
final class OsgiLoginModuleProvider implements LoginModuleProvider
{
    private final LoginModuleFactory delegate;
    private final int ranking;
    private final LoginModuleControlFlag flag;
    private final String realmName;
    private final ServiceReference serviceReference;

    public OsgiLoginModuleProvider(ServiceReference sr, LoginModuleFactory delegate)
    {
        this.delegate = delegate;
        this.ranking = PropertiesUtil.toInteger(sr.getProperty(Constants.SERVICE_RANKING), 0);
        this.flag = ControlFlag.from(
            (String) sr.getProperty(LoginModuleFactory.JAAS_CONTROL_FLAG)).flag();
        this.realmName = (String) sr.getProperty(LoginModuleFactory.JAAS_REALM_NAME);
        this.serviceReference = sr;
=======
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

final class OsgiLoginModuleProvider implements LoginModuleProvider
{
    private final LoginModuleFactory delegate;
    private final ServiceReference serviceReference;

    private int ranking;
    private LoginModuleControlFlag flag;
    private String realmName;

    public OsgiLoginModuleProvider(ServiceReference sr, LoginModuleFactory delegate)
    {
        this.delegate = delegate;
        this.serviceReference = sr;
        configure();
    }

    public void configure() {
        // FELIX-4389: Support jaas.ranking (in addition to service.ranking
        // to help ordering LoginModuleFactory instances
        Object rankingProperty = serviceReference.getProperty(LoginModuleFactory.JAAS_RANKING);
        if (rankingProperty == null) {
            rankingProperty = serviceReference.getProperty(Constants.SERVICE_RANKING);
        }
        this.ranking = PropertiesUtil.toInteger(rankingProperty, 0);
        this.flag = ControlFlag.from((String) serviceReference.getProperty(LoginModuleFactory.JAAS_CONTROL_FLAG)).flag();
        this.realmName = Util.trimToNull((String) serviceReference.getProperty(LoginModuleFactory.JAAS_REALM_NAME));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public Map<String, ?> options()
    {
        return Collections.emptyMap();
    }

    public LoginModuleControlFlag getControlFlag()
    {
        return flag;
    }

    public int ranking()
    {
        return ranking;
    }

    public String realmName()
    {
        return realmName;
    }

    public LoginModule createLoginModule()
    {
        return delegate.createLoginModule();
    }

    public String getClassName()
    {
        return delegate.getClass().getName();
    }

    public ServiceReference getServiceReference()
    {
        return serviceReference;
    }

    @Override
    public String toString()
    {
        return "OsgiLoginModuleProvider{" + "className=" + delegate.getClass().getName()
            + ", ranking=" + ranking + ", flag=" + flag + ", realmName='" + realmName
            + '\'' + '}';
    }
}
