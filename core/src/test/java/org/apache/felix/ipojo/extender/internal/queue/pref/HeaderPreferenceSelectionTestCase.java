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

package org.apache.felix.ipojo.extender.internal.queue.pref;

import static org.mockito.Mockito.when;

import java.util.Hashtable;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;

import junit.framework.TestCase;

/**
 * Checks the selection of the job processing preference from the bundle manifest.
 */
public class HeaderPreferenceSelectionTestCase extends TestCase {

    public static final String HEADER = "Header";

    @Mock
    private Bundle m_bundle;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testMissingHeader() throws Exception {
        Hashtable<String, String> headers = new Hashtable<String, String>();
        when(m_bundle.getHeaders()).thenReturn(headers);
        HeaderPreferenceSelection selection = new HeaderPreferenceSelection(HEADER);
        assertEquals(Preference.DEFAULT, selection.select(m_bundle));
    }

    public void testUnrecognizedHeader() throws Exception {
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(HEADER, "invalid");
        when(m_bundle.getHeaders()).thenReturn(headers);
        HeaderPreferenceSelection selection = new HeaderPreferenceSelection(HEADER);
        assertEquals(Preference.DEFAULT, selection.select(m_bundle));
    }

    public void testSyncHeader() throws Exception {
        Hashtable<String, String> headers = new Hashtable<String, String>();
        // We should ignore case
        headers.put(HEADER, "SyNc");
        when(m_bundle.getHeaders()).thenReturn(headers);
        HeaderPreferenceSelection selection = new HeaderPreferenceSelection(HEADER);
        assertEquals(Preference.SYNC, selection.select(m_bundle));
    }

    public void testAsyncHeader() throws Exception {
        Hashtable<String, String> headers = new Hashtable<String, String>();
        // We should ignore case
        headers.put(HEADER, "aSyNc");
        when(m_bundle.getHeaders()).thenReturn(headers);
        HeaderPreferenceSelection selection = new HeaderPreferenceSelection(HEADER);
        assertEquals(Preference.ASYNC, selection.select(m_bundle));
    }
}
