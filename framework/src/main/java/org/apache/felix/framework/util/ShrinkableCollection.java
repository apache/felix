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

import java.util.Collection;
import java.util.Iterator;

/**
 * A collection wrapper that only permits clients to shrink the collection.
**/
public class ShrinkableCollection<T> implements Collection<T>
{
    private final Collection<T> m_delegate;

    public ShrinkableCollection(Collection<T> delegate)
    {
        m_delegate = delegate;
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean add(T o)
    {
        throw new UnsupportedOperationException();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean addAll(Collection<? extends T> c)
    {
        throw new UnsupportedOperationException();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void clear()
    {
        m_delegate.clear();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean contains(Object o)
    {
        return m_delegate.contains(o);
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean containsAll(Collection<?> c)
    {
        return m_delegate.containsAll(c);
    }

    @Override
    public boolean equals(Object o)
    {
        return m_delegate.equals(o);
    }

    @Override
    public int hashCode()
    {
        return m_delegate.hashCode();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }

<<<<<<< HEAD
    public Iterator iterator()
=======
    @Override
    public Iterator<T> iterator()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return m_delegate.iterator();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean remove(Object o)
    {
        return m_delegate.remove(o);
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean removeAll(Collection<?> c)
    {
        return m_delegate.removeAll(c);
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean retainAll(Collection<?> c)
    {
        return m_delegate.retainAll(c);
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public int size()
    {
        return m_delegate.size();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Object[] toArray()
    {
        return m_delegate.toArray();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public <A> A[] toArray(A[] a)
    {
        return m_delegate.toArray(a);
    }
}