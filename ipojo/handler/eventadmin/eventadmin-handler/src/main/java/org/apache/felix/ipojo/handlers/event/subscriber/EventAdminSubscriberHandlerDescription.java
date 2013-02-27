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
package org.apache.felix.ipojo.handlers.event.subscriber;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Event Admin Subscriber Handler Description.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminSubscriberHandlerDescription extends HandlerDescription {

    /**
     * List of subscribers.
     */
    private Element[] m_subscribersDescriptions;

    /**
     * Creates a {@link EventAdminSubscriberHandlerDescription}
     * @param handler the handler
     * @param subscribers the subscribers
     */
    public EventAdminSubscriberHandlerDescription(Handler handler, Element[] subscribers) {
        super(handler);
         m_subscribersDescriptions = subscribers;
    }

    /**
     * Gets the handler info.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element root = super.getHandlerInfo();
        if (m_subscribersDescriptions != null) {
            for (int i = 0; i < m_subscribersDescriptions.length; i++) {
                Element description = m_subscribersDescriptions[i];
                root.addElement(description);
            }
        }
        return root;
    }
}
