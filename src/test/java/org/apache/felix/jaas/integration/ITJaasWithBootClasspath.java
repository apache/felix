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

import java.io.File;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
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
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.vmOption;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITJaasWithBootClasspath extends JaasTestBase
{

    // the default boot jar file name
    protected static final String BOOT_JAR_DEFAULT = "target/jaas-boot.jar";

    // the name of the system property providing the bundle file to be installed and tested
    protected static final String BOOT_JAR_SYS_PROP = "project.boot.file";

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
        final String bundleFileName = System.getProperty(BOOT_JAR_SYS_PROP,
                BOOT_JAR_DEFAULT);
        final File bundleFile = new File(bundleFileName);
        if (!bundleFile.canRead())
        {
            throw new IllegalArgumentException("Cannot read from boot file "
                    + bundleFileName + " specified in the " + BUNDLE_JAR_SYS_PROP
                    + " system property");
        }
        return composite(
                vmOption("-Xbootclasspath/a:"+bundleFile.getAbsolutePath()),
                bootDelegationPackage("org.apache.felix.jaas.boot"),
                streamBundle(createConfigBasedBundle())
        );
    }


    /**
     * Creates the scenario where jaas-boot jar is placed in bootclasspath. With this the client
     * code need not switch the TCCL
     */
    @Test
    public void testJaasWithBoot() throws Exception
    {
        String realmName = name.getMethodName();
        createLoginModuleConfig(realmName);
        delay();

        CallbackHandler handler = new SimpleCallbackHandler("foo", "foo");
        Configuration config = Configuration.getInstance("JavaLoginConfig", null,"FelixJaasProvider");
        Subject s = new Subject();

        LoginContext lc = new LoginContext(realmName, s, handler, config);
        lc.login();

        assertFalse(s.getPrincipals().isEmpty());
    }

}
