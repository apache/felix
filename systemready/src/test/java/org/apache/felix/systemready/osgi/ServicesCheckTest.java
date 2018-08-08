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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.apache.felix.systemready.SystemReadyCheck;
import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.CheckStatus.State;
import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.impl.ServicesCheck;
import org.apache.felix.systemready.osgi.util.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

@RunWith(PaxExam.class)
public class ServicesCheckTest extends BaseTest {

    @Inject
    @Filter("(component.name=" + ServicesCheck.PID + ")")
    SystemReadyCheck check;

    @Configuration
    public Option[] configuration() {
        return new Option[] {
                baseConfiguration(),
                servicesCheckConfig(StateType.ALIVE, Runnable.class.getName(), ServiceComponentRuntime.class.getName()),
        };
    }

    @Test
    public void test() {
        CheckStatus status = check.getStatus();
        assertThat(status.getState(),  is(State.YELLOW));
        assertThat(status.getDetails(), containsString("Missing service without matching DS component: java.lang.Runnable"));
        context.registerService(Runnable.class, () -> {}, null);
        CheckStatus status2 = check.getStatus();
        System.out.println(status2);
        assertThat(status2.getState(),  is(State.GREEN));
        assertThat(status2.getDetails(), equalTo(""));
    }
}
