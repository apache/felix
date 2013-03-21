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

import javax.inject.Inject;
import javax.servlet.Servlet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITJaasWebConsolePlugin extends JaasTestBase
{
    @Rule
    public TestName name= new TestName();

    @Inject
    @Filter("(&(felix.webconsole.label=*)(felix.webconsole.title=*)(felix.webconsole.configprinter.modes=*))")
    private Servlet servlet;

    static
    {
        // uncomment to enable debugging of this test class
//                paxRunnerVmOption = DEBUG_VM_OPTION;

    }

    @Override
    protected Option addExtraOptions()
    {
        return composite(
                mavenBundle("org.apache.felix","org.apache.felix.http.bundle").versionAsInProject(),
                streamBundle(createConfigBasedBundle())
        );
    }


    /**
     * Checks the presence of plugin servlet if Servlet API is present
     */
    @Test
    public void testJaasPlugin() throws Exception
    {
       assertNotNull(servlet);
    }

}
