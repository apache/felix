/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.filestore;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.service.cm.ConfigurationException;

/**
 * Test cases for {@link RoleRepositoryFileStore}.
 */
public class RoleRepositoryFileStoreTest extends TestCase {

    private RoleRepositoryFileStore m_store;
    
    /**
     * Tests that calling updated without the key "background.write.disabled" fails.
     */
    public void testUpdateConfigurationWithoutKeyWriteDisabledFail() throws Exception {
        try {
            m_store.updated(new Properties());
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }
    
    /**
     * Tests that calling updated with the key "background.write.disabled" set to "false" succeeds.
     */
    public void testUpdateConfigurationWithKeyWriteDisabledOk() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "true");

        m_store.updated(properties);
    }
    
    /**
     * Tests that calling updated with the key "background.write.disabled" set to a numeric value fails.
     */
    public void testUpdateConfigurationWithKeyWriteDisabledInvalidValueFail() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, Integer.valueOf(1));

        try {
            m_store.updated(properties);
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling updated without the key "background.write.delay.value" fails.
     */
    public void testUpdateConfigurationWithoutKeyWriteDelayValueFail() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_TIMEUNIT, "seconds");

        try {
            m_store.updated(properties);
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling updated with the key "background.write.delay.value" set to a non-numeric value fails.
     */
    public void testUpdateConfigurationWithKeyWriteDelayValueInvalidValueTypeFail() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_VALUE, "seconds");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_TIMEUNIT, "seconds");

        try {
            m_store.updated(properties);
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling updated with the key "background.write.delay.value" set to zero fails.
     */
    public void testUpdateConfigurationWithKeyWriteDelayValueZeroValueFail() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_VALUE, "0");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_TIMEUNIT, "seconds");

        try {
            m_store.updated(properties);
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling updated with the key "background.write.delay.value" set to a negative value fails.
     */
    public void testUpdateConfigurationWithKeyWriteDelayValueNegativeValueFail() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_VALUE, "-1");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_TIMEUNIT, "seconds");

        try {
            m_store.updated(properties);
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling updated without the key "background.write.delay.timeunit" succeeds as it is optional.
     */
    public void testUpdateConfigurationWithoutKeyWriteDelayTimeUnitOk() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_VALUE, "1");

        m_store.updated(properties);
    }

    /**
     * Tests that calling updated with the key "background.write.delay.timeunit" set to an invalid value fails.
     */
    public void testUpdateConfigurationWithKeyWriteDelayTimeUnitInvalidValueFail() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_VALUE, "1");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_TIMEUNIT, "1");

        try {
            m_store.updated(properties);
            fail("ConfigurationException expected!");
        } catch (ConfigurationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that calling updated with all keys succeeds.
     */
    public void testUpdateConfigurationWithAllKeysOk() throws Exception {
        Properties properties = new Properties();
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DISABLED, "false");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_VALUE, "1");
        properties.put(RoleRepositoryFileStore.KEY_WRITE_DELAY_TIMEUNIT, "seconds");

        m_store.updated(properties);
    }
    
    /**
     * Tests that calling updated with a <code>null</code>-dictionary causes the default settings to be applied.
     */
    public void testUpdateConfigurationWithoutPropertiesOk() throws Exception {
        m_store.updated(null);
    }

    protected void setUp() throws Exception {
        super.setUp();

        m_store = new RoleRepositoryFileStore(new File(System.getProperty("java.io.tmpdir")), false /* disable background writes */);
    }
}
