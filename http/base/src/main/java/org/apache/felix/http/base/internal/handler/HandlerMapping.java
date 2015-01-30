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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Map-like structure that can map path-patterns to servlet/filter handlers, allowing
 * for easy access to those handlers, based on the match rules defined in section 12.1 of Servlet
 * 3.0 specification.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerMapping<V extends AbstractHandler>
{
    /**
     * Compares {@link Pattern}s based on a set of simple rules:
     * <ol>
     * <li>exact matches go first;</li>
     * <li>followed by wildcard path matches;</li>
     * <li>lastly all wildcard extension matches.</li>
     * </ol>
     * <p>
     * Equal matches will first be sorted on length in descending order (longest patterns first),
     * and in case of equal lengths, they are sorted in natural (ascending) order.
     * </p>
     */
    static class PatternComparator implements Comparator<Pattern>
    {
        @Override
        public int compare(Pattern p1, Pattern p2)
        {
            String ps1 = p1.pattern();
            String ps2 = p2.pattern();

            // Sorts wildcard path matches before wildcard extension matches...
            int r;
            if (isWildcardPath(ps1))
            {
                if (isWildcardPath(ps2))
                {
                    // Descending on length...
                    r = ps2.length() - ps1.length();
                }
                else
                {
                    // Exact matches go first...
                    r = isWildcardExtension(ps2) ? -1 : 1;
                }
            }
            else if (isWildcardExtension(ps1))
            {
                if (isWildcardExtension(ps2))
                {
                    // Descending on length...
                    r = ps2.length() - ps1.length();
                }
                else
                {
                    // Wildcard paths & exact matches go first...
                    r = 1;
                }
            }
            else
            {
                if (isWildcardExtension(ps2) || isWildcardPath(ps2))
                {
                    // Exact matches go first...
                    r = -1;
                }
                else
                {
                    // Descending on length...
                    r = ps2.length() - ps1.length();
                }
            }

            if (r == 0)
            {
                // In case of a draw, ensure we sort in a predictable (ascending) order...
                r = ps1.compareTo(ps2);
            }

            return r;
        }

        private boolean isWildcardExtension(String p)
        {
            return p.startsWith("^(.*");
        }

        private boolean isWildcardPath(String p)
        {
            return p.startsWith("^(/");
        }
    }

    private final SortedMap<Pattern, List<V>> exactMap;
    private final SortedMap<Pattern, List<V>> wildcardMap;
    private final Set<V> all;

    /**
     * Creates a new, empty, {@link HandlerMapping} instance.
     */
    public HandlerMapping()
    {
        this(Collections.<V> emptyList());
    }

    /**
     * Creates a new {@link HandlerMapping} instance for the given elements.
     *
     * @param elements the elements to map, cannot be <code>null</code>.
     */
    public HandlerMapping(Collection<V> elements)
    {
        this.exactMap = new TreeMap<Pattern, List<V>>(new PatternComparator());
        this.wildcardMap = new TreeMap<Pattern, List<V>>(new PatternComparator());
        this.all = new HashSet<V>(elements);

        for (V element : elements)
        {
            for (Pattern pattern : element.getPatterns())
            {
                if (isWildcardPattern(pattern))
                {
                    List<V> vs = this.wildcardMap.get(pattern);
                    if (vs == null)
                    {
                        vs = new ArrayList<V>();
                        this.wildcardMap.put(pattern, vs);
                    }
                    if (!vs.contains(element))
                    {
                        vs.add(element);
                    }
                }
                else
                {
                    List<V> vs = this.exactMap.get(pattern);
                    if (vs == null)
                    {
                        vs = new ArrayList<V>();
                        this.exactMap.put(pattern, vs);
                    }
                    if (!vs.contains(element))
                    {
                        vs.add(element);
                    }
                }
            }
        }
    }

    /**
     * Returns all mapped elements.
     *
     * @return a collection of mapped elements, never <code>null</code>.
     */
    public Collection<V> getAllElements()
    {
        return this.all;
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
    public V getBestMatch(String path)
    {
        List<V> allMatches = getAllMatches(path, true /* firstOnly */);
        return allMatches.isEmpty() ? null : allMatches.get(0);
    }

    /**
     * Returns the (first) handler identified by the given name.
     * @param name the name of the handler to return, can be <code>null</code> in which case this method will return <code>null</code>.
     * @return the element with the given name, or <code>null</code> if not found, or the given argument was <code>null</code>.
     */
    public V getByName(String name)
    {
        if (name == null)
        {
            return null;
        }

        for (V element : this.all)
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
     * @return <code>true</code> if there is at least one element mapped, <code>false</code> otherwise.
     */
    public boolean hasElements()
    {
        return !this.all.isEmpty();
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

        List<V> result = new ArrayList<V>();
        // Look for exact matches only, that is, those patterns without wildcards...
        for (Entry<Pattern, List<V>> entry : this.exactMap.entrySet())
        {
            Matcher matcher = entry.getKey().matcher(path);
            // !!! we should always match the *entire* pattern, instead of the longest prefix...
            if (matcher.matches())
            {
                List<V> vs = entry.getValue();
                for (V v : vs)
                {
                    if (!result.contains(v))
                    {
                        result.add(v);
                    }

                    if (firstOnly)
                    {
                        return result;
                    }
                }
            }
        }

        // Try to apply the wildcard patterns...
        for (Entry<Pattern, List<V>> entry : this.wildcardMap.entrySet())
        {
            Matcher matcher = entry.getKey().matcher(path);
            if (matcher.find(0))
            {
                List<V> vs = entry.getValue();
                for (V v : vs)
                {
                    if (!result.contains(v))
                    {
                        result.add(v);
                    }

                    if (firstOnly)
                    {
                        return result;
                    }
                }
            }
        }

        // Make sure the results are properly sorted...
        Collections.sort(result);

        return result;
    }

    static boolean isWildcardPattern(Pattern p)
    {
        return p.pattern().contains(".*");
    }
}
