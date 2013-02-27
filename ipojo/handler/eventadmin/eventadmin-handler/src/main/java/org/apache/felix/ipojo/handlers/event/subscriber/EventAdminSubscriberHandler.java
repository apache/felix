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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.event.EventUtil;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Event Subscriber Handler.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminSubscriberHandler extends PrimitiveHandler implements
        EventHandler {

    /**
     * The handler namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handlers.event";

    // Names of instance configuration properties.

    /**
     * The event's topics instance configuration property.
     */
    public static final String TOPICS_PROPERTY = "event.topics";

    /**
     * The event's filter instance configuration property.
     */
    public static final String FILTER_PROPERTY = "event.filter";

    /**
     * The prefix for logged messages.
     */
    private static final String LOG_PREFIX = "EVENT ADMIN SUBSCRIBER HANDLER : ";

    /**
     * The instance manager.
     */
    private InstanceManager m_manager;

    /**
     * The list of subscriber accessible by name.
     */
    private final Map m_subscribersByName = new HashMap();

    /**
     * The list of callbacks accessible by subscribers' names.
     */
    private final Map m_callbacksByName = new Hashtable();

    /**
     * The iPOJO properties representing all the topics.
     * Immutable.
     */
    private String[] m_topics;

    /**
     * Listening to received events ?
     */
    private boolean m_isListening;

    /**
     * The handler description
     */
    private EventAdminSubscriberHandlerDescription m_description;

    /**
     * Initializes the component type.
     *
     * @param cd component type description to populate.
     * @param metadata component type metadata.
     * @throws ConfigurationException if the metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(
     * org.apache.felix.ipojo.architecture.ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentTypeDescription cd,
            Element metadata)
        throws ConfigurationException {

        // Update the current component description
        Dictionary dict = new Properties();
        cd.addProperty(new PropertyDescription(TOPICS_PROPERTY,
                Dictionary.class.getName(), dict.toString()));
        dict = new Properties();
        cd.addProperty(new PropertyDescription(FILTER_PROPERTY,
                Dictionary.class.getName(), dict.toString()));

        // Get Metadata subscribers
        Element[] subscribers = metadata.getElements("subscriber", NAMESPACE);
        if (subscribers != null) {

            // Maps used to check name and field are unique
            Set nameSet = new HashSet();
            Set callbackSet = new HashSet();

            // Check all subscribers are well formed
            for (int i = 0; i < subscribers.length; i++) {

                // Check the subscriber configuration is correct by creating an
                // unused subscriber metadata
                EventAdminSubscriberMetadata subscriberMetadata = new EventAdminSubscriberMetadata(
                        getFactory().getBundleContext(), subscribers[i]);

                String name = subscriberMetadata.getName();
                info(LOG_PREFIX + "Checking subscriber " + name);

                // Determine the event callback prototype
                PojoMetadata pojoMetadata = getPojoMetadata();
                String callbackType;
                if (subscriberMetadata.getDataKey() == null) {
                    callbackType = Event.class.getName();
                } else {
                    callbackType = subscriberMetadata.getDataType().getName();
                }

                // Check the event callback method is present
                MethodMetadata methodMetadata = pojoMetadata.getMethod(
                        subscriberMetadata.getCallback(),
                        new String[] { callbackType });
                String callbackSignature = subscriberMetadata.getCallback()
                        + "(" + callbackType + ")";
                if (methodMetadata == null) {
                    throw new ConfigurationException(
                            "Cannot find callback method " + callbackSignature);
                }

                // Warn if the same callback is used by several subscribers
                if (callbackSet.contains(callbackSignature)) {
                    warn("The callback method is already used by another subscriber : "
                            + callbackSignature);
                } else {
                    callbackSet.add(callbackSignature);
                }

                // Check name is unique
                if (nameSet.contains(name)) {
                    throw new ConfigurationException(
                            "A subscriber with the same name already exists : "
                                    + name);
                }
                nameSet.add(name);
            }

            m_description = new EventAdminSubscriberHandlerDescription(this,
                    subscribers);

        } else {
            info(LOG_PREFIX + "No subscriber to check");
        }
    }

    /**
     * Constructor.
     *
     * @param metadata the omponent type metadata
     * @param conf the instance configuration
     * @throws ConfigurationException if one event subscription is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf)
        throws ConfigurationException {

        // Store the component manager
        m_manager = getInstanceManager();

        // Get the topics and filter instance configuration
        Dictionary instanceTopics = (Dictionary) conf.get(TOPICS_PROPERTY);
        Dictionary instanceFilter = (Dictionary) conf.get(FILTER_PROPERTY);

        // Get Metadata subscribers
        Element[] subscribers = metadata.getElements("subscriber", NAMESPACE);

        // The topics to listen
        Set topics = new TreeSet();

        if (subscribers != null) {

            // Configure all subscribers
            for (int i = 0; i < subscribers.length; i++) {

                // Extract the subscriber configuration
                EventAdminSubscriberMetadata subscriberMetadata = new EventAdminSubscriberMetadata(
                        m_manager.getContext(), subscribers[i]);
                String name = subscriberMetadata.getName();
                info(LOG_PREFIX + "Configuring subscriber " + name);

                // Get the topics instance configuration if redefined
                String topicsString = (instanceTopics != null) ? (String) instanceTopics.get(name) : null;
                if (topicsString != null) {
                    subscriberMetadata.setTopics(topicsString);
                }

                // Get the filter instance configuration if redefined
                String filterString = (instanceFilter != null) ? (String) instanceFilter.get(name) : null;
                if (filterString != null) {
                    subscriberMetadata.setFilter(filterString);
                }

                // Check the publisher is correctly configured
                subscriberMetadata.check();

                // Add this subscriber's topics to the global list
                String[] subscriberTopics = subscriberMetadata.getTopics();
                for (int j = 0; j < subscriberTopics.length; j++) {
                    topics.add(subscriberTopics[j]);
                }

                // Determine the event callback prototype
                PojoMetadata pojoMetadata = getPojoMetadata();
                String callbackType;
                if (subscriberMetadata.getDataKey() == null) {
                    callbackType = Event.class.getName();
                } else {
                    callbackType = subscriberMetadata.getDataType().getName();
                }

                // Create the specified callback and register it
                MethodMetadata methodMetadata = pojoMetadata.getMethod(
                        subscriberMetadata.getCallback(),
                        new String[] { callbackType });
                Callback callback = new Callback(methodMetadata, m_manager);
                m_callbacksByName.put(name, callback);

                // Add the subscriber list gloal map
                m_subscribersByName.put(name, subscriberMetadata);
            }

            // Construct the global topic list
            m_topics = new String[topics.size()];
            int i = 0;
            for (Iterator iterator = topics.iterator(); iterator.hasNext();) {
                String tmp = (String) iterator.next();
                m_topics[i++] = tmp;
            }

            m_description = new EventAdminSubscriberHandlerDescription(this,
                    subscribers);

        } else {
            info(LOG_PREFIX + "No subscriber to configure");
        }
    }

    /**
     * Handler start method.
     *
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public synchronized void start() {
        m_isListening = true;
    }

    /**
     * Handler stop method.
     *
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public synchronized void stop() {
        m_isListening = false;
    }

    /**
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }

    /***************************************************************************
     * OSGi EventHandler callback
     **************************************************************************/

    /**
     * Receives an event. The event is dispatch to attached subscribers.
     *
     * @param event the received event.
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        EventAdminSubscriberMetadata subscriberMetadata = null;
        // Retrieve the event's topic
        String topic = event.getTopic();

        // For each subscribers
        Collection subscribers = m_subscribersByName.values();
        for (Iterator i = subscribers.iterator(); i.hasNext();) {
            subscriberMetadata = (EventAdminSubscriberMetadata) i.next();

            // Stack confinement
            boolean isListening = false;

            Object callbackParam = null;
            Callback callback = null;

            synchronized (this) {
                isListening = m_isListening;
            }

            // Check if the subscriber's topic and filter match
            Filter filter = subscriberMetadata.getFilter();

            if (EventUtil.matches(topic, subscriberMetadata.getTopics())
                    && (filter == null || event.matches(filter))) {

                String name = subscriberMetadata.getName();
                callback = (Callback) m_callbacksByName.get(name);

                try {
                    // Depending on the subscriber type...
                    callbackParam = getCallbackParameter(event,
                            subscriberMetadata);
                } catch (ClassCastException e) {
                    // Ignore the data event if type doesn't match
                    warn(LOG_PREFIX + "Ignoring data event : Bad data type", e);
                } catch (NoSuchFieldException e) {
                    // Ignore events without data field for data events
                    // subscriber
                    warn(LOG_PREFIX + "Ignoring data event : No data", e);
                }
            }


            // Run the callback (final check to avoid
            // NullPointerExceptions)
            if (isListening  && callback != null  && callbackParam != null) {
                try {
                    callback.call(new Object[] { callbackParam });
                } catch (InvocationTargetException e) {
                    error(LOG_PREFIX
                            + "The callback has thrown an exception",
                            e.getTargetException());
                } catch (Exception e) {
                    error(LOG_PREFIX
                            + "Unexpected exception when calling callback",
                            e);
                }
            }

        }
    }

    /**
     * Computes the callback parameter.
     * @param event the event
     * @param subscriberMetadata the subscribe metadata
     * @param dataKey the data key
     * @return the parameter of the callback
     * @throws ClassCastException the data class does not match the found value.
     * @throws NoSuchFieldException the datakey is not present in the event.
     */
    private Object getCallbackParameter(final Event event,
            final EventAdminSubscriberMetadata subscriberMetadata
            ) throws ClassCastException,NoSuchFieldException {
        String dataKey = subscriberMetadata.getDataKey();
        if (dataKey == null) {
            // Generic event subscriber : pass the event to the
            // registered callback
            return event;
        } else {
            // Check for a data key in the event
            boolean dataKeyPresent = false;
            String[] properties = event.getPropertyNames();
            for (int j = 0; j < properties.length && !dataKeyPresent; j++) {
                if (dataKey.equals(properties[j])) {
                    dataKeyPresent = true;
                }
            }

            if (dataKeyPresent) {
                // Data event : check type compatibility and
                // pass the given object to the registered
                // callback
                Object data = event.getProperty(dataKey);
                Class dataType = subscriberMetadata.getDataType();
                Class dataClazz = data.getClass();
                if (dataType.isAssignableFrom(dataClazz)) {
                    return data;
                } else {
                    throw new ClassCastException("Cannot convert " + dataClazz.getName() + " to "
                                    + dataType.getName());
                }
            } else {
                throw new java.lang.NoSuchFieldException(dataKey);
            }

        }
    }
}
