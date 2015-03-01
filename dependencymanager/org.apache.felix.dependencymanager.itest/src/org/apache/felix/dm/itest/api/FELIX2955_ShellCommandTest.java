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
package org.apache.felix.dm.itest.api;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX2955_ShellCommandTest extends TestBase {
    private long m_myBundleId;
    private Bundle m_testBundle;

    public void testShellCommands() throws Throwable {
    	try {
    		m_myBundleId = context.getBundle().getBundleId();
    		for (Bundle b : context.getBundles()) {
    			if (b.getSymbolicName().equals("org.apache.felix.dependencymanager.itest.bundle")) {
    				m_testBundle = b;
    				b.stop();
    				break;
    			}
    		}
    		DependencyManager m = getDM();
    		// helper class that ensures certain steps get executed in sequence
    		Ensure e = new Ensure();
        
    		Component shellClient = m.createComponent();
    		Component missing = m.createComponent();
        
    		long shellClientId = shellClient.getComponentDeclaration().getId();
    		long missingId = missing.getComponentDeclaration().getId();
    		shellClient.setImplementation(new ShellClient(e, shellClientId, missingId))
    				   .add(m.createServiceDependency()
    						   .setService(CommandProcessor.class)
    						   .setRequired(true));
            
    		m.add(shellClient);
    		e.waitForStep(3, 5000);
    		// now create a component with a missing dependency
    		missing.setImplementation(new Object() { public String toString() { return "Object"; }})
    			   .add(m.createServiceDependency()
    					 .setService(Missing.class) // Warning: don't use Object, or Runnable, which are already registered by bndtools ?
    					 .setRequired(true));
            
    		m.add(missing);
    		e.step(4);
    		e.waitForStep(5, 5000);
    		m.remove(missing);
    		// now start/stop our test bundle, which publishes a service that uses the dependency manager
    		m_testBundle.start();    		
    		m_testBundle.stop();
    		e.step(6);
    		e.waitForStep(7, 5000);
    		e.ensure();
    		m.remove(shellClient);
    		m_testBundle.start(); // restart the runtime bundle
    		m.clear();
    	} 
    	
    	catch (Throwable t) {
    		error("test failed", t);
    	}
    }
    
    public class ShellClient {
        volatile CommandProcessor m_commandProcessor;
        private final Ensure m_ensure;
        private final long m_shellClientId;
        private final long m_missingId;
        
        public ShellClient(Ensure e, long shellClientId, long missingId) {
            m_ensure = e;
            m_shellClientId = shellClientId;
            m_missingId = missingId;
        }

        public void start() throws InterruptedException {
            Thread t = new Thread("Shell Client") {
                public void run() {
                    String bsn = context.getBundle().getSymbolicName();
                    m_ensure.step(1);
                    execute("dm bid " + m_myBundleId,
                          "[" + m_myBundleId + "] " + bsn + "\n" +
                          " [" + m_shellClientId + "] ShellClient registered\n" +
                          "    org.apache.felix.service.command.CommandProcessor service required available\n", 
                          "");
                    
                    m_ensure.step(2);
                    // see if there's anything that's not available
                    execute("dm notavail bid " + m_myBundleId,
                        "", 
                        "");
                    m_ensure.step(3);
                    // check again, now there should be something missing
                    m_ensure.waitForStep(4, 5000);
                    execute("dm notavail bid " + m_myBundleId,
                            "[" + m_myBundleId + "] " + bsn + "\n" +
                            " [" + m_missingId + "] Object unregistered\n" + 
                            "    " + Missing.class.getName() + " service required unavailable\n", 
                            "");
                    m_ensure.step(5);
                    m_ensure.waitForStep(6, 5000);
                    // this next step actually triggers the bug in FELIX-2955
                    execute("dm notavail bid " + m_myBundleId,
                        "", 
                        "");
                    m_ensure.step(7);
                }
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
                CommandSession session = m_commandProcessor.createSession(System.in, new PrintStream(output), new PrintStream(error));
                session.execute(command); 
                
                String out = output.toString();                                                                
                Assert.assertEquals(expectedOutput, out.toString());
                Assert.assertEquals(expectedError, error.toString());
            }
            catch (Throwable throwable) {
                m_ensure.throwable(throwable);
            }
        }
    }
    
    public static class Missing {       
    }
}
