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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Handler Description for Event Admin Publisher.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminPublisherHandlerDescription extends HandlerDescription {

    /**
     * The list of publishers.
     */
    List/*<PublisherDescription>*/ m_publishersDescriptions;

    // FIXME : Add listener interface for
    //       : onServiceArrival, onServiceDeparture, onServiceBound, onServiceUnbound
    //       : methods.

    /**
     * Creates the {@link EventAdminPublisherHandlerDescription}.
     * @param handler the handler
     * @param publishers the list of publishers
     */
    public EventAdminPublisherHandlerDescription(Handler handler, Collection/*<Publisher>*/ publishers) {
        super(handler);

        m_publishersDescriptions = new ArrayList/*<PublisherDescription>*/();
        if (publishers != null) {
            Iterator iterator = publishers.iterator();
            while (iterator.hasNext()){
                Publisher p = (Publisher) iterator.next();
                m_publishersDescriptions.add(new PublisherDescription(p));
            }
        }

    }

    /**
     * Gets the publisher descriptions.
     * @return the descriptions.
     */
    public PublisherDescription[] getPublisherDescriptions() {
        return (PublisherDescription[])
            m_publishersDescriptions.toArray(new PublisherDescription[m_publishersDescriptions.size()]);
    }

    /**
     * Gets the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element root = super.getHandlerInfo();
        for (int i = 0; i < m_publishersDescriptions.size(); i++) {
            PublisherDescription p = (PublisherDescription) m_publishersDescriptions.get(i);
            Element publisher = new Element("Publisher", "");
            publisher.addAttribute(new Attribute("name", p.getName()));
            publisher.addAttribute(new Attribute("synchronous", String.valueOf(p.isSynchronous())));
            publisher.addAttribute(new Attribute("data_key", p.getDataKey()));
            Element topics = new Element("Topics", "");
            if (p.getTopics() != null) {
                for (int j = 0; j < p.getTopics().length; j++) {
                    String topic = p.getTopics()[j];
                    Element e_topic = new Element("topic","");
                    topics.addElement(e_topic);
                    e_topic.addAttribute(new Attribute("name",topic));
                }
            }
            publisher.addElement(topics);
            root.addElement(publisher);
        }
        return root;
    }

}
