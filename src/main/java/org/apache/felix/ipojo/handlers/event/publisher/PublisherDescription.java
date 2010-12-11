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
package org.apache.felix.ipojo.handlers.event.publisher;

/**
 * Publisher Description.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PublisherDescription {

    /**
     * The described publisher
     */
    private Publisher m_publisher;

    /**
     * Creates a {@link PublisherDescription} based
     * on the given {@link Publisher}
     * @param p the publisher
     */
    public PublisherDescription(Publisher p) {
        m_publisher = p;
    }


    /**
     * Gets the topic list.
     * @return the list of published topics
     */
    public String[] getTopics() {
        return m_publisher.getTopics();
    }

    /**
     * Checks if the publisher is synchronous.
     * @return <code>true</code> if the publisher is synchronous
     */
    public boolean isSynchronous() {
        return m_publisher.isSynchronous();
    }

    /**
     * Gets the data key if used.
     * @return the data key
     */
    public String getDataKey() {
        return m_publisher.getDataKey();
    }

    /**
     * Gets the publisher name if any.
     * @return the publisher name
     */
    public String getName() {
        return m_publisher.getName();
    }


}
