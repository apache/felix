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
package org.apache.felix.eventadmin.impl.util;

import java.util.ArrayList;
import java.util.List;

public abstract class Matchers {

    private static final char SEP_TOPIC = '/';
    private static final char SEP_PCK = '.';

    public static Matcher[] createEventTopicMatchers(final String[] config)
    {
        final Matcher[] matchers;
        if ( config == null || config.length == 0 )
        {
            matchers = null;
        }
        else
        {
            final List<Matcher> list = new ArrayList<>();
            for(int i=0;i<config.length;i++)
            {
                String value = config[i];
                if ( value != null )
                {
                    value = value.trim();
                }
                if ( value != null && value.length() > 0 )
                {
                    if ( value.endsWith(".") )
                    {
                        list.add(new PackageMatcher(value.substring(0, value.length() - 1), SEP_TOPIC));
                    }
                    else if ( value.endsWith("*") )
                    {
                        if ( value.equals("*") )
                        {
                            return new Matcher[] {new MatcherAll()};
                        }
                        list.add(new SubPackageMatcher(value.substring(0, value.length() - 1), SEP_TOPIC));
                    }
                    else
                    {
                        list.add(new ClassMatcher(value));
                    }
                }
            }
            if ( list.size() > 0 )
            {
                matchers = list.toArray(new Matcher[list.size()]);
            }
            else
            {
                matchers = null;
            }
        }
        return matchers;
    }

    public static Matcher[] createPackageMatchers(final String[] ignoreTimeout)
    {
        final Matchers.Matcher[] ignoreTimeoutMatcher;
        if ( ignoreTimeout == null || ignoreTimeout.length == 0 )
        {
            ignoreTimeoutMatcher = null;
        }
        else
        {
            ignoreTimeoutMatcher = new Matchers.Matcher[ignoreTimeout.length];
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
                        ignoreTimeoutMatcher[i] = new PackageMatcher(value.substring(0, value.length() - 1), SEP_PCK);
                    }
                    else if ( value.endsWith("*") )
                    {
                        ignoreTimeoutMatcher[i] = new SubPackageMatcher(value.substring(0, value.length() - 1), SEP_PCK);
                    }
                    else
                    {
                        ignoreTimeoutMatcher[i] = new ClassMatcher(value);
                    }
                }
            }
        }
        return ignoreTimeoutMatcher;
    }

    /**
     * The matcher interface for checking if timeout handling
     * is disabled for the handler.
     * Matching is based on the class name of the event handler.
     */
    public interface Matcher
    {
        boolean match(String className);
    }

    /** Match all. */
    private static final class MatcherAll implements Matcher
    {
        @Override
        public boolean match(final String className)
        {
            return true;
        }
    }

    /** Match a package. */
    private static final class PackageMatcher implements Matcher
    {
        private final String packageName;

        private final char sep;


        public PackageMatcher(final String name, final char sep)
        {
            this.packageName = name;
            this.sep = sep;
        }
        @Override
        public boolean match(final String className)
        {
            final int pos = className.lastIndexOf(sep);
            return pos > -1 && className.substring(0, pos).equals(packageName);
        }
    }

    /** Match a package or sub package. */
    private static final class SubPackageMatcher implements Matcher
    {
        private final String packageName;

        private final char sep;

        public SubPackageMatcher(final String name, final char sep)
        {
            this.packageName = name + sep;
            this.sep = sep;
        }

        @Override
        public boolean match(final String className)
        {
            final int pos = className.lastIndexOf(sep);
            return pos > -1 && className.substring(0, pos + 1).startsWith(packageName);
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
        @Override
        public boolean match(final String className)
        {
            return m_className.equals(className);
        }
    }
}
