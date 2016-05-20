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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_PORT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

public class SslFilterRequestTest
{
    @Test
    public void test_isSecure() throws Exception
    {
        HttpServletRequest req = mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req, null);

        when(req.isSecure()).thenReturn(false);
        assertFalse(req.isSecure());
        assertTrue(sreq.isSecure());
        assertFalse(req.isSecure());

        when(req.isSecure()).thenReturn(true);
        assertTrue(req.isSecure());
        assertTrue(sreq.isSecure());
        assertTrue(req.isSecure());
    }

    @Test
    public void test_getScheme() throws Exception
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req, null);

        when(req.getScheme()).thenReturn("http");
        assertEquals("http", req.getScheme());
        assertEquals("https", sreq.getScheme());
        assertEquals("http", req.getScheme());

        when(req.getScheme()).thenReturn("https");
        assertEquals("https", req.getScheme());
        assertEquals("https", sreq.getScheme());
        assertEquals("https", req.getScheme());
    }

    @Test
    public void test_getRequestURL() throws Exception
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req, null);

        when(req.getRequestURL()).thenReturn(new StringBuffer("http://some/page"));
        assertEquals("http://some/page", req.getRequestURL().toString());
        assertEquals("https://some/page", sreq.getRequestURL().toString());
        assertEquals("http://some/page", req.getRequestURL().toString());

        when(req.getRequestURL()).thenReturn(new StringBuffer("https://some/page"));
        assertEquals("https://some/page", req.getRequestURL().toString());
        assertEquals("https://some/page", sreq.getRequestURL().toString());
        assertEquals("https://some/page", req.getRequestURL().toString());
    }
    
    @Test
    public void test_getServerPort() throws Exception
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        SslFilterRequest sreq = new SslFilterRequest(req, null);
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn(null);        
        when(req.getServerPort()).thenReturn(-1);
        assertEquals(-1, sreq.getServerPort());
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn("");        
        when(req.getServerPort()).thenReturn(-1);
        assertEquals(-1, sreq.getServerPort());
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn("WRONG");        
        when(req.getServerPort()).thenReturn(-1);
        assertEquals(-1, sreq.getServerPort());
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn("W1");        
        when(req.getServerPort()).thenReturn(-1);
        assertEquals(-1, sreq.getServerPort());
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn("443");        
        assertEquals(443, sreq.getServerPort());
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn("80");        
        assertEquals(80, sreq.getServerPort());
        
        when(req.getHeader(HDR_X_FORWARDED_PORT)).thenReturn("4502");        
        assertEquals(4502, sreq.getServerPort());
        
    }
}
