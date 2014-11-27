/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect.felix.framework.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * A collection wrapper that only permits clients to shrink the collection.
 */
public class ShrinkableCollection<T> extends AbstractCollection<T>
{
    private final Collection<T> m_delegate;

    public ShrinkableCollection(Collection<T> delegate)
    {
        m_delegate = delegate;
    }

    @Override
    public Iterator<T> iterator()
    {
        return m_delegate.iterator();
    }

    @Override
    public int size()
    {
        return m_delegate.size();
    }

}
