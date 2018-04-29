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

<<<<<<< HEAD
import java.util.*;

=======
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.eventadmin.impl.util.Matchers;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
public class EventHandlerTracker extends ServiceTracker {

    /** The proxies in this list match all events. */
	private volatile List matchingAllEvents;
=======
public class EventHandlerTracker extends ServiceTracker<EventHandler, EventHandlerProxy> {

    /** The proxies in this list match all events. */
	private final List<EventHandlerProxy> matchingAllEvents;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /** This is a map for exact topic matches. The key is the topic,
     * the value is a list of proxies.
     */
<<<<<<< HEAD
    private volatile Map matchingTopic;
=======
    private final Map<String, List<EventHandlerProxy>> matchingTopic;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

	/** This is a map for wildcard topics. The key is the prefix of the topic,
	 * the value is a list of proxies
	 */
<<<<<<< HEAD
	private volatile Map matchingPrefixTopic;
=======
	private final Map<String, List<EventHandlerProxy>> matchingPrefixTopic;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368


	/** The context for the proxies. */
	private HandlerContext handlerContext;

    public EventHandlerTracker(final BundleContext context) {
		super(context, EventHandler.class.getName(), null);

		// we start with empty collections
<<<<<<< HEAD
		this.matchingAllEvents = new ArrayList();
		this.matchingTopic = new HashMap();
		this.matchingPrefixTopic = new HashMap();
=======
		this.matchingAllEvents = new CopyOnWriteArrayList<>();
		this.matchingTopic = new ConcurrentHashMap<>();
		this.matchingPrefixTopic = new ConcurrentHashMap<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	}

    /**
     * Update the timeout configuration.
     * @param ignoreTimeout
     */
    public void update(final String[] ignoreTimeout, final boolean requireTopic) {
<<<<<<< HEAD
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
=======
        final Matchers.Matcher[] ignoreTimeoutMatcher = Matchers.createPackageMatchers(ignoreTimeout);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        this.handlerContext = new HandlerContext(this.context, ignoreTimeoutMatcher, requireTopic);
    }

    /**
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
<<<<<<< HEAD
	public Object addingService(final ServiceReference reference) {
=======
	@Override
    public EventHandlerProxy addingService(final ServiceReference<EventHandler> reference) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
		final EventHandlerProxy proxy = new EventHandlerProxy(this.handlerContext, reference);
		if ( proxy.update() ) {
			this.put(proxy);
		}
		return proxy;
	}

	/**
	 * @see org.osgi.util.tracker.ServiceTracker#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
<<<<<<< HEAD
	public void modifiedService(final ServiceReference reference, final Object service) {
	    final EventHandlerProxy proxy = (EventHandlerProxy)service;
=======
	@Override
    public void modifiedService(final ServiceReference<EventHandler> reference, final EventHandlerProxy proxy) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	    this.remove(proxy);
	    if ( proxy.update() ) {
            this.put(proxy);
	    }
	}

	/**
	 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
<<<<<<< HEAD
	public void removedService(ServiceReference reference, Object service) {
        final EventHandlerProxy proxy = (EventHandlerProxy)service;
=======
	@Override
    public void removedService(final ServiceReference<EventHandler> reference, final EventHandlerProxy proxy) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        this.remove(proxy);
        proxy.dispose();
	}

<<<<<<< HEAD
	private void updateMap(final Map proxyListMap, final String key, final EventHandlerProxy proxy, final boolean add) {
        List proxies = (List)proxyListMap.get(key);
        if (proxies == null) {
=======
	private void updateMap(final Map<String, List<EventHandlerProxy>> proxyListMap, final String key, final EventHandlerProxy proxy, final boolean add) {
        List<EventHandlerProxy> proxies = proxyListMap.get(key);
        if (proxies == null)
        {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            if ( !add )
            {
                return;
            }
<<<<<<< HEAD
            proxies = new ArrayList();
        } else {
            proxies = new ArrayList(proxies);
        }
=======
            proxies = new CopyOnWriteArrayList<>();
            proxyListMap.put(key, proxies);
        }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if ( add )
        {
            proxies.add(proxy);
        }
        else
        {
            proxies.remove(proxy);
<<<<<<< HEAD
        }
        if ( proxies.size() == 0 )
        {
            proxyListMap.remove(key);
        }
        else
        {
            proxyListMap.put(key, proxies);
=======
            if ( proxies.size() == 0 )
            {
                proxyListMap.remove(key);
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
	}

	/**
	 * Check the topics of the event handler and put it into the
	 * corresponding collections.
<<<<<<< HEAD
	 * We always create new collections - while this is "expensive"
	 * it allows us to read from them unsynced
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	 */
	private synchronized void put(final EventHandlerProxy proxy) {
		final String[] topics = proxy.getTopics();
		if ( topics == null )
		{
<<<<<<< HEAD
		    final List newMatchingAllEvents = new ArrayList(this.matchingAllEvents);
		    newMatchingAllEvents.add(proxy);
		    this.matchingAllEvents = newMatchingAllEvents;
		}
		else
		{
		    Map newMatchingTopic = null;
		    Map newMatchingPrefixTopic = null;
=======
		    this.matchingAllEvents.add(proxy);
		}
		else
		{
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    		for(int i = 0; i < topics.length; i++) {
    			final String topic = topics[i];

    			if ( topic.endsWith("/*") )
    			{
                    // prefix topic: we remove the /*
<<<<<<< HEAD
    			    if ( newMatchingPrefixTopic == null )
    			    {
    			        newMatchingPrefixTopic = new HashMap(this.matchingPrefixTopic);
    			    }

    				final String prefix = topic.substring(0, topic.length() - 2);
                    this.updateMap(newMatchingPrefixTopic, prefix, proxy, true);
=======
    				final String prefix = topic.substring(0, topic.length() - 2);
                    this.updateMap(this.matchingPrefixTopic, prefix, proxy, true);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    			}
    			else
    			{
    			    // exact match
<<<<<<< HEAD
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
=======
                    this.updateMap(this.matchingTopic, topic, proxy, true);
    			}
    		}
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
		}
	}

    /**
     * Check the topics of the event handler and remove it from the
     * corresponding collections.
<<<<<<< HEAD
     * We always create new collections - while this is "expensive"
     * it allows us to read from them unsynced
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
	private synchronized void remove(final EventHandlerProxy proxy) {
        final String[] topics = proxy.getTopics();
        if ( topics == null )
        {
<<<<<<< HEAD
            final List newMatchingAllEvents = new ArrayList(this.matchingAllEvents);
            newMatchingAllEvents.remove(proxy);
            this.matchingAllEvents = newMatchingAllEvents;
        } else {
            Map newMatchingTopic = null;
            Map newMatchingPrefixTopic = null;
=======
            this.matchingAllEvents.remove(proxy);
        } else {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            for(int i = 0; i < topics.length; i++) {
                final String topic = topics[i];

                if ( topic.endsWith("/*") )
                {
                    // prefix topic: we remove the /*
<<<<<<< HEAD
                    if ( newMatchingPrefixTopic == null )
                    {
                        newMatchingPrefixTopic = new HashMap(this.matchingPrefixTopic);
                    }

                    final String prefix = topic.substring(0, topic.length() - 2);
                    this.updateMap(newMatchingPrefixTopic, prefix, proxy, false);
=======
                    final String prefix = topic.substring(0, topic.length() - 2);
                    this.updateMap(this.matchingPrefixTopic, prefix, proxy, false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
                else
                {
                    // exact match
<<<<<<< HEAD
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
=======
                    this.updateMap(this.matchingTopic, topic, proxy, false);
                }
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
	}

	/**
	 * Get all handlers for this event
	 *
	 * @param event The event topic
	 * @return All handlers for the event
	 */
<<<<<<< HEAD
	public Collection getHandlers(final Event event) {
	    final String topic = event.getTopic();

		final Set handlers = new HashSet();

		// Add all handlers matching everything
		handlers.addAll(this.matchingAllEvents);
=======
	public Collection<EventHandlerProxy> getHandlers(final Event event) {
	    final String topic = event.getTopic();

		final Set<EventHandlerProxy> handlers = new HashSet<>();

		// Add all handlers matching everything
        this.checkHandlerAndAdd(handlers, this.matchingAllEvents, event);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

		// Now check for prefix matches
		if ( !this.matchingPrefixTopic.isEmpty() )
		{
		    int pos = topic.lastIndexOf('/');
			while (pos != -1)
			{
			    final String prefix = topic.substring(0, pos);
<<<<<<< HEAD
				List proxies = (List)this.matchingPrefixTopic.get(prefix);
				if (proxies != null)
				{
					handlers.addAll(proxies);
				}
=======
		        this.checkHandlerAndAdd(handlers, this.matchingPrefixTopic.get(prefix), event);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

				pos = prefix.lastIndexOf('/');
			}
		}

		// Add the handlers for matching topic names
<<<<<<< HEAD
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
=======
		this.checkHandlerAndAdd(handlers, this.matchingTopic.get(topic), event);

		return handlers;
	}

	/**
	 * Checks each handler from the proxy list if it can deliver the event
	 * If the event can be delivered, the proxy is added to the handlers.
	 */
	private void checkHandlerAndAdd( final Set<EventHandlerProxy> handlers,
	        final List<EventHandlerProxy> proxies,
	        final Event event)
	{
	    if ( proxies != null )
	    {
            for(final EventHandlerProxy p : proxies)
            {
                if ( p.canDeliver(event) )
                {
                    handlers.add(p);
                }
            }
	    }
	}
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * The context object passed to the proxies.
     */
    static final class HandlerContext
    {
        /** The bundle context. */
        public final BundleContext bundleContext;

        /** The matchers for ignore timeout handling. */
<<<<<<< HEAD
        public final Matcher[] ignoreTimeoutMatcher;
=======
        public final Matchers.Matcher[] ignoreTimeoutMatcher;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        /** Is a topic required. */
        public final boolean requireTopic;

        public HandlerContext(final BundleContext bundleContext,
<<<<<<< HEAD
                final Matcher[] ignoreTimeoutMatcher,
=======
                final Matchers.Matcher[] ignoreTimeoutMatcher,
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                final boolean   requireTopic)
        {
            this.bundleContext = bundleContext;
            this.ignoreTimeoutMatcher = ignoreTimeoutMatcher;
            this.requireTopic = requireTopic;
        }
    }
}
