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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.felix.systemready.SystemReadyMonitor;
import org.apache.felix.systemready.osgi.util.BaseTest;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ServletTest extends BaseTest {
    public static final String SERVLET_PATH = "/servlet/path";
    @Inject
    SystemReadyMonitor monitor;

    @Configuration
    public Option[] configuration() {
        return new Option[] {
                baseConfiguration(),
                servletConfig(SERVLET_PATH),
                httpService(),
                monitorConfig(),
                servicesCheckConfig(Runnable.class.getName())
        };
    }

    @Test
    public void test() throws IOException, InterruptedException {
        disableFrameworkStartCheck();

        Awaitility.pollInSameThread();
        Awaitility.await().until(monitor::isReady, is(false));
        String content = Awaitility.await().until(() -> readFromUrl(getUrl(SERVLET_PATH), 503), notNullValue());
        System.out.println(content);
        assertThat(content, containsString("\"systemStatus\": \"YELLOW\""));
        context.registerService(Runnable.class, () -> {}, null);
        System.out.println(content);
        Awaitility.await().until(monitor::isReady, is(true));
        String content2 = Awaitility.await().until(() -> readFromUrl(getUrl(SERVLET_PATH), 200), notNullValue());
        assertThat(content2, containsString("\"systemStatus\": \"GREEN\""));
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
