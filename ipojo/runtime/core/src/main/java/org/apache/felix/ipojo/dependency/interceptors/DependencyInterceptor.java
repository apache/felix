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

package org.apache.felix.ipojo.dependency.interceptors;

import org.apache.felix.ipojo.util.DependencyModel;

/**
 * Dependency interceptor are collaborating with the service dependency during the service resolution.
 *
 * Interceptors publishes a service property (target) to select the dependencies they handle.
 *
 * Notice that interceptor can invalidate the set of service used by a dependency by calling
 * {@link org.apache.felix.ipojo.util.DependencyModel#invalidateMatchingServices()} and
 * {@link org.apache.felix.ipojo.util.DependencyModel#invalidateSelectedServices()}.
 */
public interface DependencyInterceptor {

    /**
     * A mandatory property published by provider of this service.
     * The value must be a LDAP filter (Filter or String). This filter will be confronted to the dependency property.
     *
     * @see org.osgi.framework.Filter
     */
    public static String TARGET_PROPERTY = "target";


    /**
     * The interceptor is plugged to the given dependency.
     * @param dependency the dependency starting using the interceptor.
     */
    public void open(DependencyModel dependency);

    /**
     * The interceptor won't be use anymore by the given dependency.
     * This method is called either when the interceptor is replace or when the instance's dependency is stopping.
     * @param dependency the dependency stopping its use of the interceptor
     */
    public void close(DependencyModel dependency);
}
