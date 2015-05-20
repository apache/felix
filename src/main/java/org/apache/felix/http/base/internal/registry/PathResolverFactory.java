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
package org.apache.felix.http.base.internal.registry;

/**
 * The path resolver factory creates a path resolver for a pattern.
 * The servlet spec supports different patterns
 * - path mapping, a pattern starting with / and ending with /*
 * - extension mapping, a pattern starting with *.
 * - default mapping, the pattern /
 * - root mapping, the pattern is the empty string
 * - exact match
 *
 * Exact match is tried first, followed by longest match and finally
 * extension match.
 */
public abstract class PathResolverFactory {

    public static PathResolver create(final String pattern)
    {
        if ( pattern.length() == 0 )
        {
            return new RootMatcher();
        }
        else if ( pattern.equals("/") )
        {
            return new DefaultMatcher();
        }
        else if ( pattern.startsWith("*.") )
        {
            return new ExtensionMatcher(pattern.substring(1));
        }
        else if ( pattern.endsWith("/*") )
        {
            return new PathMatcher(pattern);
        }
        return new ExactAndPathMatcher(pattern);
    }

    public static abstract class AbstractMatcher implements PathResolver
    {
        private final int ranking;

        public AbstractMatcher(final int ranking)
        {
            this.ranking = ranking;
        }

        @Override
        public int compareTo(final PathResolver o) {
            int result = o.getRanking() - this.ranking;
            if ( result == 0 )
            {
                result = o.getOrdering() - this.getOrdering();
            }
            return result;
        }

        @Override
        public int getRanking() {
            return this.ranking;
        }

        @Override
        public int getOrdering() {
            return 0;
        }
    }

    public static final class RootMatcher extends AbstractMatcher
    {
        public RootMatcher()
        {
            super(2);
        }

        @Override
        public PathResolution match(final String uri) {
            if ( uri.length() == 0 )
            {
                final PathResolution pr = new PathResolution();
                pr.pathInfo = "/";
                pr.servletPath = "";
                pr.requestURI = "";

                return pr;
            }
            return null;
        }
    }

    public static final class DefaultMatcher extends AbstractMatcher
    {
        public DefaultMatcher()
        {
            super(1);
        }

        @Override
        public PathResolution match(final String uri) {
            final PathResolution pr = new PathResolution();
            pr.pathInfo = null;
            pr.servletPath = uri;
            pr.requestURI = uri;

            return pr;
        }
    }

    public static final class ExactAndPathMatcher extends AbstractMatcher
    {
        private final String path;

        private final String prefix;

        public ExactAndPathMatcher(final String pattern)
        {
            super(4);
            this.path = pattern;
            this.prefix = pattern.concat("/");
        }

        @Override
        public PathResolution match(final String uri) {
            if ( uri.equals(this.path) )
            {
                final PathResolution pr = new PathResolution();
                pr.pathInfo = null;
                pr.servletPath = uri;
                pr.requestURI = uri;

                return pr;
            }
            else if ( uri.startsWith(prefix) )
            {
                final PathResolution pr = new PathResolution();
                pr.servletPath = this.prefix.substring(0, this.prefix.length() - 1);
                pr.pathInfo = uri.substring(pr.servletPath.length());
                pr.requestURI = uri;

                return pr;
            }
            return null;
        }

        @Override
        public int getOrdering()
        {
            return this.path.length();
        }
    }

    public static final class PathMatcher extends AbstractMatcher
    {
        private final String prefix;

        public PathMatcher(final String pattern)
        {
            super(4);
            this.prefix = pattern.substring(0, pattern.length() - 1);
        }

        @Override
        public PathResolution match(final String uri) {
            if ( uri.startsWith(this.prefix) )
            {
                final PathResolution pr = new PathResolution();
                pr.servletPath = this.prefix.substring(0, this.prefix.length() - 1);
                pr.pathInfo = uri.substring(pr.servletPath.length());
                pr.requestURI = uri;

                return pr;
            }
            return null;
        }

        @Override
        public int getOrdering()
        {
            return this.prefix.length() + 1;
        }
    }

    public static final class ExtensionMatcher extends AbstractMatcher
    {
        private final String extension;

        public ExtensionMatcher(final String extension)
        {
            super(3);
            this.extension = extension;
        }

        @Override
        public PathResolution match(final String uri) {
            if ( uri.endsWith(this.extension) )
            {
                final PathResolution pr = new PathResolution();
                pr.pathInfo = null;
                pr.servletPath = uri;
                pr.requestURI = uri;

                return pr;
            }
            return null;
        }

        @Override
        public int getOrdering()
        {
            return this.extension.length();
        }
    }
}
