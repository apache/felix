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

import org.osgi.service.event.TopicPermission;

/**
 * Utility class for permissions.
 *
 * @see org.apache.felix.eventadmin.impl.security.TopicPermissions
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class PermissionsUtil
{
    /**
     * Creates a <tt>TopicPermission</tt> for the given topic and the type PUBLISH
     * Note that a
     * <tt>java.lang.Object</tt> is returned in case creating a new TopicPermission
     * fails. This assumes that Bundle.hasPermission is used in order to evaluate the
     * created Permission which in turn will return true if security is not supported
     * by the framework. Otherwise, it will return false due to receiving something
     * that is not a subclass of <tt>java.lang.SecurityPermission</tt> hence, this
     * combination ensures that access is granted in case a topic permission could
     * not be created due to missing security support by the framework.
     *
     * @param topic The target topic
     *
     * @return The created permission or a <tt>java.lang.Object</tt> in case the
     *      permission could not be created.
     *
     * @see org.apache.felix.eventadmin.impl.security.TopicPermissions#createTopicPermission(String)
     * @see org.osgi.service.event.TopicPermission
     */
    public static Object createPublishPermission(final String topic)
    {
        Object result;
        try
        {
            result = new org.osgi.service.event.TopicPermission(topic, TopicPermission.PUBLISH);
        } catch (Throwable t)
        {
            // This might happen in case security is not supported
            // Bundle.hasPermission will return true in this case
            // hence topicPermission = new Object() is o.k.

            result = new Object();
        }
        return result;
    }

    /**
     * Creates a <tt>TopicPermission</tt> for the given topic and the type SUBSCRIBE
     * Note that a
     * <tt>java.lang.Object</tt> is returned in case creating a new TopicPermission
     * fails. This assumes that Bundle.hasPermission is used in order to evaluate the
     * created Permission which in turn will return true if security is not supported
     * by the framework. Otherwise, it will return false due to receiving something
     * that is not a subclass of <tt>java.lang.SecurityPermission</tt> hence, this
     * combination ensures that access is granted in case a topic permission could
     * not be created due to missing security support by the framework.
     *
     * @param topic The target topic
     *
     * @return The created permission or a <tt>java.lang.Object</tt> in case the
     *      permission could not be created.
     *
     * @see org.apache.felix.eventadmin.impl.security.TopicPermissions#createTopicPermission(String)
     * @see org.osgi.service.event.TopicPermission
     */
    public static Object createSubscribePermission(final String topic)
    {
        Object result;
        try
        {
            result = new org.osgi.service.event.TopicPermission(topic, TopicPermission.SUBSCRIBE);
        } catch (Throwable t)
        {
            // This might happen in case security is not supported
            // Bundle.hasPermission will return true in this case
            // hence topicPermission = new Object() is o.k.

            result = new Object();
        }
        return result;
    }

}
