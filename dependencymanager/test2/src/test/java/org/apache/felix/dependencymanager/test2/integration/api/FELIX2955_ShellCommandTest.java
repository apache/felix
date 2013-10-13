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
package org.apache.felix.dependencymanager.test2.integration.api;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.Assert;

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.shell.ShellService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
public class FELIX2955_ShellCommandTest extends TestBase {
    private long m_testBundleId;
    private Bundle m_deploymentAdmin;

    @Test
    public void testShellCommands() throws Throwable {
        m_testBundleId = context.getBundle().getBundleId();
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals("org.apache.felix.deploymentadmin")) {
                m_deploymentAdmin = b;
                break;
            }
        }
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        Component shellClient = m.createComponent()
            .setImplementation(new ShellClient(e))
            .add(m.createServiceDependency()
                .setService(ShellService.class)
                .setRequired(true)
            );
        m.add(shellClient);
        e.waitForStep(3, 5000);
        // now create a component with a missing dependency
        Component missing = m.createComponent()
            .setImplementation(new Object() { public String toString() { return "Object"; }})
            .add(m.createServiceDependency()
                .setService(Object.class)
                .setRequired(true)
            );
        m.add(missing);
        e.step(4);
        e.waitForStep(5, 5000);
        m.remove(missing);
        // now start/stop deploymentadmin, which we use here because it's a bundle that
        // publishes a service that uses the dependency manager (saving us from having to
        // create a bundle that does that on the fly)
        m_deploymentAdmin.start();
        m_deploymentAdmin.stop();
        e.step(6);
        e.waitForStep(7, 5000);
        e.ensure();
        m.remove(shellClient);
        
    }
    
    public class ShellClient {
        volatile ShellService m_shell;
        private final Ensure m_ensure;
        
        public ShellClient(Ensure e) {
            m_ensure = e;
        }

        public void start() {
            Thread t = new Thread("Shell Client") {
                public void run() {
                    m_ensure.step(1);
                    execute("dm " + m_testBundleId,
                        "[" + m_testBundleId + "] pax-exam-probe\n" +
                        "  ShellClient registered\n" +
                        "    org.apache.felix.shell.ShellService service required available\n", 
                        "");
                    m_ensure.step(2);
                    // see if there's anything that's not available
                    execute("dm notavail " + m_testBundleId,
                        "", 
                        "");
                    m_ensure.step(3);
                    // check again, now there should be something missing
                    m_ensure.waitForStep(4, 5000);
                    execute("dm notavail " + m_testBundleId,
                        "[" + m_testBundleId + "] pax-exam-probe\n" + 
                        "  Object unregistered\n" + 
                        "    java.lang.Object service required unavailable\n", 
                        "");
                    m_ensure.step(5);
                    m_ensure.waitForStep(6, 5000);
                    // this next step actually triggers the bug in FELIX-2955
                    execute("dm notavail " + m_testBundleId,
                        "", 
                        "");
                    m_ensure.step(7);
                };
            };
            t.start();
        }
        
        @Override
        public String toString() {
            return "ShellClient";
        }
        
        public void execute(String command, String expectedOutput, String expectedError) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ByteArrayOutputStream error = new ByteArrayOutputStream();
                m_shell.executeCommand(command, new PrintStream(output), new PrintStream(error)); 
                // In pax-exam 3.0.0, we have to work around something like "[25] PAXEXAM-PROBE-3f88597d-4bc5-4bf4-affb-74db4e453e71 ..." 
                Assert.assertEquals(expectedOutput, output.toString().replaceAll("PAXEXAM-PROBE.*", "pax-exam-probe"));
                Assert.assertEquals(expectedError, error.toString());
            }
            catch (Throwable throwable) {
                m_ensure.throwable(throwable);
            }
        }
    }
}
