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

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import java.util.Collections;
import java.util.Map;

import javax.security.auth.spi.LoginModule;


final class ConfigLoginModuleProvider implements LoginModuleProvider
{
    private final Map options;
    private final LoginModuleControlFlag controlFlag;
    private final int ranking;
    private final String realmName;
    private final String className;
    private final LoginModuleCreator moduleCreator;

    @SuppressWarnings("unchecked")
    ConfigLoginModuleProvider(String realmName, String className, Map options,
                              LoginModuleControlFlag controlFlag, int order,
                              LoginModuleCreator moduleCreator)
    {
        this.options = Collections.unmodifiableMap(options);
        this.controlFlag = controlFlag;
        this.ranking = order;
        this.realmName = realmName;
        this.className = className;
        this.moduleCreator = moduleCreator;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ?> options()
    {
        return options;
    }

    public LoginModuleControlFlag getControlFlag()
    {
        return controlFlag;
    }

    public int ranking()
    {
        return ranking;
    }

    public String realmName()
    {
        return realmName;
    }

    public String getClassName()
    {
        return className;
    }

    public LoginModule createLoginModule()
    {
        return moduleCreator.newInstance(className);
    }

    @Override
    public String toString()
    {
        return "ConfigLoginModuleProvider{" + "flag=" + controlFlag + ", ranking="
            + ranking + ", realmName='" + realmName + '\'' + ", className='" + className
            + '\'' + '}';
    }
}
