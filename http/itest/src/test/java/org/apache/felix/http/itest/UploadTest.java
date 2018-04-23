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

package org.apache.felix.http.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class UploadTest extends BaseIntegrationTest
{
    private static final String PATH = "/post";

    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    private CountDownLatch initLatch;
    private CountDownLatch destroyLatch;
    private CountDownLatch receivedLatch;

    public void setupLatches(int count)
    {
        initLatch = new CountDownLatch(count);
        destroyLatch = new CountDownLatch(count);
        receivedLatch = new CountDownLatch(count);
    }

    public void setupServlet(final Map<String, Long> contents) throws Exception
    {
        setupLatches(1);

        Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
        servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, PATH);
        servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED, Boolean.TRUE);
        servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE, 1024L);

        TestServlet servletWithErrorCode = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws IOException, ServletException
            {
                try
                {
                    final Collection<Part> parts = req.getParts();
                    for(final Part p : parts)
                    {
                        contents.put(p.getName(), p.getSize());
                    }
                    resp.setStatus(201);
                }
                finally
                {
                    receivedLatch.countDown();
                }

            }
        };

        registrations.add(m_context.registerService(Servlet.class.getName(), servletWithErrorCode, servletProps));

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));
    }

    @After
    public void unregisterServices() throws InterruptedException
    {
        for (ServiceRegistration<?> serviceRegistration : registrations)
        {
            serviceRegistration.unregister();
        }

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
    }

    private static void postContent(final char c, final long length, final int expectedRT) throws IOException
    {
        final URL url = createURL(PATH);
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        try
        {
            final HttpPost httppost = new HttpPost(url.toExternalForm());

            final StringBuilder sb = new StringBuilder();
            for(int i=0;i<length;i++)
            {
                sb.append(c);
            }
            final StringBody text = new StringBody(sb.toString(), ContentType.TEXT_PLAIN);

            final HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("text", text)
                    .build();


            httppost.setEntity(reqEntity);

            final CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                final HttpEntity resEntity = response.getEntity();
                EntityUtils.consume(resEntity);
                assertEquals(expectedRT, response.getStatusLine().getStatusCode());
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }

    @Test
    public void testUpload() throws Exception
    {
        setupLatches(2);

        final Map<String, Long> contents = new HashMap<>();
        setupServlet(contents);

        postContent('a', 500, 201);
        assertTrue(receivedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(1, contents.size());
        assertEquals(500L, (long)contents.get("text"));
    }

    @Test
    public void testMaxFileSize() throws Exception
    {
        setupLatches(2);

        final Map<String, Long> contents = new HashMap<>();
        setupServlet(contents);

        postContent('b', 2048, 500);
        assertTrue(receivedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(contents.isEmpty());
    }
}
