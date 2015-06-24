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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;

/**
 * Test cases for all supported event listeners.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class EventListenerTest extends BaseIntegrationTest
{
    /**
     * Tests that {@link HttpSessionListener}s are called whenever a session is created or destroyed.
     */
    @Test
    public void testHttpSessionListenerOk() throws Exception
    {
        final CountDownLatch createdLatch = new CountDownLatch(1);
        final CountDownLatch destroyedLatch = new CountDownLatch(1);

        HttpSessionListener listener = new HttpSessionListener()
        {
            @Override
            public void sessionDestroyed(HttpSessionEvent se)
            {
                destroyedLatch.countDown();
            }

            @Override
            public void sessionCreated(HttpSessionEvent se)
            {
                createdLatch.countDown();
            }
        };

        ServiceRegistration reg = m_context.registerService(HttpSessionListener.class.getName(), listener, null);

        register("/session", new TestServlet()
        {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                HttpSession session = req.getSession();
                session.setMaxInactiveInterval(2);

                resp.setStatus(SC_OK);
                resp.flushBuffer();
            }
        });

        try
        {
            assertContent(SC_OK, null, new URL("http://localhost:8080/session"));

            // Session should been created...
            assertTrue(createdLatch.await(50, TimeUnit.SECONDS));

            assertContent(SC_OK, null, new URL("http://localhost:8080/session"));

            // Session should timeout automatically...
            assertTrue(destroyedLatch.await(50, TimeUnit.SECONDS));
        }
        finally
        {
            reg.unregister();
        }
    }

    /**
     * Tests that {@link HttpSessionAttributeListener}s are called whenever a session attribute is added, changed or removed.
     */
    @Test
    public void testHttpSessionAttributeListenerOk() throws Exception
    {
        final CountDownLatch addedLatch = new CountDownLatch(1);
        final CountDownLatch removedLatch = new CountDownLatch(1);
        final CountDownLatch replacedLatch = new CountDownLatch(1);

        HttpSessionAttributeListener listener = new HttpSessionAttributeListener()
        {
            @Override
            public void attributeAdded(HttpSessionBindingEvent event)
            {
                addedLatch.countDown();
            }

            @Override
            public void attributeRemoved(HttpSessionBindingEvent event)
            {
                removedLatch.countDown();
            }

            @Override
            public void attributeReplaced(HttpSessionBindingEvent event)
            {
                replacedLatch.countDown();
            }
        };

        ServiceRegistration reg = m_context.registerService(HttpSessionAttributeListener.class.getName(), listener, null);

        register("/session", new TestServlet()
        {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
            {
                try
                {
                    HttpSession session = req.getSession();

                    session.setAttribute("foo", "bar");

                    assertTrue(addedLatch.await(5, TimeUnit.SECONDS));

                    session.setAttribute("foo", "qux");

                    assertTrue(replacedLatch.await(5, TimeUnit.SECONDS));

                    session.removeAttribute("foo");

                    assertTrue(removedLatch.await(5, TimeUnit.SECONDS));

                    resp.setStatus(SC_OK);
                }
                catch (InterruptedException e)
                {
                    resp.sendError(SC_SERVICE_UNAVAILABLE, e.getMessage());
                }
                finally
                {
                    resp.flushBuffer();
                }
            }
        });

        try
        {
            assertContent(SC_OK, null, new URL("http://localhost:8080/session"));
        }
        finally
        {
            reg.unregister();
        }
    }
}
