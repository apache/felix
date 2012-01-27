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
package org.apache.felix.eventadmin.impl.security;

import java.security.Permission;

import org.osgi.service.event.TopicPermission;

/**
 * Utility class for permissions.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class PermissionsUtil
{
    /** Marker if permission created failed. */
    private static volatile boolean createPermissions = true;

    /**
     * Creates a <tt>TopicPermission</tt> for the given topic and the type PUBLISH
     *
     * @param topic The target topic
     *
     * @return The created permission or <tt>null</tt> in case the
     *         permission could not be created.
     *
     * @see org.osgi.service.event.TopicPermission
     */
    public static Permission createPublishPermission(final String topic)
    {
        if ( createPermissions )
        {
            try
            {
                return new org.osgi.service.event.TopicPermission(topic, TopicPermission.PUBLISH);
            }
            catch (Throwable t)
            {
                // This might happen in case security is not supported
                createPermissions = false;
            }
        }
        return null;
    }

    /**
     * Creates a <tt>TopicPermission</tt> for the given topic and the type SUBSCRIBE
     * Note that a
     *
     * @param topic The target topic
     *
     * @return The created permission or a <tt>null</tt> in case the
     *      permission could not be created.
     *
     * @see org.osgi.service.event.TopicPermission
     */
    public static Permission createSubscribePermission(final String topic)
    {
        if ( createPermissions )
        {
            try
            {
                return new org.osgi.service.event.TopicPermission(topic, TopicPermission.SUBSCRIBE);
            }
            catch (Throwable t)
            {
                // This might happen in case security is not supported
                createPermissions = false;
            }
        }
        return null;
    }
}
