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

package org.apache.felix.ipojo.runtime.core;

import org.junit.After;
import org.junit.Before;
import org.osgi.service.cm.ConfigurationAdmin;
import org.ow2.chameleon.testing.helpers.BaseTest;
import org.ow2.chameleon.testing.helpers.ConfigAdminHelper;
import org.ow2.chameleon.testing.helpers.TimeUtils;

/**
 * Bootstrap the test from this project
 */
public class Common extends BaseTest {

    public static int UPDATE_WAIT_TIME = 2000;

    public ConfigAdminHelper caHelper = null;

    public ConfigurationAdmin admin;

    public void grace() {
        TimeUtils.grace(UPDATE_WAIT_TIME);
    }

    @Override
    public boolean deployConfigAdmin() {
        return true;
    }

    @Before
    public void initializeConfigAdmin() {
        caHelper = new ConfigAdminHelper(bc);
        admin = caHelper.getConfigurationAdmin();
        caHelper.deleteAllConfigurations();
    }

    @After
    public void stoppingConfigAdmin() {
        caHelper.deleteAllConfigurations();
        caHelper.dispose();
    }
}
