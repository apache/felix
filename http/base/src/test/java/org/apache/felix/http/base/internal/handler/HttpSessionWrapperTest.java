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

package org.apache.felix.http.base.internal.handler;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

import javax.servlet.http.HttpSession;

import org.junit.Test;

/**
 * Test cases for {@link HttpSessionWrapper}.
 */
public class HttpSessionWrapperTest
{

    /**
     * FELIX-5175 - sessions are incorrectly destroyed / destroyed too soon. 
     */
    @Test
    public void testSessionTimeout() throws Exception
    {
        Set<Long> ids;

        long sessionID = 123;
        long now = System.currentTimeMillis();

        HttpSession session = createMockSession(sessionID, now, 1);

        ids = HttpSessionWrapper.getExpiredSessionContextIds(session);
        assertTrue("Session should NOT be destroyed!", ids.isEmpty());

        // Pretend we've accessed this session two seconds ago, which should imply it is timed out...
        session = createMockSession(sessionID, now - 2000L, 1);

        ids = HttpSessionWrapper.getExpiredSessionContextIds(session);
        assertFalse("Session should be destroyed!", ids.isEmpty());
        assertTrue(ids.contains(sessionID));
    }

    private HttpSession createMockSession(long sessionId, long lastAccessed, int maxInactive)
    {
        String attrLastAccessed = String.format("org.apache.felix.http.session.context.lastaccessed.%d", sessionId);
        String attrMaxInactive = String.format("org.apache.felix.http.session.context.maxinactive.%d", sessionId);

        HttpSession session = mock(HttpSession.class);
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Arrays.asList(attrLastAccessed)));
        when(session.getAttribute(eq(attrLastAccessed))).thenReturn(lastAccessed);
        when(session.getAttribute(eq(attrMaxInactive))).thenReturn(maxInactive);

        return session;
    }
}
