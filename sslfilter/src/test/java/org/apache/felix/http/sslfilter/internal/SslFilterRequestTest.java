/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.sslfilter.internal;

import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;

public class SslFilterRequestTest
{

    @Test
    public void test_isSecure()
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req);

        when(req.isSecure()).thenReturn(false);
        TestCase.assertFalse(req.isSecure());
        TestCase.assertTrue(sreq.isSecure());
        TestCase.assertFalse(req.isSecure());

        when(req.isSecure()).thenReturn(true);
        TestCase.assertTrue(req.isSecure());
        TestCase.assertTrue(sreq.isSecure());
        TestCase.assertTrue(req.isSecure());
    }

    @Test
    public void test_getScheme()
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req);

        when(req.getScheme()).thenReturn("http");
        TestCase.assertEquals("http", req.getScheme());
        TestCase.assertEquals("https", sreq.getScheme());
        TestCase.assertEquals("http", req.getScheme());

        when(req.getScheme()).thenReturn("https");
        TestCase.assertEquals("https", req.getScheme());
        TestCase.assertEquals("https", sreq.getScheme());
        TestCase.assertEquals("https", req.getScheme());
    }

    @Test
    public void test_getRequestURL()
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req);

        when(req.getRequestURL()).thenReturn(new StringBuffer("http://some/page"));
        TestCase.assertEquals("http://some/page", req.getRequestURL().toString());
        TestCase.assertEquals("https://some/page", sreq.getRequestURL().toString());
        TestCase.assertEquals("http://some/page", req.getRequestURL().toString());

        when(req.getRequestURL()).thenReturn(new StringBuffer("https://some/page"));
        TestCase.assertEquals("https://some/page", req.getRequestURL().toString());
        TestCase.assertEquals("https://some/page", sreq.getRequestURL().toString());
        TestCase.assertEquals("https://some/page", req.getRequestURL().toString());
    }
}
