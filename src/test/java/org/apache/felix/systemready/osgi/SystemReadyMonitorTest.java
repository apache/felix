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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.Status;
import org.apache.felix.systemready.SystemReadyCheck;
import org.apache.felix.systemready.SystemReadyMonitor;
import org.apache.felix.systemready.osgi.examples.TestSystemReadyCheck;
import org.apache.felix.systemready.osgi.util.BaseTest;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class SystemReadyMonitorTest extends BaseTest {

    @Inject
    SystemReadyMonitor monitor;

    private final ConditionFactory wait = await();

    @Configuration
    public Option[] configuration() throws MalformedURLException {
        return new Option[] {
                baseConfiguration(),
                monitorConfig()
        };
    }

    @Test
    public void test() throws InterruptedException {
        disableFrameworkStartCheck();

        Awaitility.setDefaultPollDelay(0, TimeUnit.MILLISECONDS);
        assertNumChecks(0);
        wait.until(monitor::isReady, is(true));

        TestSystemReadyCheck check = new TestSystemReadyCheck();
        context.registerService(SystemReadyCheck.class, check, null);
        assertNumChecks(1);
        wait.until(monitor::isReady, is(false));

        // make the status green
        check.setInternalState(Status.State.GREEN);
        wait.until(monitor::isReady, is(true));

        // make the status fail and check that the monitor handles that
        check.exception();
        wait.until(monitor::isReady, is(false));
        assertNumChecks(1);

        CheckStatus status = monitor.getStatus().getCheckStates().iterator().next();
        assertThat(status.getCheckName(), is(check.getClass().getName()));
        assertThat(status.getStatus().getState(), Matchers.is(Status.State.RED));
        assertThat(status.getStatus().getDetails(), containsString("Failure"));

        check.setInternalState(Status.State.RED);
        assertNumChecks(1);
        wait.until(monitor::isReady, is(false));

        // register a second check
        TestSystemReadyCheck check2 = new TestSystemReadyCheck();
        context.registerService(SystemReadyCheck.class, check2, null);
        assertNumChecks(2);
        wait.until(monitor::isReady, is(false));

        check2.setInternalState(Status.State.GREEN);
        wait.until(monitor::isReady, is(false));

        check.setInternalState(Status.State.GREEN);
        wait.until(monitor::isReady, is(true));

    }

    private void assertNumChecks(int expectedNum) {
        wait.until(this::numChecks, is(expectedNum));
    }

    private int numChecks() {
        return monitor.getStatus().getCheckStates().size();
    }
}
