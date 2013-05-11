/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ContextListener;
import org.apache.felix.ipojo.ContextSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * This class manages context-source management.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ContextSourceManager implements ContextListener {

    /**
     * The variable prefix
     */
    public static final String VARIABLE_START = "${";

    /**
     * The variable end
     */
    public static final char VARIABLE_END = '}';

    /**
     * List of characters forbidden in variable names
     */
    public static final List<Character> FORBIDDEN_CHARACTERS = Arrays.asList(
            '(', ')', '&', '|', '!', '=', '*', ' ');

    /**
     * Managed dependency.
     */
    private DependencyModel m_dependency;
    /**
     * List of monitored context sources.
     */
    private List<ContextSource> m_sources = new ArrayList<ContextSource>(1);
    /**
     * Variables contained in the original filter.
     */
    private List<String> m_variables;
    /**
     * Original filter (containing variables).
     */
    private String m_filter;
    /**
     * Bundle context.
     */
    private BundleContext m_context;
    /**
     * The Context-Source service tracker.
     */
    private Tracker m_tracker;

    /**
     * Creates the context source manager.
     *
     * @param dependency the dependency model on which this manager is plugged.
     */
    public ContextSourceManager(DependencyModel dependency) throws InvalidSyntaxException {
        m_filter = dependency.getFilter();
        m_variables = extractVariablesFromFilter(m_filter);
        m_dependency = dependency;
        m_context = dependency.getComponentInstance().getContext();
    }

    /**
     * This method substitutes ${var} substring by values stored in a map.
     *
     * @param str    : string with variables
     * @param values : dictionary containing the variable name and the value.
     * @return resulted string
     */
    public static String substitute(String str, Dictionary values) {
        int len = str.length();
        StringBuilder builder = new StringBuilder(len);

        int prev = 0;
        int start = str.indexOf("${");
        int end = str.indexOf('}', start);
        while (start != -1 && end != -1) {
            String key = str.substring(start + 2, end);
            Object value = values.get(key);
            if (value == null) {
                builder.append(str.substring(prev, end + 1));
            } else {
                builder.append(str.substring(prev, start));
                builder.append(value);
            }
            prev = end + 1;
            if (prev >= str.length()) {
                break;
            }

            start = str.indexOf("${", prev);
            if (start != -1) {
                end = str.indexOf('}', start);
            }
        }

        builder.append(str.substring(prev));

        return builder.toString();
    }

    /**
     * Extracts the variables (${name}) from the given filter.
     *
     * @param filter : string form of the filter.
     * @return the list of found properties.
     * @throws InvalidSyntaxException thrown when the variables are not consistent.
     */
    public static List<String> extractVariablesFromFilter(String filter) throws InvalidSyntaxException {
        List<String> variables = new ArrayList<String>();
        int prev;
        int start = filter.indexOf(VARIABLE_START);
        int end = filter.indexOf(VARIABLE_END, start);
        while (start != -1) {
            // Unfinished variable
            if (end == -1) {
                throw new InvalidSyntaxException("The filter contains an unfinished variable", filter);
            }

            String key = filter.substring(start + VARIABLE_START.length(), end);

            // Error detection :
            // Empty variable.
            if (key.length() == 0) {
                throw new InvalidSyntaxException("The filter variable '${}' is not a valid " +
                        "variable", filter);
            }
            // Variable with spaces.
            Character forbidden = containsForbiddenCharacter(key);
            if (forbidden != null) {
                throw new InvalidSyntaxException("The filter variable '${" + key + "}' contains a forbidden " +
                        "character : '" + forbidden + "'", filter);
            }


            variables.add(key);
            prev = end + 1;
            if (prev >= filter.length()) {
                break;
            }

            start = filter.indexOf(VARIABLE_START, prev);
            if (start != -1) {
                end = filter.indexOf(VARIABLE_END, start);
            }
        }

        return variables;
    }

    private static Character containsForbiddenCharacter(String key) {
        for (Character character : FORBIDDEN_CHARACTERS) {
            if (key.indexOf(character) != -1) {
                return character;
            }
        }
        // Safe key.
        return null;
    }

    /**
     * Start the context management.
     */
    public void start() {
        if (m_tracker == null) {
            m_tracker = new Tracker(m_context, ContextSource.class.getName(), new SourceTracker());
        }
        m_tracker.open();
        computeFilter();
    }

    /**
     * Stop the context management.
     */
    public void stop() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
        // Reinitialize the filter.
        setFilter(m_filter);
        for (ContextSource source : m_sources) {
            source.unregisterContextListener(this);
        }
        m_sources.clear();
    }

    /**
     * Set the filter of the managed dependency.
     *
     * @param filter : the new filter to apply
     */
    private void setFilter(String filter) {
        if (!filter.equals(m_dependency.getFilter())) {
            // Reconfigure
            try {
                m_dependency.setFilter(m_context.createFilter(filter));
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("A context filter is invalid : " + filter, e);
            }
        }
    }

    /**
     * Compute the new filter.
     */
    private void computeFilter() {
        String fil = m_filter; // Gets a copy.
        synchronized (this) {
            for (ContextSource m_source : m_sources) {
                Dictionary props = m_source.getContext();
                fil = substitute(fil, props); //NOPMD
            }
        }
        if (!fil.equals(m_dependency.getFilter())) {
            setFilter(fil);
        }
    }

    /**
     * A context source has modified a monitored property.
     *
     * @param source   : source
     * @param property : modified property
     * @param value    : new value.
     * @see org.apache.felix.ipojo.ContextListener#update(org.apache.felix.ipojo.ContextSource, String, Object)
     */
    public synchronized void update(ContextSource source, String property, Object value) {
        computeFilter();
    }

    /**
     * A context source appears.
     *
     * @param source : new context source.
     */
    public void addContextSource(ContextSource source) {
        m_sources.add(source);
        computeFilter();
        source.registerContextListener(this, m_variables.toArray(new String[m_variables.size()]));
    }

    /**
     * A context source disappears.
     *
     * @param source : leaving context source.
     */
    public void removeContextSource(ContextSource source) {
        source.unregisterContextListener(this);
        m_sources.remove(source);
        computeFilter();
    }

    private class SourceTracker implements TrackerCustomizer {
        /**
         * A new context-source was added.
         * This method inject the context-source object in the source manager.
         *
         * @param reference : service reference.
         * @see TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
         */
        public void addedService(ServiceReference reference) {
            System.out.println("Source added");
            addContextSource((ContextSource) m_tracker.getService(reference));
        }

        /**
         * A new context-source is adding in the tracker..
         *
         * @param reference : service reference
         * @return true.
         * @see TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
         */
        public boolean addingService(ServiceReference reference) {
            return true;
        }

        /**
         * A used context-source is modified.
         *
         * @param reference : service reference.
         * @param service   : service object.
         * @see TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, Object)
         */
        public void modifiedService(ServiceReference reference, Object service) {
            // Nothing to do.
        }

        /**
         * A used context-source disappears.
         * This method notify the Source Manager in order to manage this departure.
         *
         * @param reference : service reference.
         * @param service   : service object.
         * @see TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            removeContextSource((ContextSource) service);
        }

    }

}
