/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollectionUtils {
    public static <T extends Comparable<?>> SortedSet<T> sortedUnion(Collection<? extends T>... collections)
    {
        return sortedUnion(null, collections);
    }

    public static <T> SortedSet<T> sortedUnion(Comparator<T> comparator, Collection<? extends T>... collections)
    {
        SortedSet<T> union = comparator == null ? new TreeSet<T>() : new TreeSet<T>(comparator);
        for (Collection<? extends T> collection : collections)
        {
            union.addAll(collection);
        }
        return union;
    }
}
