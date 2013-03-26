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

package org.apache.felix.example.jaas.jdbc;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.spi.LoginModule;
import javax.sql.DataSource;

import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@Component(label = "%jdbc.name",
        description = "%jdbc.description",
        metatype = true,
        name = JdbcLoginModuleFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class JdbcLoginModuleFactory implements LoginModuleFactory
{

    public static final String SERVICE_PID = " org.apache.felix.example.jaas.jdbc.factory";

    @Property(value = "required", options = {
            @PropertyOption(name = "required", value = "%jaas.flag.required"),
            @PropertyOption(name = "requisite", value = "%jaas.flag.requisite"),
            @PropertyOption(name = "sufficient", value = "%jaas.flag.sufficient"),
            @PropertyOption(name = "optional", value = "%jaas.flag.optional") })
    static final String JAAS_CONTROL_FLAG = "jaas.controlFlag";

    @Property(intValue = 0)
    static final String JAAS_RANKING = "jaas.ranking";

    @Property
    private static final String PROP_REALM = "jaas.realmName";

    private static final String DEFAULT_PWD_QUERY = "SELECT PASSWORD FROM USERS WHERE USERNAME=?";
    @Property(value = DEFAULT_PWD_QUERY)
    private static final String PROP_PWD_QUERY = "query.pwd";
    private String passwordQuery;

    private static final String DEFAULT_ROLE_QUERY = "SELECT ROLE FROM ROLES WHERE USERNAME=?";
    @Property(value = DEFAULT_ROLE_QUERY)
    private static final String PROP_ROLE_QUERY = "query.role";
    private String roleQuery;

    private static final String DEFAULT_DS_NAME = "test";
    @Property
    private static final String PROP_DS_NAME = "datasourceName";
    private String datasourceName;
    private ServiceTracker dataSourceTracker;

    private ServiceRegistration loginModuleFactoryReg;

    @Activate
    public void activate(BundleContext context, Map<String, ?> conf)
        throws InvalidSyntaxException
    {
        passwordQuery = PropertiesUtil.toString(conf.get(PROP_PWD_QUERY),
            DEFAULT_PWD_QUERY);
        roleQuery = PropertiesUtil.toString(conf.get(PROP_ROLE_QUERY), DEFAULT_ROLE_QUERY);
        datasourceName = PropertiesUtil.toString(conf.get(PROP_DS_NAME), DEFAULT_DS_NAME);

        Filter filter = context.createFilter("(&(objectClass=javax.sql.DataSource)"
            + "(dataSourceName=" + datasourceName + "))");
        dataSourceTracker = new ServiceTracker(context, filter, null);
        dataSourceTracker.open();
        registerLoginModuleFactory(context, conf);
    }

    @Deactivate
    private void deactivate()
    {
        if (loginModuleFactoryReg != null)
        {
            loginModuleFactoryReg.unregister();
        }

        if(dataSourceTracker != null)
        {
            dataSourceTracker.close();
        }
    }

    private void registerLoginModuleFactory(BundleContext context, Map<String, ?> config)
    {
        Dictionary<String,Object> lmProps = new Hashtable<String,Object>();

        String controlFlag = PropertiesUtil.toString(config.get(JAAS_CONTROL_FLAG),
            "required");
        lmProps.put(LoginModuleFactory.JAAS_CONTROL_FLAG,controlFlag);
        lmProps.put(LoginModuleFactory.JAAS_REALM_NAME, PropertiesUtil.toString(config.get(PROP_REALM), null));
        lmProps.put(Constants.SERVICE_RANKING,
            PropertiesUtil.toInteger(config.get(JAAS_RANKING), 0));

        loginModuleFactoryReg = context.registerService(
            LoginModuleFactory.class.getName(), this, lmProps);
    }

    @Override
    public LoginModule createLoginModule()
    {
        return new JdbcLoginModule(
            (DataSource) dataSourceTracker.getService(), passwordQuery, roleQuery);
    }
}
