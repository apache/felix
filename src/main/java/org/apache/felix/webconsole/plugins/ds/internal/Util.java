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
package org.apache.felix.webconsole.plugins.ds.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;

import org.apache.felix.scr.Component;

class Util
{

    static final Comparator COMPONENT_COMPARATOR = new Comparator()
    {
        public int compare(Object o0, Object o1)
        {
            final Component c0 = (Component) o0;
            final Component c1 = (Component) o1;
            final int nameCmp = c0.getName().compareTo(c1.getName());
            if (nameCmp != 0)
            {
                return nameCmp;
            }
            return (c0.getId() < c1.getId()) ? -1 : ((c0.getId() > c1.getId()) ? 1 : 0);
        }
    };

    private Util()
    {
        // prevent instantiation
    }

    /**
     * This method is the same as Collections#list(Enumeration). The reason to
     * duplicate it here, is that it is missing in OSGi/Minimum execution
     * environment.
     *
     * @param e the enumeration which to convert
     * @return the list containing all enumeration entries.
     */
    public static final ArrayList list(Enumeration e)
    {
        ArrayList l = new ArrayList();
        while (e.hasMoreElements())
        {
            l.add(e.nextElement());
        }
        return l;
    }

}
