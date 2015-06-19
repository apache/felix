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
package org.apache.felix.cm.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.cm.MockNotCachablePersistenceManager;
import org.apache.felix.cm.MockPersistenceManager;
import org.apache.felix.cm.PersistenceManager;

import org.osgi.framework.Constants;

import junit.framework.TestCase;


/**
 * The <code>CachingPersistenceManagerProxyTest</code> class tests the issues
 * related to caching of configurations.
 * <p>
 * @see <a href="https://issues.apache.org/jira/browse/FELIX-4930">FELIX-4930</a>
 */
public class CachingPersistenceManagerProxyTest extends TestCase
{

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void test_caching_is_applied() throws Exception {
        String pid = "testDefaultPersistenceManager";
        SimpleFilter filter = SimpleFilter.parse("(&(service.pid=" + pid + ")(property1=value1))");

        PersistenceManager pm = new MockPersistenceManager();
        CachingPersistenceManagerProxy cpm = new CachingPersistenceManagerProxy( pm );

        Dictionary dictionary = new Hashtable();
        dictionary.put( "property1", "value1" );
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        Enumeration dictionaries = cpm.getDictionaries( filter );
        List list = Collections.list( dictionaries );
        assertEquals(1, list.size());

        dictionary = new Hashtable();
        dictionary.put( "property1", "value2" );
        pid = "testDefaultPersistenceManager";
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        dictionaries = cpm.getDictionaries( filter );
        list = Collections.list( dictionaries );
        assertEquals(1, list.size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void test_caching_is_avoided() throws Exception {
        String pid = "testDefaultPersistenceManager";
        SimpleFilter filter = SimpleFilter.parse("(&(service.pid=" + pid + ")(property1=value1))");

        PersistenceManager pm = new MockNotCachablePersistenceManager();
        CachingPersistenceManagerProxy cpm = new CachingPersistenceManagerProxy( pm );

        Dictionary dictionary = new Hashtable();
        dictionary.put( "property1", "value1" );
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        Enumeration dictionaries = cpm.getDictionaries( filter );
        List list = Collections.list( dictionaries );
        assertEquals(1, list.size());

        dictionary = new Hashtable();
        dictionary.put( "property1", "value2" );
        pid = "testDefaultPersistenceManager";
        dictionary.put( Constants.SERVICE_PID, pid );
        pm.store( pid, dictionary );

        dictionaries = cpm.getDictionaries( filter );
        list = Collections.list( dictionaries );
        assertEquals(0, list.size());
    }

}