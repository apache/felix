/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.whiteboard;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.junit.Test;
import org.osgi.service.http.runtime.dto.DTOConstants;

public class FailureStateHandlerTest {

    @Test public void testAddRemoveNoContext()
    {
        final ServletInfo info = new ServletInfo("test", "/test", 3, Collections.EMPTY_MAP);

        final FailureStateHandler handler = new FailureStateHandler();
        handler.add(info, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);

        final FailedDTOHolder holder = new FailedDTOHolder();
        handler.getRuntime(holder);

        assertEquals(1, holder.failedServletDTOs.size());
        assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, holder.failedServletDTOs.get(0).failureReason);

        holder.failedServletDTOs.clear();

        handler.remove(info);
        handler.getRuntime(holder);

        assertEquals(0, holder.failedServletDTOs.size());
    }

    @Test public void testAddRemoveContext()
    {
        final ServletInfo info1 = new ServletInfo("test", "/test", 3, Collections.EMPTY_MAP);
        final ServletInfo info2 = new ServletInfo("test", "/test", 4, Collections.EMPTY_MAP);

        final FailureStateHandler handler = new FailureStateHandler();
        handler.add(info1, 1L, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
        handler.add(info2, 2L, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);

        final FailedDTOHolder holder = new FailedDTOHolder();
        handler.getRuntime(holder);

        assertEquals(2, holder.failedServletDTOs.size());
        assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, holder.failedServletDTOs.get(0).failureReason);
        assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, holder.failedServletDTOs.get(1).failureReason);
        assertEquals(1L, holder.failedServletDTOs.get(0).servletContextId);
        assertEquals(2L, holder.failedServletDTOs.get(1).servletContextId);


        handler.remove(info1, 1L);
        handler.remove(info2, 2L);

        holder.failedServletDTOs.clear();
        handler.getRuntime(holder);

        assertEquals(0, holder.failedServletDTOs.size());
    }

}
