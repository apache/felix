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
package org.apache.felix.framework;

import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.felix.framework.util.FelixConstants;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;

public class StartBundleTest extends TestCase
{
    public void testTransientExeption() throws Exception
    {
        HashMap<String, String> config = new HashMap<String, String>();
        config.put(FelixConstants.BUNDLE_STARTLEVEL_PROP, "1");
        final Felix f = new Felix(config);

        BundleImpl b = Mockito.mock(BundleImpl.class);
        Mockito.when(b.isLockable()).thenReturn(true);
        Mockito.when(b.getState()).thenReturn(Bundle.INSTALLED);
        Mockito.when(b.getStartLevel(1)).thenReturn(3);

        BundleRevisionImpl br = new BundleRevisionImpl(b, "test");
        Mockito.when(b.adapt(BundleRevision.class)).thenReturn(br);

        try
        {
            f.startBundle(b, Bundle.START_TRANSIENT);
            fail("Should have thrown a Bundle Exception");
        }
        catch (BundleException e)
        {
            assertEquals(BundleException.START_TRANSIENT_ERROR, e.getType());
        }
    }
}
