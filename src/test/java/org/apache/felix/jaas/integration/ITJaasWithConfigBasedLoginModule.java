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

package org.apache.felix.jaas.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.felix.jaas.LoginContextFactory;
import org.apache.felix.jaas.integration.common.SimpleCallbackHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITJaasWithConfigBasedLoginModule extends JaasTestBase
{

    @Inject
    private LoginContextFactory loginContextFactory;

    @Rule
    public TestName name= new TestName();

    static
    {
        // uncomment to enable debugging of this test class
//                paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addExtraOptions()
    {
        return composite(streamBundle(createConfigBasedBundle()));
    }

    @Test
    public void testJaasWithTCCL() throws Exception {
        String realmName = name.getMethodName();
        createLoginModuleConfig(realmName);
        delay();

        CallbackHandler handler = new SimpleCallbackHandler("foo","foo");
        Configuration config = Configuration.getInstance("JavaLoginConfig",null,"FelixJaasProvider");

        Subject s = new Subject();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            LoginContext lc = new LoginContext(realmName,s,handler,config);
            lc.login();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(cl);
        }

        assertFalse(s.getPrincipals().isEmpty());
    }

    @Test
    public void testJaasWithFactory() throws Exception
    {
        String realmName = name.getMethodName();
        createLoginModuleConfig(realmName);
        delay();

        CallbackHandler handler = new SimpleCallbackHandler("foo", "foo");

        Subject s = new Subject();

        //Using LoginFactory we can avoid providing Configuration and switching TCCL
        LoginContext lc = loginContextFactory.createLoginContext(realmName, s, handler);
        lc.login();

        assertFalse(s.getPrincipals().isEmpty());

        //Negative case. Login fails with incorrect password
        try
        {
            LoginContext lc2 = loginContextFactory.createLoginContext(realmName, s,
                    new SimpleCallbackHandler("foo", "bar"));
            lc2.login();
            fail("Login should have failed");
        }catch(LoginException e){

        }
    }

    /**
     * Validates that OSGi config do gets passed as part of options to the LoginModule
     */
    @Test
    public void testJaasConfigPassing() throws Exception {
        String realmName = name.getMethodName();

        //1. Create sample config
        org.osgi.service.cm.Configuration config =
                ca.createFactoryConfiguration("org.apache.felix.jaas.Configuration.factory",null);
        Dictionary<String,Object> p = new Hashtable<String, Object>();
        p.put("jaas.classname","org.apache.felix.jaas.integration.sample1.ConfigLoginModule");
        p.put("jaas.realmName", realmName);

        //Following passed config gets validated in
        //org.apache.felix.jaas.integration.sample1.ConfigLoginModule.validateConfig()
        p.put("validateConfig", Boolean.TRUE);
        p.put("key0", "val0");
        p.put("key1", "val1");
        p.put("key2", "val2");

        //Override the value directly passed in config via options value explicitly
        p.put("jaas.options", new String[]{"key3=val3", "key4=val4", "key0=valNew"});
        config.update(p);

        delay();

        //2. Validate the login passes with this config. LoginModule would validate
        //the config also
        CallbackHandler handler = new SimpleCallbackHandler("foo","foo");
        Configuration jaasConfig = Configuration.getInstance("JavaLoginConfig",null,"FelixJaasProvider");

        Subject s = new Subject();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            LoginContext lc = new LoginContext(realmName,s,handler,jaasConfig);
            lc.login();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(cl);
        }

        assertFalse(s.getPrincipals().isEmpty());
    }


    @Test
    public void testJaasConfigOrderedViaRanking() throws Exception {
        String realmName = name.getMethodName();
        List<Integer> ranks = Arrays.asList(1,2,3,4,5,6);
        Collections.shuffle(ranks);

        //1. Create LoginModule config with random rankings
        for(Integer i : ranks)
        {
            org.osgi.service.cm.Configuration config =
                    ca.createFactoryConfiguration("org.apache.felix.jaas.Configuration.factory",null);
            Dictionary<String,Object> p = new Hashtable<String, Object>();
            p.put("jaas.classname","org.apache.felix.jaas.integration.sample1.ConfigLoginModule");
            p.put("jaas.realmName", realmName);
            p.put("jaas.ranking", i);
            p.put("order", i);

            config.update(p);
        }

        delay();

        Configuration jaasConfig = Configuration.getInstance("JavaLoginConfig",null,"FelixJaasProvider");
        AppConfigurationEntry[] entries = jaasConfig.getAppConfigurationEntry(realmName);

        assertEquals("No of entries does not match the no of created",ranks.size(),entries.length);

        //Entries would be sorted via ranking. Higher ranking comes first
        int ranking = 6;
        for(AppConfigurationEntry e : entries){
            Integer order = (Integer) e.getOptions().get("order");
            assertEquals(ranking--,order.intValue());
        }

    }

    @Test
    public void testJaasConfigWithEmptyRealm() throws Exception {
        String realmName = name.getMethodName();

        //Scenario 1 - Create a config with no realm name set. So its default name would
        //be set to the defaultRealmName setting of ConfigurationSpi. Which defaults to 'other'
        org.osgi.service.cm.Configuration config =
                ca.createFactoryConfiguration("org.apache.felix.jaas.Configuration.factory",null);
        Dictionary<String,Object> dict = new Hashtable<String, Object>();
        dict.put("jaas.classname", "org.apache.felix.jaas.integration.sample1.ConfigLoginModule");

        config.update(dict);
        delay();

        CallbackHandler handler = new SimpleCallbackHandler("foo","foo");

        Subject s = new Subject();
        LoginContext lc = loginContextFactory.createLoginContext(realmName, s, handler);
        lc.login();

        assertFalse(s.getPrincipals().isEmpty());


        //Scenario 2 - Now we change the default realm name to 'default' and we do not have any login module which
        //is bound to 'other' as they get part of 'default'. In this case login should fail
        org.osgi.service.cm.Configuration config2 = ca.getConfiguration("org.apache.felix.jaas.ConfigurationSpi",null);
        Properties p2 = new Properties();
        p2.setProperty("jaas.defaultRealmName","default");
        config2.update(p2);
        delay();

        try{
            Subject s2 = new Subject();
            LoginContext lc2 = loginContextFactory.createLoginContext(realmName, s2, handler);
            lc2.login();
            fail("Should have failed as no LoginModule bound with 'other'");
        }catch(LoginException e){

        }
    }

}
