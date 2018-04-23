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
package org.apache.felix.http.base.internal.util;

import java.util.StringTokenizer;

/**
 * Some convenience utilities to deal with path patterns.
 */
public abstract class PatternUtil
{

    /**
     * Check for valid servlet pattern
     * @param pattern The pattern
     * @return {@code true} if its valid
     */
    public static boolean isValidPattern(final String pattern)
    {
        if ( pattern == null )
        {
            return false;
        }
        if ( pattern.indexOf("?") != -1 )
        {
            return false;
        }
        // default and root
        if ( pattern.length() == 0 || pattern.equals("/") )
        {
            return true;
        }
        // extension
        if ( pattern.startsWith("*.") )
        {
            return pattern.indexOf("/") == -1;
        }
        if ( !pattern.startsWith("/") )
        {
            return false;
        }
        final int pos = pattern.indexOf('*');
        if ( pos != -1 && pos < pattern.length() - 1 )
        {
            return false;
        }
        if ( pos != -1 && pattern.charAt(pos - 1) != '/')
        {
            return false;
        }
        if ( pattern.charAt(pattern.length() - 1) == '/')
        {
            return false;
        }
        return true;
    }

    // check for valid symbolic name
    public static boolean isValidSymbolicName(final String name)
    {
        if ( name == null || name.isEmpty() )
        {
            return false;
        }
        boolean valid = true;
        boolean expectToken = false;
        boolean done = false;
        final StringTokenizer st = new StringTokenizer(name, ".", true);
        while ( !done && st.hasMoreTokens() )
        {
            final String token = st.nextToken();
            if ( expectToken )
            {
                if ( !".".equals(token) )
                {
                    valid = false;
                    done = true;
                }
                else
                {
                    expectToken = false;
                }
            }
            else
            {
                if ( ".".equals(token) )
                {
                    valid = false;
                    done = true;
                }
                else
                {
                    int i = 0;
                    while ( i < token.length() && valid )
                    {
                        final char c = token.charAt(i);
                        i++;
                        if ( c >= 'a' && c <= 'z' )
                        {
                            continue;
                        }
                        if ( c >= 'A' && c <= 'Z' )
                        {
                            continue;
                        }
                        if ( c >= '0' && c <= '9' )
                        {
                            continue;
                        }
                        if ( c == '-' || c == '_' )
                        {
                            continue;
                        }
                        valid = false;
                        done = true;
                    }
                }
                expectToken = true;
            }
        }
        if ( !expectToken )
        {
            valid = false;
        }

        return valid;
    }
}
