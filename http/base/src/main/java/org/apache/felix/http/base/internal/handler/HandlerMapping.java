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
package org.apache.felix.http.base.internal.handler;

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

import org.apache.felix.http.base.internal.util.PatternUtil;
import org.apache.felix.http.base.internal.util.PatternUtil.PatternComparator;

/**
 * Represents a Map-like structure that can map path-patterns to servlet/filter handlers, allowing
 * for easy access to those handlers, based on the match rules defined in section 12.1 of Servlet
 * 3.0 specification.
 * <p>
 * {@link HandlerMapping} instances are immutable.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class HandlerMapping<V extends AbstractHandler<V>>
{
    private final SortedMap<Pattern, Set<V>> exactMap;
    private final SortedMap<Pattern, Set<V>> wildcardMap;
    private final Set<V> mappedHandlers;

    /**
     * Creates a new, empty, {@link HandlerMapping} instance.
     */
    public HandlerMapping()
    {
        this(Collections.<Pattern, Collection<V>>emptyMap());
    }

    /**
     * Creates a new {@link HandlerMapping} instance for the given elements.
     *
     * @param mappings the elements to map.
     */
    private HandlerMapping(Map<Pattern, Collection<V>> mappings)
    {
        this.exactMap = new TreeMap<Pattern, Set<V>>(PatternComparator.INSTANCE);
        this.wildcardMap = new TreeMap<Pattern, Set<V>>(PatternComparator.INSTANCE);
        this.mappedHandlers = new TreeSet<V>();

        for (Map.Entry<Pattern, Collection<V>> mapping : mappings.entrySet())
        {
            Pattern pattern = mapping.getKey();
            Collection<V> handlers = mapping.getValue();

            mappedHandlers.addAll(handlers);

            if (PatternUtil.isWildcardPattern(pattern))
            {
                Set<V> vs = this.wildcardMap.get(pattern);
                if (vs == null)
                {
                    vs = new TreeSet<V>();
                    this.wildcardMap.put(pattern, vs);
                }
                vs.addAll(handlers);
            }
            else
            {
                Set<V> vs = this.exactMap.get(pattern);
                if (vs == null)
                {
                    vs = new TreeSet<V>();
                    this.exactMap.put(pattern, vs);
                }
                vs.addAll(handlers);
            }
        }
    }

    /**
     * Returns a new {@link HandlerMapping} instance with a mapping for the
     * given handler.
     *
     * @param handler the handler to be added to the mapping.
     * @return a new {@link HandlerMapping} instance with a mapping for the
     *         given handler.
     */
    public HandlerMapping<V> add(V handler)
    {
        Map<Pattern, V> mappings = new TreeMap<Pattern, V>(PatternComparator.INSTANCE);
        for (Pattern pattern : handler.getPatterns())
        {
            mappings.put(pattern, handler);
        }
        return add(mappings);
    }

    HandlerMapping<V> add(Map<Pattern, V> mappings)
    {
        Map<Pattern, Collection<V>> newMappings = getAllMappings();
        addMappings(mappings, newMappings);
        return new HandlerMapping<V>(newMappings);
    }

    /**
     * Returns a new {@link HandlerMapping} instance without a mapping for the
     * given handler.
     *
     * @param subject the handled element to be removed from the mapping
     * @return a new {@link HandlerMapping} instance without a mapping for the
     *         given handler.
     */
    public HandlerMapping<V> remove(V handler)
    {
        Map<Pattern, V> mappings = new TreeMap<Pattern, V>(PatternComparator.INSTANCE);
        for (Pattern pattern : handler.getPatterns())
        {
            mappings.put(pattern, handler);
        }
        return remove(mappings);
    }

    HandlerMapping<V> remove(Map<Pattern, V> mappings)
    {
        Map<Pattern, Collection<V>> newMappings = getAllMappings();
        removeMappings(mappings, newMappings);
        return new HandlerMapping<V>(newMappings);
    }

    HandlerMapping<V> update(Map<Pattern, V> add, Map<Pattern, V> remove)
    {
        Map<Pattern, Collection<V>> newMappings = getAllMappings();
        removeMappings(remove, newMappings);
        addMappings(add, newMappings);
        return new HandlerMapping<V>(newMappings);
    }

    private void addMappings(Map<Pattern, V> mappings, Map<Pattern, Collection<V>> target)
    {
        for (Map.Entry<Pattern, V> mapping : mappings.entrySet())
        {
            if (!target.containsKey(mapping.getKey()))
            {
                target.put(mapping.getKey(), new TreeSet<V>());
            }
            target.get(mapping.getKey()).add(mapping.getValue());
        }
    }

    private void removeMappings(Map<Pattern, V> mappings, Map<Pattern, Collection<V>> target)
    {
        for (Map.Entry<Pattern, V> mapping : mappings.entrySet())
        {
            Collection<V> mappedHandlers = target.get(mapping.getKey());
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

    private Map<Pattern, Collection<V>> getAllMappings()
    {
        Map<Pattern, Collection<V>> newMappings = new TreeMap<Pattern, Collection<V>>(PatternComparator.INSTANCE);
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
    public Collection<V> values()
    {
        return unmodifiableCollection(mappedHandlers);
    }

    /**
     * Returns whether this mapping contains the specified handler.
     *
     * @return <code>true</code> if the handlers contains the specified handler,
     *         <code>false</code> otherwise
     */
    public boolean contains(V handler)
    {
        return mappedHandlers.contains(handler);
    }

    /**
     * Returns all matching handlers for the given path.
     *
     * @param path the path that should match, cannot be <code>null</code>.
     * @return a {@link Collection} of all matching handlers, never <code>null</code>.
     */
    public List<V> getAllMatches(String path)
    {
        return getAllMatches(path, false /* firstOnly */);
    }

    /**
     * Returns the best matching handler for the given path, according to the rules defined in section 12.1 of Servlet 3.0 specification:
     * <ul>
     * <li>find an exact match of the path of the request to the path of the handler. A successful match selects the handler;</li>
     * <li>recursively try to match the longest path-prefix. This is done by stepping down the path tree a directory at a time, using the
     *     '/' character as a path separator. The longest match determines the servlet selected;</li>
     * <li>if the last segment in the URL path contains an extension (e.g. .jsp), the servlet container will try to match a servlet that
     *     handles requests for the extension. An extension is defined as the part of the last segment after the last '.' character.</li>
     * </ul>
     *
     * @param path the path that should match, cannot be <code>null</code>.
     * @return the best matching handler for the given path, or <code>null</code> in case no handler matched.
     */
    V getBestMatch(String path)
    {
        List<V> allMatches = getAllMatches(path, true /* firstOnly */);
        return allMatches.isEmpty() ? null : allMatches.get(0);
    }

    /**
     * Returns the (first) handler identified by the given name.
     *
     * @param name the name of the handler to return, can be <code>null</code> in which case this method will return <code>null</code>.
     * @return the element with the given name, or <code>null</code> if not found, or the given argument was <code>null</code>.
     */
    V getByName(String name)
    {
        if (name == null)
        {
            return null;
        }

        for (V element : this.mappedHandlers)
        {
            if (name.equals(element.getName()))
            {
                return element;
            }
        }

        return null;
    }

    /**
     * Provides information on whether there are elements mapped or not.
     *
     * @return <code>false</code> if there is at least one element mapped, <code>true</code> otherwise.
     */
    boolean isEmpty()
    {
        return this.mappedHandlers.isEmpty();
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
    private List<V> getAllMatches(String path, boolean firstOnly)
    {
        path = (path == null) ? "" : path.trim();

        Set<V> result = new TreeSet<V>();
        // Look for exact matches only, that is, those patterns without wildcards...
        for (Entry<Pattern, Set<V>> entry : this.exactMap.entrySet())
        {
            Matcher matcher = entry.getKey().matcher(path);
            // !!! we should always match the *entire* pattern, instead of the longest prefix...
            if (matcher.matches())
            {
                Set<V> vs = entry.getValue();
                for (V v : vs)
                {
                    result.add(v);
                    if (firstOnly)
                    {
                        return new ArrayList<V>(result);
                    }
                }
            }
        }

        // Try to apply the wildcard patterns...
        for (Entry<Pattern, Set<V>> entry : this.wildcardMap.entrySet())
        {
            Matcher matcher = entry.getKey().matcher(path);
            if (matcher.find(0))
            {
                Set<V> vs = entry.getValue();
                for (V v : vs)
                {
                    result.add(v);

                    if (firstOnly)
                    {
                        break;
                    }
                }
            }
        }

        return new ArrayList<V>(result);
    }
}
