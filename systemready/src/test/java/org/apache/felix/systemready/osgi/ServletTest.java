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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.CheckStatus.State;
import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyMonitor;
import org.apache.felix.systemready.osgi.util.BaseTest;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ServletTest extends BaseTest {
    public static final String READY_SERVLET_PATH = "/readyservlet/path";
	private static final String ALIVE_SERVLET_PATH = "/aliveservlet/path";
    @Inject
    SystemReadyMonitor monitor;

    @Configuration
    public Option[] configuration() {
        return new Option[] {
                baseConfiguration(),
                readyServletConfig(READY_SERVLET_PATH),
                aliveServletConfig(ALIVE_SERVLET_PATH),
                httpService(),
                monitorConfig(),
                servicesCheckConfig(StateType.ALIVE, Runnable.class.getName()),
                servicesCheckConfig(StateType.READY, Consumer.class.getName())
        };
    }

    @Test
    public void testServlets() throws IOException, InterruptedException {
        disableFrameworkStartCheck();

        Awaitility.pollInSameThread();
        
        waitState(StateType.READY, CheckStatus.State.YELLOW);
        await().until(() -> readFromUrl(getUrl(ALIVE_SERVLET_PATH), 503), containsString("\"systemStatus\": \"YELLOW\""));
        await().until(() -> readFromUrl(getUrl(READY_SERVLET_PATH), 503), containsString("\"systemStatus\": \"YELLOW\""));
        context.registerService(Runnable.class, () -> {}, null);
        waitState(StateType.ALIVE, State.GREEN);
        waitState(StateType.READY, State.YELLOW);

        await().until(() -> readFromUrl(getUrl(ALIVE_SERVLET_PATH), 200), containsString("\"systemStatus\": \"GREEN\""));
        await().until(() -> readFromUrl(getUrl(READY_SERVLET_PATH), 503), containsString("\"systemStatus\": \"YELLOW\""));

        context.registerService(Consumer.class, input -> {}, null);
        
        waitState(StateType.ALIVE, State.GREEN);
        waitState(StateType.READY, State.GREEN);
        
        await().until(() -> readFromUrl(getUrl(ALIVE_SERVLET_PATH), 200), containsString("\"systemStatus\": \"GREEN\""));
        await().until(() -> readFromUrl(getUrl(READY_SERVLET_PATH), 200), containsString("\"systemStatus\": \"GREEN\""));
    }
    
    

	private void waitState(StateType type, State expectedState) {
		Awaitility.await().until(() -> monitor.getStatus(type).getState(), is(expectedState));
	}
    
    private String getUrl(String path) {
        return URI.create("http://localhost:8080").resolve(path).toString();
    }

    private String readFromUrl(String address, int expectedCode) throws MalformedURLException, IOException {
        URL url = new URL(address);   
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        int code = urlConnection.getResponseCode();
        assertThat(code, equalTo(expectedCode));
        InputStream in = code == 200 ? urlConnection.getInputStream(): urlConnection.getErrorStream();
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader buffered = new BufferedReader(reader);
        String content = buffered.lines().collect(Collectors.joining("\n"));
        buffered.close();
        return content;
    }

}
