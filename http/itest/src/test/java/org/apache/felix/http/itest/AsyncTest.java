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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class AsyncTest extends BaseIntegrationTest
{

    /**
     * Tests that we can use an asynchronous servlet (introduced in Servlet 3.0 spec).
     */
    @Test
    public void testAsyncServletOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                final AsyncContext asyncContext = req.startAsync(req, resp);
                asyncContext.setTimeout(2000);
                asyncContext.start(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            // Simulate a long running process...
                            Thread.sleep(1000);

                            HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                            response.setStatus(SC_OK);
                            response.getWriter().printf("Hello Async world!");

                            asyncContext.complete();
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("Hello Async world!", createURL("/test"));

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, createURL("/test"));
    }

    /**
     * Tests that we can use an asynchronous servlet (introduced in Servlet 3.0 spec) using the dispatching functionality.
     */
    @Test
    public void testAsyncServletWithDispatchOk() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);

        TestServlet servlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                DispatcherType dispatcherType = req.getDispatcherType();
                if (DispatcherType.REQUEST == dispatcherType)
                {
                    final AsyncContext asyncContext = req.startAsync(req, resp);
                    asyncContext.start(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                // Simulate a long running process...
                                Thread.sleep(1000);

                                asyncContext.getRequest().setAttribute("msg", "Hello Async world!");
                                asyncContext.dispatch();
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                }
                else if (DispatcherType.ASYNC == dispatcherType)
                {
                    String response = (String) req.getAttribute("msg");
                    resp.setStatus(SC_OK);
                    resp.getWriter().printf(response);
                    resp.flushBuffer();
                }
            }
        };

        register("/test", servlet);

        assertTrue(initLatch.await(5, TimeUnit.SECONDS));

        assertContent("Hello Async world!", createURL("/test"));

        unregister(servlet);

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        assertResponseCode(SC_NOT_FOUND, createURL("/test"));
    }
}
