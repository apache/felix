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
package org.apache.felix.gogo.runtime;

import java.util.AbstractList;
import java.util.List;

/**
 * List that overrides toString() for implicit $args expansion.
 * Also checks for index out of bounds, so that $1 evaluates to null
 * rather than throwing IndexOutOfBoundsException.
 * e.g. x = { a$args }; x 1 2 => a1 2 and not a[1, 2]
 */
public class ArgList extends AbstractList<Object>
{
    private List<?> list;

    public ArgList(List<?> args)
    {
        this.list = args;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for (Object o : list)
        {
            if (buf.length() > 0)
                buf.append(' ');
            buf.append(o);
        }
        return buf.toString();
    }

    @Override
    public Object get(int index)
    {
        return index < list.size() ? list.get(index) : null;
    }

    @Override
    public Object remove(int index)
    {
        return list.remove(index);
    }

    @Override
    public int size()
    {
        return list.size();
    }
}
