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

import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;

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

    public static @Nonnull PathResolver createPatternMatcher(@CheckForNull final ServletHandler handler, @Nonnull final String pattern)
    {
        if ( pattern.length() == 0 )
        {
            return new RootMatcher(handler);
        }
        else if ( pattern.equals("/") )
        {
            return new DefaultMatcher(handler);
        }
        else if ( pattern.startsWith("*.") )
        {
            return new ExtensionMatcher(handler, pattern);
        }
        else if ( pattern.endsWith("/*") )
        {
            return new PathMatcher(handler, pattern);
        }
        else if ( handler != null && handler.getContextServiceId() == HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID )
        {
            return new ExactAndPathMatcher(handler, pattern);
        }
        return new ExactMatcher(handler, pattern);
    }

    public static @Nonnull PathResolver createRegexMatcher(@Nonnull final String regex)
    {
        return new RegexMatcher(regex);
    }

    public static abstract class AbstractMatcher implements PathResolver
    {
        private final int ranking;

        private final String pattern;

        private final ServletHandler handler;

        public AbstractMatcher(final ServletHandler handler, final String pattern, final int ranking)
        {
            this.handler = handler;
            this.ranking = ranking;
            this.pattern = pattern;
        }

        @Override
        public int compareTo(final PathResolver o)
        {
            int result = o.getRanking() - this.ranking;
            if ( result == 0 )
            {
                result = o.getOrdering() - this.getOrdering();
            }
            return result;
        }

        @Override
        public ServletHandler getServletHandler()
        {
            return this.handler;
        }

        @Override
        public int getRanking()
        {
            return this.ranking;
        }

        @Override
        public int getOrdering()
        {
            return 0;
        }

        @Override
        public String getPattern()
        {
            return this.pattern;
        }

        @Override
        public int hashCode()
        {
            return pattern.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
            {
                return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {
                return false;
            }
            final AbstractMatcher other = (AbstractMatcher) obj;
            return this.pattern.equals(other.pattern);
        }
    }

    public static final class RootMatcher extends AbstractMatcher
    {
        public RootMatcher(final ServletHandler handler)
        {
            super(handler, "", 2);
        }

        @Override
        public PathResolution resolve(final String uri) {
            if ( uri.length() == 0 || uri.equals("/") )
            {
                final PathResolution pr = new PathResolution();
                pr.pathInfo = "/";
                pr.servletPath = "";
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

                return pr;
            }
            return null;
        }
    }

    public static final class DefaultMatcher extends AbstractMatcher
    {
        public DefaultMatcher(final ServletHandler handler)
        {
            super(handler, "/", 1);
        }

        @Override
        public PathResolution resolve(final String uri) {
            final PathResolution pr = new PathResolution();
            pr.pathInfo = null;
            pr.servletPath = uri;
            pr.requestURI = uri;
            pr.handler = this.getServletHandler();

            return pr;
        }
    }

    public static final class ExactAndPathMatcher extends AbstractMatcher
    {
        private final String path;

        private final String prefix;

        public ExactAndPathMatcher(final ServletHandler handler, final String pattern)
        {
            super(handler, pattern, 5);
            this.path = pattern;
            this.prefix = pattern.concat("/");
        }

        @Override
        public PathResolution resolve(final String uri) {
            if ( uri.equals(this.path) )
            {
                final PathResolution pr = new PathResolution();
                pr.pathInfo = null;
                pr.servletPath = uri;
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

                return pr;
            }
            else if ( uri.startsWith(prefix) )
            {
                final PathResolution pr = new PathResolution();
                pr.servletPath = this.prefix.substring(0, this.prefix.length() - 1);
                pr.pathInfo = uri.substring(pr.servletPath.length());
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

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

    public static final class ExactMatcher extends AbstractMatcher
    {
        private final String path;

        public ExactMatcher(final ServletHandler handler, final String pattern)
        {
            super(handler, pattern, 5);
            this.path = pattern;
        }

        @Override
        public PathResolution resolve(final String uri) {
            if ( uri.equals(this.path) )
            {
                final PathResolution pr = new PathResolution();
                pr.servletPath = uri;
                pr.pathInfo = null;
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

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

        private final String path;

        public PathMatcher(final ServletHandler handler, final String pattern)
        {
            super(handler, pattern, 4);
            this.prefix = pattern.substring(0, pattern.length() - 1);
            this.path = pattern.substring(0, pattern.length() - 2);
        }

        @Override
        public PathResolution resolve(final String uri) {
            if ( uri.equals(this.path) )
            {
                final PathResolution pr = new PathResolution();
                pr.servletPath = this.path;
                pr.pathInfo = null;
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

                return pr;
            }
            if ( uri.startsWith(this.prefix) )
            {
                final PathResolution pr = new PathResolution();
                pr.servletPath = this.prefix.substring(0, this.prefix.length() - 1);
                pr.pathInfo = uri.substring(pr.servletPath.length());
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

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

        public ExtensionMatcher(final ServletHandler handler, final String pattern)
        {
            super(handler, pattern, 3);
            this.extension = pattern.substring(1);
        }

        @Override
        public PathResolution resolve(final String uri) {
            if ( uri.endsWith(this.extension) )
            {
                final PathResolution pr = new PathResolution();
                pr.pathInfo = null;
                pr.servletPath = uri;
                pr.requestURI = uri;
                pr.handler = this.getServletHandler();

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

    public static final class RegexMatcher extends AbstractMatcher
    {
        private final Pattern pattern;

        public RegexMatcher(final String regex)
        {
            super(null, regex, 0);
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public @CheckForNull PathResolution resolve(@Nonnull final String uri) {
            if ( pattern.matcher(uri).matches() )
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
            return this.pattern.toString().length();
        }
    }
}
