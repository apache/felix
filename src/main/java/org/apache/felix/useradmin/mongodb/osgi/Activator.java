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
package org.apache.felix.useradmin.mongodb.osgi;

import java.util.Properties;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.apache.felix.useradmin.mongodb.MongoDBStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * Registers {@link MongoDBStore} as service.
 */
public class Activator implements BundleActivator {

    private LogServiceHelper m_logServiceHelper;
    private MongoDBStore m_store;

    @Override
    public void start(BundleContext context) throws Exception {
        m_logServiceHelper = new LogServiceHelper(context);
        m_logServiceHelper.open();
        
        m_store = new MongoDBStore();
        m_store.setLogService(m_logServiceHelper);
        
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, MongoDBStore.PID);

        String[] serviceNames = { RoleRepositoryStore.class.getName(), UserAdminListener.class.getName(), ManagedService.class.getName() };

        context.registerService(serviceNames, m_store, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (m_store != null) {
            m_store.close();
            m_store = null;
        }
        if (m_logServiceHelper != null) {
            m_logServiceHelper.close();
            m_logServiceHelper = null;
        }
    }
}
