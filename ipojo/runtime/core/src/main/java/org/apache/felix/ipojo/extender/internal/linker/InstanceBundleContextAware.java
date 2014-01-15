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

package org.apache.felix.ipojo.extender.internal.linker;

import org.osgi.framework.BundleContext;

/**
 * Instance containers that can handle the bundle context from the instance declaration implement this interface,
 * letting handlers and other entities to retrieve this bundle context.
 * @since 1.11.2
 */
public interface InstanceBundleContextAware {

    /**
     * Sets the instance bundle context.
     * @param context the context of the instance
     * @since 1.11.2
     */
    public void setInstanceBundleContext(BundleContext context);

    /**
     * Gets the bundle context of the instance, i.e. the bundle context of the bundle having declared this instance.
     * @return the bundle context of the instance.
     * @since 1.11.2
     */
    public BundleContext getInstanceContext();
}
