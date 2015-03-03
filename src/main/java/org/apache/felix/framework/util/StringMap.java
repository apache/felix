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
package org.apache.felix.framework.util;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple utility class that creates a map for string-based keys.
 * This map can be set to use case-sensitive or case-insensitive
 * comparison when searching for the key.  Any keys put into this
 * map will be converted to a <tt>String</tt> using the
 * <tt>toString()</tt> method, since it is only intended to
 * compare strings.
 **/
public class StringMap extends TreeMap<String, Object>
{
    private static final CaseInsensitiveComparator COMPARATOR = new CaseInsensitiveComparator();

    public StringMap()
    {
        super(COMPARATOR);
    }

    public StringMap(Map<?, ?> map)
    {
        super(COMPARATOR);
        for (Map.Entry<?, ?> e : map.entrySet())
        {
            put(e.getKey().toString(), e.getValue());
        }
    }

    private static class CaseInsensitiveComparator implements Comparator<String>
    {

        public int compare(String s1, String s2)
        {
            int n1 = s1.length();
            int n2 = s2.length();
            int min = n1 < n2 ? n1 : n2;
            for ( int i = 0; i < min; i++ )
            {
                char c1 = s1.charAt( i );
                char c2 = s2.charAt( i );
                if ( c1 != c2 )
                {
                    // Fast check for simple ascii codes
                    if ( c1 <= 128 && c2 <= 128 )
                    {
                        c1 = toLowerCaseFast(c1);
                        c2 = toLowerCaseFast(c2);
                        if ( c1 != c2 )
                        {
                            return c1 - c2;
                        }
                    }
                    else
                    {
                        c1 = Character.toUpperCase( c1 );
                        c2 = Character.toUpperCase( c2 );
                        if ( c1 != c2 )
                        {
                            c1 = Character.toLowerCase( c1 );
                            c2 = Character.toLowerCase( c2 );
                            if ( c1 != c2 )
                            {
                                // No overflow because of numeric promotion
                                return c1 - c2;
                            }
                        }
                    }
                }
            }
            return n1 - n2;
        }
    }

    private static char toLowerCaseFast( char ch )
    {
        return ( ch >= 'A' && ch <= 'Z' ) ? ( char ) ( ch + 'a' - 'A' ) : ch;
    }

}
