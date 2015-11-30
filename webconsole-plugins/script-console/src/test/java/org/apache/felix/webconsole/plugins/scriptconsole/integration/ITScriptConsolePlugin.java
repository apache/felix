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

package org.apache.felix.webconsole.plugins.scriptconsole.integration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.exam.junit.PaxExamServer;

public class ITScriptConsolePlugin
{
    private DefaultHttpClient httpClient = new DefaultHttpClient();
    private RequestExecutor executor = new RequestExecutor(httpClient);

    @Rule
    public PaxExamServer exam = new PaxExamServer(ServerConfiguration.class);

    @Test
    public void testScriptExecution() throws Exception
    {
          //Somehow multipart data based invocation does not work in with old webconsole
//        execute(new InputStreamBody(getClass().getResourceAsStream("/test.groovy"),
//            "test.groovy"));
//        execute(new StringBody("def a = 2+2; assert a == 4;"));
        InputStream is = getClass().getResourceAsStream("/test.groovy");
        try{
            execute2(IOUtils.toString(is, "utf-8"));
        }finally {
            IOUtils.closeQuietly(is);
        }
        execute2("def a = 2+2; assert a == 4;");
    }

    private void execute(ContentBody code) throws Exception
    {
        RequestBuilder rb = new RequestBuilder(ServerConfiguration.getServerUrl());

        final MultipartEntity entity = new MultipartEntity();
        // Add Sling POST options
        entity.addPart("lang", new StringBody("groovy"));
        entity.addPart("code", code);
        executor.execute(
            rb.buildPostRequest("/system/console/sc").withEntity(entity).withCredentials(
                    "admin", "admin")).assertStatus(200);
    }

    private void execute2(String code) throws Exception
    {
        RequestBuilder rb = new RequestBuilder(ServerConfiguration.getServerUrl());

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("lang","groovy"));
        params.add(new BasicNameValuePair("code",code));

        final HttpEntity entity = new UrlEncodedFormEntity(params);
        // Add Sling POST options
        executor.execute(
                rb.buildPostRequest("/system/console/sc")
                        .withEntity(entity)
                        .withCredentials("admin", "admin"))
                .assertStatus(200);
    }

}
