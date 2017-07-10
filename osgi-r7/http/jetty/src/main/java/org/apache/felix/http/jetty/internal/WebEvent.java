/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.jetty.internal;

import org.osgi.framework.Bundle;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

import java.util.Dictionary;
import java.util.Hashtable;

public abstract class WebEvent
{
    private static final String TOPIC_WEB_EVENT = "org/osgi/service/web";
    private static final String TOPIC_DEPLOYING = TOPIC_WEB_EVENT + "/DEPLOYING";
    private static final String TOPIC_DEPLOYED = TOPIC_WEB_EVENT + "/DEPLOYED";
    private static final String TOPIC_UNDEPLOYING = TOPIC_WEB_EVENT + "/UNDEPLOYING";
    private static final String TOPIC_UNDEPLOYED = TOPIC_WEB_EVENT + "/UNDEPLOYED";
    private static final String TOPIC_FAILED = TOPIC_WEB_EVENT + "/FAILED";

    private static final String CONTEXT_PATH = "context.path";
    private static final String EXCEPTION = "exception";
    private static final String COLLISION = "collision";
    private static final String COLLISION_BUNDLES = "collision.bundles";

    private static final String EXTENDER_BUNDLE = "extender." + EventConstants.BUNDLE;
    private static final String EXTENDER_BUNDLE_ID = "extender." + EventConstants.BUNDLE_ID;
    private static final String EXTENDER_BUNDLE_VERSION = "extender." + EventConstants.BUNDLE_VERSION;
    private static final String EXTENDER_BUNDLE_SYMBOLICNAME = "extender." + EventConstants.BUNDLE_SYMBOLICNAME;

    private static final String HEADER_WEB_CONTEXT_PATH = "Web-ContextPath";

    static Event DEPLOYING(Bundle webAppBundle, Bundle extenderBundle)
    {
        return new Event(TOPIC_DEPLOYING, createBaseProperties(webAppBundle, extenderBundle));
    }

    static Event DEPLOYED(Bundle webAppBundle, Bundle extenderBundle)
    {
        return new Event(TOPIC_DEPLOYED, createBaseProperties(webAppBundle, extenderBundle));
    }

    static Event UNDEPLOYING(Bundle webAppBundle, Bundle extenderBundle)
    {
        return new Event(TOPIC_UNDEPLOYING, createBaseProperties(webAppBundle, extenderBundle));
    }

    static Event UNDEPLOYED(Bundle webAppBundle, Bundle extenderBundle)
    {
        return new Event(TOPIC_UNDEPLOYED, createBaseProperties(webAppBundle, extenderBundle));
    }

    static Event FAILED(Bundle webAppBundle, Bundle extenderBundle, Throwable exception, String collision, Long collisionBundles)
    {
        Dictionary<String, Object> props = createBaseProperties(webAppBundle, extenderBundle);
        if (exception != null)
        {
            props.put(EXCEPTION, exception);
        }
        if (collision != null)
        {
            props.put(COLLISION, collision);
        }
        if (collisionBundles != null)
        {
            props.put(COLLISION_BUNDLES, collisionBundles);
        }
        return new Event(TOPIC_FAILED, props);
    }

    private static Dictionary<String, Object> createBaseProperties(Bundle webAppBundle, Bundle extenderBundle)
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.BUNDLE_SYMBOLICNAME, webAppBundle.getSymbolicName());
        props.put(EventConstants.BUNDLE_ID, webAppBundle.getBundleId());
        props.put(EventConstants.BUNDLE, webAppBundle);
        props.put(EventConstants.BUNDLE_VERSION, webAppBundle.getVersion());
        props.put(CONTEXT_PATH, webAppBundle.getHeaders().get(HEADER_WEB_CONTEXT_PATH));
        props.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
        props.put(EXTENDER_BUNDLE, extenderBundle);
        props.put(EXTENDER_BUNDLE_ID, extenderBundle.getBundleId());
        props.put(EXTENDER_BUNDLE_SYMBOLICNAME, extenderBundle.getSymbolicName());
        props.put(EXTENDER_BUNDLE_VERSION, extenderBundle.getVersion());
        return props;
    }
}
