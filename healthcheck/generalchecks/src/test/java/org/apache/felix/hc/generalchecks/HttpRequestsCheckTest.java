/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.generalchecks;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.generalchecks.HttpRequestsCheck.RequestSpec;
import org.apache.felix.hc.generalchecks.HttpRequestsCheck.ResponseCheck.ResponseCheckResult;
import org.junit.Test;
import org.mockito.Mockito;

public class HttpRequestsCheckTest {

    FormattingResultLog log = new FormattingResultLog();

    HttpRequestsCheck.Response simple200HtmlResponse = new HttpRequestsCheck.Response(200, "OK", null, "<html><head><title>test</title></head><body>body text</body></html>", 200);

    @Test
    public void testRequestSpecParsing() throws Exception {
        
        HttpRequestsCheck.RequestSpec requestSpec = new HttpRequestsCheck.RequestSpec("/path/to/page.html");
        assertEquals("/path/to/page.html", requestSpec.url);
        assertEquals("GET", requestSpec.method);
        assertEquals(new HashMap<String,String>(), requestSpec.headers);
        assertNull(requestSpec.data);
        assertNull(requestSpec.user);
        assertNull(requestSpec.connectTimeoutInMs);
        assertNull(requestSpec.readTimeoutInMs);
        assertNull(requestSpec.proxy);
        
        requestSpec = new HttpRequestsCheck.RequestSpec("-X POST -H \"X-Test: Test\" -d \"{ 1,2,3 }\" -u admin:admin --connect-timeout 4 -m 5 --proxy http://proxy:2000 /path/to/page.html => 201");
        assertEquals("/path/to/page.html", requestSpec.url);
        assertEquals("POST", requestSpec.method);
        HashMap<String, String> expectedHeaders = new HashMap<String,String>();
        expectedHeaders.put("X-Test", "Test");
        expectedHeaders.put("Authorization", "Basic YWRtaW46YWRtaW4=");
        assertEquals(expectedHeaders, requestSpec.headers);
        assertEquals("{ 1,2,3 }", requestSpec.data);
        assertEquals("admin", requestSpec.user);
        assertEquals((Integer) 4000, requestSpec.connectTimeoutInMs);
        assertEquals((Integer) 5000, requestSpec.readTimeoutInMs);
        assertEquals("proxy:2000", requestSpec.proxy.address().toString());

    }
    
    @Test
    public void testSimpleRequestSpec() throws Exception {

        HttpRequestsCheck.RequestSpec requestSpec = new HttpRequestsCheck.RequestSpec("/path/to/page.html");
        Entry entry = fakeRequestForSpecAndReturnResponse(requestSpec, simple200HtmlResponse);
        assertEquals(Result.Status.OK, entry.getStatus());

        requestSpec = new HttpRequestsCheck.RequestSpec("/path/to/page.html => 200");
        entry = fakeRequestForSpecAndReturnResponse(requestSpec, simple200HtmlResponse);
        assertEquals(Result.Status.OK, entry.getStatus());

        requestSpec = new HttpRequestsCheck.RequestSpec("/path/to/page.html => 401");
        entry = fakeRequestForSpecAndReturnResponse(requestSpec, simple200HtmlResponse);
        assertEquals(Result.Status.WARN, entry.getStatus());
        assertThat(entry.getMessage(), containsString("200 (expected 401)"));
    }
    
    @Test
    public void testSimpleRequestSpecWithContentCheck() throws Exception {

        HttpRequestsCheck.RequestSpec requestSpec = new HttpRequestsCheck.RequestSpec("/path/to/page.html => 200 && MATCHES (body|other) text");
        Entry entry = fakeRequestForSpecAndReturnResponse(requestSpec, simple200HtmlResponse);
        assertEquals(Result.Status.OK, entry.getStatus());
        assertThat(entry.getMessage(), containsString("[200 OK], response matches [(body|other) text]"));

        requestSpec = new HttpRequestsCheck.RequestSpec("/path/to/page.html => 200 && MATCHES special text");
        entry = fakeRequestForSpecAndReturnResponse(requestSpec, simple200HtmlResponse);
        assertEquals(Result.Status.WARN, entry.getStatus());
        assertThat(entry.getMessage(), containsString("[200 OK], response does not match [special text]"));

    }

    private Entry fakeRequestForSpecAndReturnResponse(HttpRequestsCheck.RequestSpec requestSpecOrig, HttpRequestsCheck.Response response) throws Exception {
        RequestSpec requestSpec = Mockito.spy(requestSpecOrig);
        doReturn(response).when(requestSpec).performRequest(anyString(), anyString(), anyInt(), anyInt(), any(FormattingResultLog.class));
        FormattingResultLog resultLog = requestSpec.check("http://localhost:8080", 10000, 10000, Result.Status.WARN, true);
        Iterator<Entry> entryIt = resultLog.iterator();
        Entry lastEntry = null;
        while(entryIt.hasNext()) {
            lastEntry = entryIt.next();
        }
        return lastEntry;
    }

    
    @Test
    public void testJsonConstraint() {

        String testJson = "{\"test\": { \"intProp\": 2, \"arrProp\": [\"test1\",\"test2\",\"test3\",{\"deepProp\": \"deepVal\"}]} }";

        assertJsonResponse(testJson, "test.intProp = 2", true);
        assertJsonResponse(testJson, "test.arrProp[2] = test3", true);
        assertJsonResponse(testJson, "test.intProp between 1 and 3", true);
        assertJsonResponse(testJson, "test.arrProp[3].deepProp matches deep.*", true);
    }

    private void assertJsonResponse(String testJson, String jsonExpression, boolean expectedTrueOrFalse) {
        HttpRequestsCheck.JsonPropertyCheck jsonPropertyCheck = new HttpRequestsCheck.JsonPropertyCheck(jsonExpression);
        HttpRequestsCheck.Response response = new HttpRequestsCheck.Response(200, "OK", null, testJson, 200);
        ResponseCheckResult checkResult = jsonPropertyCheck.checkResponse(response, log);
        assertEquals("Expected "+expectedTrueOrFalse + " for expression ["+jsonExpression+"] against json: "+testJson, expectedTrueOrFalse, !checkResult.contraintFailed);
    }
    
    @Test
    public void testTimeConstraint() {
        assertTimeConstraint(1000, "< 2000", true);
        assertTimeConstraint(1000, "between 500 and 2000", true);
    }
    
    private void assertTimeConstraint(long time, String constraint, boolean expectedTrueOrFalse) {
        HttpRequestsCheck.ResponseTimeCheck responseTimeCheck = new HttpRequestsCheck.ResponseTimeCheck(constraint);
        HttpRequestsCheck.Response response = new HttpRequestsCheck.Response(200, "OK", null, "", time);
        ResponseCheckResult checkResult = responseTimeCheck.checkResponse(response, log);
        assertEquals("Expected "+expectedTrueOrFalse + " for expression ["+constraint+"] against json: "+time+"ms", expectedTrueOrFalse, !checkResult.contraintFailed);
    }
    
    @Test
    public void testSplitArgsRespectingQuotes() throws Exception {
    
        HttpRequestsCheck.RequestSpec requestSpec = new HttpRequestsCheck.RequestSpec("/page.html");
        String[] args = requestSpec.splitArgsRespectingQuotes("normal1 \"one two three\" normal2 'one two three' -p --words \"w1 w2 w3\"");
        assertArrayEquals(new String[] {"normal1", "\"one two three\"", "normal2", "'one two three'", "-p", "--words", "\"w1 w2 w3\""}, args);
    }


}
