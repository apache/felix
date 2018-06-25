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
package org.apache.felix.systemready.osgi;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.felix.systemready.Status;
import org.apache.felix.systemready.SystemReadyCheck;
import org.apache.felix.systemready.impl.ComponentsCheck;
import org.apache.felix.systemready.osgi.examples.CompWithoutService;
import org.apache.felix.systemready.osgi.examples.CompWithoutService2;
import org.apache.felix.systemready.osgi.util.BaseTest;
import org.apache.felix.systemready.osgi.util.BndDSOptions;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(PaxExam.class)
public class ComponentsCheckTest extends BaseTest {

    @Inject
    @Filter("(component.name=" + ComponentsCheck.PID + ")")
    SystemReadyCheck check;
    
    @Inject
    ConfigurationAdmin configAdmin;

    @Configuration
    public Option[] configuration() {
        return new Option[] {
                baseConfiguration(),
                componentsCheckConfig("CompWithoutService", "CompWithoutService2"),
                BndDSOptions.dsBundle("test", bundle()
                        .add(CompWithoutService.class)
                        .add(CompWithoutService2.class)
                        )
        };
    }

    @Test
    public void test() throws IOException {
        Status status = check.getStatus();
        assertThat(status.getState(),  Matchers.is(Status.State.YELLOW));
        assertThat(status.getDetails(), containsString("unsatisfied references"));
        //configAdmin.getConfiguration("CompWithoutService").update();
        context.registerService(Runnable.class, () -> {}, null);
        Status status2 = check.getStatus();
        System.out.println(status2);
        assertThat(status2.getState(),  Matchers.is(Status.State.GREEN));
        assertThat(status2.getDetails(), containsString(" satisfied"));
    }
}
