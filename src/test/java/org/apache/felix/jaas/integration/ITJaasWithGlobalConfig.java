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

import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

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
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITJaasWithGlobalConfig extends JaasTestBase
{

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
        return composite(
                streamBundle(createConfigBasedBundle())
        );
    }


    /**
     * Creates the scenario where jaas-boot jar is placed in bootclasspath. With this the client
     * code need not switch the TCCL
     */
    @Test
    public void testJaasWithGlobalConfig() throws Exception
    {
        String realmName = name.getMethodName();
        createLoginModuleConfig(realmName);

        //1. Configure the ConfigSpi to replace global config
        org.osgi.service.cm.Configuration config2 = ca.getConfiguration("org.apache.felix.jaas.ConfigurationSpi",null);
        Properties p2 = new Properties();
        p2.setProperty("jaas.globalConfigPolicy","replace");
        config2.update(p2);
        delay();

        //2. Validate the login passes with this config. Would not pass explicit config
        CallbackHandler handler = new SimpleCallbackHandler("foo","foo");

        Subject s = new Subject();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            LoginContext lc = new LoginContext(realmName,s,handler);
            lc.login();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(cl);
        }

        assertFalse(s.getPrincipals().isEmpty());
    }

}
