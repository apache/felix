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
package org.apache.felix.http.base.internal.registry;

import static java.util.Collections.unmodifiableCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.apache.felix.http.base.internal.util.PatternUtil.PatternComparator;

/**
 * Represents a Map-like structure that can map path-patterns to servlet/filter handlers, allowing
 * for easy access to those handlers, based on the match rules defined in section 12.1 of Servlet
 * 3.0 specification.
 * <p>
 * {@link FilterHandlerMapping} instances are immutable.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class FilterHandlerMapping
{
    private final SortedMap<Pattern, Set<FilterHandler>> exactMap;
    private final SortedMap<Pattern, Set<FilterHandler>> wildcardMap;
    private final Set<FilterHandler> mappedHandlers;

    /**
     * Creates a new, empty, {@link FilterHandlerMapping} instance.
     */
    public FilterHandlerMapping()
    {
        this(Collections.<Pattern, Collection<FilterHandler>>emptyMap());
    }

    /**
     * Creates a new {@link FilterHandlerMapping} instance for the given elements.
     *
     * @param mappings the elements to map.
     */
    private FilterHandlerMapping(@Nonnull final Map<Pattern, Collection<FilterHandler>> mappings)
    {
        this.exactMap = new TreeMap<Pattern, Set<FilterHandler>>(PatternComparator.INSTANCE);
        this.wildcardMap = new TreeMap<Pattern, Set<FilterHandler>>(PatternComparator.INSTANCE);
        this.mappedHandlers = new TreeSet<FilterHandler>();

        for (Map.Entry<Pattern, Collection<FilterHandler>> mapping : mappings.entrySet())
        {
            Pattern pattern = mapping.getKey();
            Collection<FilterHandler> handlers = mapping.getValue();

            mappedHandlers.addAll(handlers);

            if (PatternUtil.isWildcardPattern(pattern))
            {
                Set<FilterHandler> vs = this.wildcardMap.get(pattern);
                if (vs == null)
                {
                    vs = new TreeSet<FilterHandler>();
                    this.wildcardMap.put(pattern, vs);
                }
                vs.addAll(handlers);
            }
            else
            {
                Set<FilterHandler> vs = this.exactMap.get(pattern);
                if (vs == null)
                {
                    vs = new TreeSet<FilterHandler>();
                    this.exactMap.put(pattern, vs);
                }
                vs.addAll(handlers);
            }
        }
    }

    /**
     * Returns a new {@link FilterHandlerMapping} instance with a mapping for the
     * given handler.
     *
     * @param handler the handler to be added to the mapping.
     * @return a new {@link FilterHandlerMapping} instance with a mapping for the
     *         given handler.
     */
    public FilterHandlerMapping add(@Nonnull final FilterHandler handler)
    {
        final Map<Pattern, FilterHandler> mappings = new TreeMap<Pattern, FilterHandler>(PatternComparator.INSTANCE);
        for (final Pattern pattern : handler.getPatterns())
        {
            mappings.put(pattern, handler);
        }
        return add(mappings);
    }

    private FilterHandlerMapping add(@Nonnull final Map<Pattern, FilterHandler> mappings)
    {
        final Map<Pattern, Collection<FilterHandler>> newMappings = getAllMappings();
        addMappings(mappings, newMappings);
        return new FilterHandlerMapping(newMappings);
    }

    /**
     * Returns a new {@link FilterHandlerMapping} instance without a mapping for the
     * given handler.
     *
     * @param subject the handled element to be removed from the mapping
     * @return a new {@link FilterHandlerMapping} instance without a mapping for the
     *         given handler.
     */
    public FilterHandlerMapping remove(FilterHandler handler)
    {
        Map<Pattern, FilterHandler> mappings = new TreeMap<Pattern, FilterHandler>(PatternComparator.INSTANCE);
        for (Pattern pattern : handler.getPatterns())
        {
            mappings.put(pattern, handler);
        }
        return remove(mappings);
    }

    private FilterHandlerMapping remove(Map<Pattern, FilterHandler> mappings)
    {
        Map<Pattern, Collection<FilterHandler>> newMappings = getAllMappings();
        removeMappings(mappings, newMappings);
        return new FilterHandlerMapping(newMappings);
    }

    private void addMappings(Map<Pattern, FilterHandler> mappings, Map<Pattern, Collection<FilterHandler>> target)
    {
        for (Map.Entry<Pattern, FilterHandler> mapping : mappings.entrySet())
        {
            if (!target.containsKey(mapping.getKey()))
            {
                target.put(mapping.getKey(), new TreeSet<FilterHandler>());
            }
            target.get(mapping.getKey()).add(mapping.getValue());
        }
    }

    private void removeMappings(Map<Pattern, FilterHandler> mappings, Map<Pattern, Collection<FilterHandler>> target)
    {
        for (Map.Entry<Pattern, FilterHandler> mapping : mappings.entrySet())
        {
            Collection<FilterHandler> mappedHandlers = target.get(mapping.getKey());
            if (mappedHandlers == null)
            {
                continue;
            }
            mappedHandlers.remove(mapping.getValue());
            if (mappedHandlers.isEmpty())
            {
                target.remove(mapping.getKey());
            }
        }
    }

    private Map<Pattern, Collection<FilterHandler>> getAllMappings()
    {
        Map<Pattern, Collection<FilterHandler>> newMappings = new TreeMap<Pattern, Collection<FilterHandler>>(PatternComparator.INSTANCE);
        newMappings.putAll(exactMap);
        newMappings.putAll(wildcardMap);
        return newMappings;
    }

    /**
     * Returns all mapped handlers.
     *
     * @return the handlers contained in this mapping. The returned
     *         <code>Collection</code> is unmodifiable and never
     *         <code>null</code>.
     */
    public Collection<FilterHandler> values()
    {
        return unmodifiableCollection(mappedHandlers);
    }

    /**
     * Returns all matching handlers for the given path.
     *
     * @param path the path that should match, cannot be <code>null</code>.
     * @return a {@link Collection} of all matching handlers, never <code>null</code>.
     */
    public List<FilterHandler> getAllMatches(String path)
    {
        return getAllMatches(path, false /* firstOnly */);
    }

    /**
     * Performs the actual matching, yielding a list of either the first or all matching patterns.
     *
     * @param path the path to match, can be <code>null</code> in which case an empty string is
     *        used;
     * @param firstOnly <code>true</code> if only the first matching pattern should be returned,
     *        <code>false</code> if all matching patterns should be returned.
     * @return a list with matching elements, never <code>null</code>.
     */
    private List<FilterHandler> getAllMatches(String path, boolean firstOnly)
    {
        path = (path == null) ? "" : path.trim();

        Set<FilterHandler> result = new TreeSet<FilterHandler>();
        // Look for exact matches only, that is, those patterns without wildcards...
        for (Entry<Pattern, Set<FilterHandler>> entry : this.exactMap.entrySet())
        {
            Matcher matcher = entry.getKey().matcher(path);
            // !!! we should always match the *entire* pattern, instead of the longest prefix...
            if (matcher.matches())
            {
                Set<FilterHandler> vs = entry.getValue();
                for (FilterHandler v : vs)
                {
                    result.add(v);
                    if (firstOnly)
                    {
                        return new ArrayList<FilterHandler>(result);
                    }
                }
            }
        }

        // Try to apply the wildcard patterns...
        for (Entry<Pattern, Set<FilterHandler>> entry : this.wildcardMap.entrySet())
        {
            Matcher matcher = entry.getKey().matcher(path);
            if (matcher.find(0))
            {
                Set<FilterHandler> vs = entry.getValue();
                for (FilterHandler v : vs)
                {
                    result.add(v);

                    if (firstOnly)
                    {
                        break;
                    }
                }
            }
        }

        return new ArrayList<FilterHandler>(result);
    }
}
