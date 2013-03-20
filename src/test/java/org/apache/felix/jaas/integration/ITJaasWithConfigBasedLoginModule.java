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

import java.io.IOException;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
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
        createConfigSpiConfig();
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
        createConfigSpiConfig();
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
}
