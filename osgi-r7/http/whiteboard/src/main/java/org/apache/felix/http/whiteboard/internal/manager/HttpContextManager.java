/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.whiteboard.internal.manager;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class HttpContextManager
{
    /**
     * HttpContextHolders indexed by context ID fully configured
     * with an HttpContext and optional servlets and filters.
     * <p>
     * The context ID either includes the bundle ID as the first part of the
     * name, such as <i>123-sample.context</i> in the case of non-shared
     * contexts. IDs of shared contexts are prefixed with the fixed string
     * <code>shared</code> to not mix them with per-bundle contexts.
     */
    private final HashMap<String, HttpContextHolder> idMap;

    /**
     * Reverse mapping of HttpContext services to the context ID with
     * which they are registered.
     */
    private final HashMap<HttpContext, String> contextMap;

    /**
     * Map of servlets and filters registered referring to unregistered
     * contexts as of yet.
     */
    private final HashMap<String, Set<AbstractMapping>> orphanMappings;

    public HttpContextManager()
    {
        this.idMap = new HashMap<String, HttpContextHolder>();
        this.contextMap = new HashMap<HttpContext, String>();
        this.orphanMappings = new HashMap<String, Set<AbstractMapping>>();
    }

    private static String createId(Bundle bundle, String contextId)
    {
        if (bundle != null)
        {
            return bundle.getBundleId() + "-" + ((contextId == null) ? "" : contextId);
        }

        return createId(contextId);
    }

    private static String createId(String contextId)
    {
        return "shared-" + ((contextId == null) ? "" : contextId);
    }

    private static String getContextId(String id)
    {
        final int dash = id.indexOf('-');
        return (dash < 0) ? id : id.substring(dash + 1);
    }

    public synchronized HttpContext getHttpContext(Bundle bundle, String contextId, AbstractMapping mapping)
    {
        // per-bundle context
        String id = createId(bundle, contextId);
        HttpContextHolder holder = this.idMap.get(id);

        // shared context
        if (holder == null)
        {
            id = createId(contextId);
            holder = this.idMap.get(id);
        }

        // no context yet, put the mapping on hold
        if (holder == null)
        {
            // care for default context if no context ID
            if (ExtenderManager.isEmpty(contextId))
            {
                addHttpContext(bundle, "", new DefaultHttpContext(bundle));
                return getHttpContext(bundle, "", mapping);
            }

            // otherwise context is not here yet
            Set<AbstractMapping> orphaned = this.orphanMappings.get(contextId);
            if (orphaned == null)
            {
                orphaned = new HashSet<AbstractMapping>();
                this.orphanMappings.put(contextId, orphaned);
            }
            if (contextId != null)
            {
                // Only log something when an actual context ID is used. Should solve FELIX-4307...
                SystemLogger.debug("Holding off mapping with unregistered context with id [" + contextId + "]");
            }
            orphaned.add(mapping);
            return null;
        }

        // otherwise use the context
        if (contextId != null)
        {
            // Only log something when an actual context ID is used. Should solve FELIX-4307...
            SystemLogger.debug("Reusing context with id [" + contextId + "]");
        }

        holder.addMapping(mapping);
        return holder.getContext();
    }

    public synchronized void ungetHttpContext(Bundle bundle, String contextId, AbstractMapping mapping)
    {
        // per-bundle context
        String id = createId(bundle, contextId);
        HttpContextHolder context = this.idMap.get(id);

        // shared context
        if (context == null)
        {
            id = createId(contextId);
            context = this.idMap.get(id);
        }

        // remove the mapping if there is a mapped context
        if (context != null)
        {
            context.removeMapping(mapping);
        }
        else
        {
            Set<AbstractMapping> orphans = this.orphanMappings.get(contextId);
            if (orphans != null)
            {
                orphans.remove(mapping);
                if (orphans.isEmpty())
                {
                    this.orphanMappings.remove(contextId);
                }
            }

            // it is not expected but make sure there is no reference
            mapping.setContext(null);
        }
    }

    public synchronized Collection<AbstractMapping> addHttpContext(Bundle bundle, String contextId, HttpContext context)
    {
        String id = createId(bundle, contextId);
        HttpContextHolder holder = new HttpContextHolder(context);

        Set<AbstractMapping> orphans = this.orphanMappings.remove(contextId);
        if (orphans != null)
        {
            for (Iterator<AbstractMapping> mi = orphans.iterator(); mi.hasNext();)
            {
                AbstractMapping mapping = mi.next();
                if (bundle == null || bundle.equals(mapping.getBundle()))
                {
                    holder.addMapping(mapping);
                    mi.remove();
                }
            }

            // put any remaining orphans back
            if (!orphans.isEmpty())
            {
                this.orphanMappings.put(contextId, orphans);
            }
        }

        this.idMap.put(id, holder);
        this.contextMap.put(context, id);
        
        // return a copy to prevent concurrent modification
        return new HashSet<AbstractMapping>(holder.getMappings());
    }

    public synchronized Collection<AbstractMapping> removeHttpContext(HttpContext context)
    {
        String id = this.contextMap.remove(context);
        if (id != null)
        {
            HttpContextHolder holder = this.idMap.remove(id);
            if (holder != null)
            {
                Set<AbstractMapping> mappings = holder.getMappings();
                if (mappings != null && !mappings.isEmpty())
                {
                    // keep the orphans around
                    final String contextId = getContextId(id);
                    Set<AbstractMapping> orphans = this.orphanMappings.get(contextId);
                    if (orphans == null)
                    {
                        orphans = new HashSet<AbstractMapping>();
                        this.orphanMappings.put(getContextId(id), orphans);
                    }

                    for (AbstractMapping mapping : mappings)
                    {
                        mapping.setContext(null);
                        orphans.add(mapping);
                    }
                }
                return mappings;
            }
        }
        return null;
    }

    synchronized Map<String, HttpContextHolder> getHttpContexts()
    {
        return new HashMap<String, HttpContextHolder>(this.idMap);
    }

    synchronized Map<String, Set<AbstractMapping>> getOrphanMappings()
    {
        return new HashMap<String, Set<AbstractMapping>>(this.orphanMappings);
    }

    static class HttpContextHolder
    {
        private final HttpContext context;
        private final Set<AbstractMapping> mappings;

        HttpContextHolder(final HttpContext context)
        {
            this.context = context;
            this.mappings = new HashSet<AbstractMapping>();
        }

        public HttpContext getContext()
        {
            return context;
        }

        void addMapping(AbstractMapping mapping)
        {
            this.mappings.add(mapping);
            mapping.setContext(this.getContext());
        }

        void removeMapping(AbstractMapping mapping)
        {
            mapping.setContext(null);
            this.mappings.remove(mapping);
        }

        public Set<AbstractMapping> getMappings()
        {
            return mappings;
        }
    }
}
