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
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PatternUtil
{

    public static String convertToRegEx(String pattern)
    {
        String result = pattern;
        // see Servlet 3.0, section 12.2
        // - replace '*.' prefixes with a regex that matches extensions...
        result = result.replaceFirst("^\\Q*.\\E(.*)$", "\\^(.*)(\\\\.\\\\Q$1\\\\E)\\$");
        // - replace '/*' suffixes with a regex that matches paths (actually,
        //   the path should also start with a leading slash, but we're a bit
        //   more liberal on this one)...
        result = result.replaceFirst("^(.*)\\Q/*\\E$", "\\^($1)(|/.*)\\$");
        return result;
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
