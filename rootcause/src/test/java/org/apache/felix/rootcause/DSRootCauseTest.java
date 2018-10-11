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
package org.apache.felix.rootcause;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import javax.inject.Inject;

import org.apache.felix.rootcause.DSComp;
import org.apache.felix.rootcause.DSRef;
import org.apache.felix.rootcause.DSRootCause;
import org.apache.felix.rootcause.RootCausePrinter;
import org.apache.felix.rootcause.examples.CompWithCyclicRef;
import org.apache.felix.rootcause.examples.CompWithMissingConfig;
import org.apache.felix.rootcause.examples.CompWithMissingRef;
import org.apache.felix.rootcause.examples.CompWithMissingRef2;
import org.apache.felix.rootcause.examples.CompWithoutService;
import org.apache.felix.rootcause.util.BaseTest;
import org.apache.felix.rootcause.util.BndDSOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DSRootCauseTest extends BaseTest {

    @Inject
    ServiceComponentRuntime scr;
    
    DSRootCause dsRootCause;
    
    @Configuration
    public Option[] configuration() {
        return new Option[] {
                baseConfiguration(),
                BndDSOptions.dsBundle("test", bundle()
                        .add(CompWithMissingConfig.class)
                        .add(CompWithMissingRef.class)
                        .add(CompWithCyclicRef.class)
                        .add(CompWithMissingRef2.class)
                        .add(CompWithoutService.class)
                        )
        };
    }
    
    @Before
    public void before() {
        dsRootCause = new DSRootCause(scr);
    }

    
    @Test
    public void testMissingConfig() throws InterruptedException {
        ComponentDescriptionDTO desc = getComponentDesc(CompWithMissingConfig.class);
        DSComp rootCause = dsRootCause.getRootCause(desc);
        new RootCausePrinter().print(rootCause);
        assertEquals("CompWithMissingConfig", rootCause.desc.name);
        assertNull(rootCause.config);
    }
    
    @Test
    public void testMissingRef() throws InterruptedException {
        ComponentDescriptionDTO desc = getComponentDesc(CompWithMissingRef.class);
        DSComp rootCause = dsRootCause.getRootCause(desc);
        new RootCausePrinter().print(rootCause);
        assertEquals(1, rootCause.unsatisfied.size());
        DSRef unsatisfied = rootCause.unsatisfied.iterator().next();
        assertEquals(1, rootCause.unsatisfied.size());
        DSComp candidate = unsatisfied.candidates.iterator().next();
        assertEquals("CompWithMissingConfig", candidate.desc.name);
        assertNull(candidate.config);
    }
    
    @Test
    public void testMissingRef2() throws InterruptedException {
        ComponentDescriptionDTO desc = getComponentDesc(CompWithMissingRef2.class);
        DSComp rootCause = dsRootCause.getRootCause(desc);
        new RootCausePrinter().print(rootCause);
        assertEquals(1, rootCause.unsatisfied.size());
        DSRef unsatisfied = rootCause.unsatisfied.iterator().next();
        assertEquals(1, rootCause.unsatisfied.size());
        DSComp candidate = unsatisfied.candidates.iterator().next();
        assertEquals("CompWithMissingRef", candidate.desc.name);
        assertNull(candidate.config);
    }
    
    @Test
    public void testNoService() throws InterruptedException {
        ComponentDescriptionDTO desc = getComponentDesc(CompWithoutService.class);
        DSComp rootCause = dsRootCause.getRootCause(desc);
        assertThat(rootCause.desc.serviceInterfaces.length, equalTo(0)); 
        new RootCausePrinter().print(rootCause);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testCyclic() throws InterruptedException {
        ComponentDescriptionDTO desc = getComponentDesc(CompWithCyclicRef.class);
        dsRootCause.getRootCause(desc);
    }

}
