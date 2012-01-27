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
package org.apache.felix.eventadmin.impl.handler;

import java.util.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The event handler tracker keeps track of all event handler services.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventHandlerTracker extends ServiceTracker {

    /** The proxies in this list match all events. */
	private volatile List matchingAllEvents;

    /** This is a map for exact topic matches. The key is the topic,
     * the value is a list of proxies.
     */
    private volatile Map matchingTopic;

	/** This is a map for wildcard topics. The key is the prefix of the topic,
	 * the value is a list of proxies
	 */
	private volatile Map matchingPrefixTopic;


	/** The context for the proxies. */
	private HandlerContext handlerContext;

    public EventHandlerTracker(final BundleContext context) {
		super(context, EventHandler.class.getName(), null);

		// we start with empty collections
		this.matchingAllEvents = new ArrayList();
		this.matchingTopic = new HashMap();
		this.matchingPrefixTopic = new HashMap();
	}

    /**
     * Update the timeout configuration.
     * @param ignoreTimeout
     */
    public void update(final String[] ignoreTimeout, final boolean requireTopic) {
        final Matcher[] ignoreTimeoutMatcher;
        if ( ignoreTimeout == null || ignoreTimeout.length == 0 )
        {
            ignoreTimeoutMatcher = null;
        }
        else
        {
            ignoreTimeoutMatcher = new Matcher[ignoreTimeout.length];
            for(int i=0;i<ignoreTimeout.length;i++)
            {
                String value = ignoreTimeout[i];
                if ( value != null )
                {
                    value = value.trim();
                }
                if ( value != null && value.length() > 0 )
                {
                    if ( value.endsWith(".") )
                    {
                        ignoreTimeoutMatcher[i] = new PackageMatcher(value.substring(0, value.length() - 1));
                    }
                    else if ( value.endsWith("*") )
                    {
                        ignoreTimeoutMatcher[i] = new SubPackageMatcher(value.substring(0, value.length() - 1));
                    }
                    else
                    {
                        ignoreTimeoutMatcher[i] = new ClassMatcher(value);
                    }
                }
            }
        }
        this.handlerContext = new HandlerContext(this.context, ignoreTimeoutMatcher, requireTopic);
    }

    /**
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
	public Object addingService(final ServiceReference reference) {
		final EventHandlerProxy proxy = new EventHandlerProxy(this.handlerContext, reference);
		if ( proxy.update() ) {
			this.put(proxy);
		}
		return proxy;
	}

	/**
	 * @see org.osgi.util.tracker.ServiceTracker#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void modifiedService(final ServiceReference reference, final Object service) {
	    final EventHandlerProxy proxy = (EventHandlerProxy)service;
	    this.remove(proxy);
	    if ( proxy.update() ) {
            this.put(proxy);
	    }
	}

	/**
	 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void removedService(ServiceReference reference, Object service) {
        final EventHandlerProxy proxy = (EventHandlerProxy)service;
        this.remove(proxy);
        proxy.dispose();
	}

	private void updateMap(final Map proxyListMap, final String key, final EventHandlerProxy proxy, final boolean add) {
        List proxies = (List)proxyListMap.get(key);
        if (proxies == null) {
            if ( !add )
            {
                return;
            }
            proxies = new ArrayList();
        } else {
            proxies = new ArrayList(proxies);
        }
        if ( add )
        {
            proxies.add(proxy);
        }
        else
        {
            proxies.remove(proxy);
        }
        if ( proxies.size() == 0 )
        {
            proxyListMap.remove(key);
        }
        else
        {
            proxyListMap.put(key, proxies);
        }
	}

	/**
	 * Check the topics of the event handler and put it into the
	 * corresponding collections.
	 * We always create new collections - while this is "expensive"
	 * it allows us to read from them unsynced
	 */
	private synchronized void put(final EventHandlerProxy proxy) {
		final String[] topics = proxy.getTopics();
		if ( topics == null )
		{
		    final List newMatchingAllEvents = new ArrayList(this.matchingAllEvents);
		    newMatchingAllEvents.add(proxy);
		    this.matchingAllEvents = newMatchingAllEvents;
		}
		else
		{
		    Map newMatchingTopic = null;
		    Map newMatchingPrefixTopic = null;
    		for(int i = 0; i < topics.length; i++) {
    			final String topic = topics[i];

    			if ( topic.endsWith("/*") )
    			{
                    // prefix topic: we remove the /*
    			    if ( newMatchingPrefixTopic == null )
    			    {
    			        newMatchingPrefixTopic = new HashMap(this.matchingPrefixTopic);
    			    }

    				final String prefix = topic.substring(0, topic.length() - 2);
                    this.updateMap(newMatchingPrefixTopic, prefix, proxy, true);
    			}
    			else
    			{
    			    // exact match
                    if ( newMatchingTopic == null )
                    {
                        newMatchingTopic = new HashMap(this.matchingTopic);
                    }

                    this.updateMap(newMatchingTopic, topic, proxy, true);
    			}
    		}
    		if ( newMatchingTopic != null )
    		{
    		    this.matchingTopic = newMatchingTopic;
    		}
    		if ( newMatchingPrefixTopic != null )
    		{
    		    this.matchingPrefixTopic = newMatchingPrefixTopic;
    		}
		}
	}

    /**
     * Check the topics of the event handler and remove it from the
     * corresponding collections.
     * We always create new collections - while this is "expensive"
     * it allows us to read from them unsynced
     */
	private synchronized void remove(final EventHandlerProxy proxy) {
        final String[] topics = proxy.getTopics();
        if ( topics == null )
        {
            final List newMatchingAllEvents = new ArrayList(this.matchingAllEvents);
            newMatchingAllEvents.remove(proxy);
            this.matchingAllEvents = newMatchingAllEvents;
        } else {
            Map newMatchingTopic = null;
            Map newMatchingPrefixTopic = null;
            for(int i = 0; i < topics.length; i++) {
                final String topic = topics[i];

                if ( topic.endsWith("/*") )
                {
                    // prefix topic: we remove the /*
                    if ( newMatchingPrefixTopic == null )
                    {
                        newMatchingPrefixTopic = new HashMap(this.matchingPrefixTopic);
                    }

                    final String prefix = topic.substring(0, topic.length() - 2);
                    this.updateMap(newMatchingPrefixTopic, prefix, proxy, false);
                }
                else
                {
                    // exact match
                    if ( newMatchingTopic == null )
                    {
                        newMatchingTopic = new HashMap(this.matchingTopic);
                    }

                    this.updateMap(newMatchingTopic, topic, proxy, false);
                }
            }
            if ( newMatchingTopic != null )
            {
                this.matchingTopic = newMatchingTopic;
            }
            if ( newMatchingPrefixTopic != null )
            {
                this.matchingPrefixTopic = newMatchingPrefixTopic;
            }
        }
	}

	/**
	 * Get all handlers for this event
	 *
	 * @param event The event topic
	 * @return All handlers for the event
	 */
	public Collection getHandlers(final Event event) {
	    final String topic = event.getTopic();

		final Set handlers = new HashSet();

		// Add all handlers matching everything
		handlers.addAll(this.matchingAllEvents);

		// Now check for prefix matches
		if ( !this.matchingPrefixTopic.isEmpty() )
		{
		    int pos = topic.lastIndexOf('/');
			while (pos != -1)
			{
			    final String prefix = topic.substring(0, pos);
				List proxies = (List)this.matchingPrefixTopic.get(prefix);
				if (proxies != null)
				{
					handlers.addAll(proxies);
				}

				pos = prefix.lastIndexOf('/');
			}
		}

		// Add the handlers for matching topic names
		List proxies = (List)this.matchingTopic.get(topic);
		if (proxies != null) {
			handlers.addAll(proxies);
		}

		// now check permission and filters
		final Iterator i = handlers.iterator();
		while ( i.hasNext() ) {
		    final EventHandlerProxy proxy = (EventHandlerProxy) i.next();
		    if ( !proxy.canDeliver(event) ) {
		        i.remove();
		    }
		}
		return handlers;
	}

    /**
     * The matcher interface for checking if timeout handling
     * is disabled for the handler.
     * Matching is based on the class name of the event handler.
     */
    static interface Matcher
    {
        boolean match(String className);
    }

    /** Match a package. */
    private static final class PackageMatcher implements Matcher
    {
        private final String m_packageName;

        public PackageMatcher(final String name)
        {
            m_packageName = name;
        }
        public boolean match(String className)
        {
            final int pos = className.lastIndexOf('.');
            return pos > -1 && className.substring(0, pos).equals(m_packageName);
        }
    }

    /** Match a package or sub package. */
    private static final class SubPackageMatcher implements Matcher
    {
        private final String m_packageName;

        public SubPackageMatcher(final String name)
        {
            m_packageName = name + '.';
        }
        public boolean match(String className)
        {
            final int pos = className.lastIndexOf('.');
            return pos > -1 && className.substring(0, pos + 1).startsWith(m_packageName);
        }
    }

    /** Match a class name. */
    private static final class ClassMatcher implements Matcher
    {
        private final String m_className;

        public ClassMatcher(final String name)
        {
            m_className = name;
        }
        public boolean match(String className)
        {
            return m_className.equals(className);
        }
    }

    /**
     * The context object passed to the proxies.
     */
    static final class HandlerContext
    {
        /** The bundle context. */
        public final BundleContext bundleContext;

        /** The matchers for ignore timeout handling. */
        public final Matcher[] ignoreTimeoutMatcher;

        /** Is a topic required. */
        public final boolean requireTopic;

        public HandlerContext(final BundleContext bundleContext,
                final Matcher[] ignoreTimeoutMatcher,
                final boolean   requireTopic)
        {
            this.bundleContext = bundleContext;
            this.ignoreTimeoutMatcher = ignoreTimeoutMatcher;
            this.requireTopic = requireTopic;
        }
    }
}
