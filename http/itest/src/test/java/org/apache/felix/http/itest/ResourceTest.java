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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.service.http.HttpContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ResourceTest extends BaseIntegrationTest
{

    @Test
    public void testHandleResourceRegistrationOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        HttpContext context = new HttpContext()
        {
            @Override
            public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                return true;
            }

            @Override
            public URL getResource(String name)
            {
                try
                {
                    File f = new File("src/test/resources/" + name);
                    if (f.exists())
                    {
                        return f.toURI().toURL();
                    }
                }
                catch (MalformedURLException e)
                {
                    fail();
                }
                return null;
            }

            @Override
            public String getMimeType(String name)
            {
                return null;
            }
        };

        TestServlet servlet = new TestServlet(initLatch, destroyLatch);

        register("/", "/resource", context);
        register("/test", servlet, context);

        URL testHtmlURL = createURL("/test.html");
        URL testURL = createURL("/test");

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_OK, testHtmlURL);
        assertResponseCode(SC_OK, testURL);

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_OK, testHtmlURL);
        assertResponseCode(SC_OK, testURL);

        unregister("/");

        assertResponseCode(SC_NOT_FOUND, testHtmlURL);
        assertResponseCode(SC_NOT_FOUND, testURL);
    }
}
