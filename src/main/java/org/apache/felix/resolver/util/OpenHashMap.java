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
package org.apache.felix.resolver.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Based on fastutil Object2ObjectLinkedOpenHashMap
 */
@SuppressWarnings("NullableProblems")
public class OpenHashMap<K, V> implements Serializable, Cloneable, SortedMap<K, V> {

    private static final long serialVersionUID = 0L;
    protected transient Object[] key;
    protected transient Object[] value;
    protected transient int mask;
    protected transient boolean containsNullKey;
    protected transient int first;
    protected transient int last;
    protected transient long[] link;
    protected transient int n;
    protected transient int maxFill;
    protected int size;
    protected final float f;
    protected V defRetValue;

    protected transient Iterable<Map.Entry<K, V>> fast;
    protected transient SortedSet<Map.Entry<K, V>> entries;
    protected transient SortedSet<K> keys;
    protected transient Collection<V> values;

    public OpenHashMap(int expected, float f) {
        this.first = -1;
        this.last = -1;
        if (f > 0.0F && f <= 1.0F) {
            if (expected < 0) {
                throw new IllegalArgumentException("The expected number of elements must be nonnegative");
            } else {
                this.f = f;
                this.n = arraySize(expected, f);
                this.mask = this.n - 1;
                this.maxFill = maxFill(this.n, f);
                this.key = new Object[this.n + 1];
                this.value = new Object[this.n + 1];
                this.link = new long[this.n + 1];
            }
        } else {
            throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than or equal to 1");
        }
    }

    public OpenHashMap(int expected) {
        this(expected, 0.75F);
    }

    public OpenHashMap() {
        this(16, 0.75F);
    }

    public OpenHashMap(Map<? extends K, ? extends V> m, float f) {
        this(m.size(), f);
        this.putAll(m);
    }

    public OpenHashMap(Map<? extends K, ? extends V> m) {
        this(m, 0.75F);
    }

    public OpenHashMap(K[] k, V[] v, float f) {
        this(k.length, f);
        if (k.length != v.length) {
            throw new IllegalArgumentException("The key array and the value array have different lengths (" + k.length + " and " + v.length + ")");
        } else {
            for (int i = 0; i < k.length; ++i) {
                this.put(k[i], v[i]);
            }

        }
    }

    public OpenHashMap(K[] k, V[] v) {
        this(k, v, 0.75F);
    }

    public void defaultReturnValue(V rv) {
        this.defRetValue = rv;
    }

    public V defaultReturnValue() {
        return this.defRetValue;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Map)) {
            return false;
        } else {
            Map m = (Map) o;
            int n = m.size();
            if (this.size() != n) {
                return false;
            }
            Iterator<? extends Entry<?, ?>> i = this.fast().iterator();
            while (n-- > 0) {
                Entry e = i.next();
                Object k = e.getKey();
                Object v = e.getValue();
                Object v2 = m.get(k);
                if (v == null) {
                    if (v2 != null) {
                        return false;
                    }
                } else if (!v.equals(v2)) {
                    return false;
                }
            }
            return true;
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        Iterator<Map.Entry<K, V>> i = this.fast().iterator();
        int n = this.size();
        boolean first = true;
        s.append("{");

        while (n-- != 0) {
            if (first) {
                first = false;
            } else {
                s.append(", ");
            }

            Map.Entry<K, V> e = i.next();
            if (this == e.getKey()) {
                s.append("(this map)");
            } else {
                s.append(String.valueOf(e.getKey()));
            }

            s.append("=>");
            if (this == e.getValue()) {
                s.append("(this map)");
            } else {
                s.append(String.valueOf(e.getValue()));
            }
        }

        s.append("}");
        return s.toString();
    }

    private int realSize() {
        return this.containsNullKey ? this.size - 1 : this.size;
    }

    private void ensureCapacity(int capacity) {
        int needed = arraySize(capacity, this.f);
        if (needed > this.n) {
            this.rehash(needed);
        }

    }

    private void tryCapacity(long capacity) {
        int needed = (int) Math.min(1073741824L, Math.max(2L, nextPowerOfTwo((long) Math.ceil((double) ((float) capacity / this.f)))));
        if (needed > this.n) {
            this.rehash(needed);
        }

    }

    @SuppressWarnings("unchecked")
    private V removeEntry(int pos) {
        Object oldValue = this.value[pos];
        this.value[pos] = null;
        --this.size;
        this.fixPointers(pos);
        this.shiftKeys(pos);
        if (this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
        }

        return (V) oldValue;
    }

    @SuppressWarnings("unchecked")
    private V removeNullEntry() {
        this.containsNullKey = false;
        Object oldValue = this.value[this.n];
        this.value[this.n] = null;
        --this.size;
        this.fixPointers(this.n);
        if (this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
        }

        return (V) oldValue;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        if ((double) this.f <= 0.5D) {
            this.ensureCapacity(m.size());
        } else {
            this.tryCapacity((long) (this.size() + m.size()));
        }

        int n = m.size();
        if (m instanceof OpenHashMap) {
            Iterator<? extends Map.Entry<? extends K, ? extends V>> i = ((OpenHashMap) m).fast().iterator();
            while (n-- != 0) {
                Map.Entry<? extends K, ? extends V> e = i.next();
                this.put(e.getKey(), e.getValue());
            }
        } else {
            Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator();
            while (n-- != 0) {
                Map.Entry<? extends K, ? extends V> e = i.next();
                this.put(e.getKey(), e.getValue());
            }
        }
    }

    private int insert(K k, V v) {
        int pos;
        if (k == null) {
            if (this.containsNullKey) {
                return this.n;
            }

            this.containsNullKey = true;
            pos = this.n;
        } else {
            Object[] key = this.key;
            Object curr;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) != null) {
                if (curr.equals(k)) {
                    return pos;
                }

                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (curr.equals(k)) {
                        return pos;
                    }
                }
            }

            key[pos] = k;
        }

        this.value[pos] = v;
        if (this.size == 0) {
            this.first = this.last = pos;
            this.link[pos] = -1L;
        } else {
            this.link[this.last] ^= (this.link[this.last] ^ (long) pos & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            this.link[pos] = ((long) this.last & 0xFFFFFFFFL) << 32 | 0xFFFFFFFFL;
            this.last = pos;
        }

        if (this.size++ >= this.maxFill) {
            this.rehash(arraySize(this.size + 1, this.f));
        }

        return -1;
    }

    @SuppressWarnings("unchecked")
    public V put(K k, V v) {
        int pos = this.insert(k, v);
        if (pos < 0) {
            return this.defRetValue;
        } else {
            Object oldValue = this.value[pos];
            this.value[pos] = v;
            return (V) oldValue;
        }
    }

    @SuppressWarnings("unchecked")
    public V getOrCompute(K k) {
        int pos;
        if (k == null) {
            if (this.containsNullKey) {
                return (V) this.value[this.n];
            }

            this.containsNullKey = true;
            pos = this.n;
        } else {
            Object[] key = this.key;
            Object curr;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) != null) {
                if (curr.equals(k)) {
                    return (V) this.value[pos];
                }

                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (curr.equals(k)) {
                        return (V) this.value[pos];
                    }
                }
            }

            key[pos] = k;
        }

        Object v;
        this.value[pos] = (v = compute(k));
        if (this.size == 0) {
            this.first = this.last = pos;
            this.link[pos] = -1L;
        } else {
            this.link[this.last] ^= (this.link[this.last] ^ (long) pos & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            this.link[pos] = ((long) this.last & 0xFFFFFFFFL) << 32 | 0xFFFFFFFFL;
            this.last = pos;
        }

        if (this.size++ >= this.maxFill) {
            this.rehash(arraySize(this.size + 1, this.f));
        }

        return (V) v;
    }

    protected V compute(K k) {
        throw new UnsupportedOperationException();
    }

    protected final void shiftKeys(int pos) {
        Object[] key = this.key;

        label32:
        while (true) {
            int last = pos;

            Object curr;
            for (pos = pos + 1 & this.mask; (curr = key[pos]) != null; pos = pos + 1 & this.mask) {
                int slot = mix(curr.hashCode()) & this.mask;
                if (last <= pos) {
                    if (last < slot && slot <= pos) {
                        continue;
                    }
                } else if (last < slot || slot <= pos) {
                    continue;
                }

                key[last] = curr;
                this.value[last] = this.value[pos];
                this.fixPointers(pos, last);
                continue label32;
            }

            key[last] = null;
            this.value[last] = null;
            return;
        }
    }

    public V remove(Object k) {
        if (k == null) {
            return this.containsNullKey ? this.removeNullEntry() : this.defRetValue;
        } else {
            Object[] key = this.key;
            Object curr;
            int pos;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) == null) {
                return this.defRetValue;
            } else if (k.equals(curr)) {
                return this.removeEntry(pos);
            } else {
                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (k.equals(curr)) {
                        return this.removeEntry(pos);
                    }
                }

                return this.defRetValue;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private V setValue(int pos, V v) {
        Object oldValue = this.value[pos];
        this.value[pos] = v;
        return (V) oldValue;
    }

    @SuppressWarnings("unchecked")
    public V removeFirst() {
        if (this.size == 0) {
            throw new NoSuchElementException();
        } else {
            int pos = this.first;
            this.first = (int) this.link[pos];
            if (0 <= this.first) {
                this.link[this.first] |= 0xFFFFFFFF00000000L;
            }

            --this.size;
            Object v = this.value[pos];
            if (pos == this.n) {
                this.containsNullKey = false;
                this.value[this.n] = null;
            } else {
                this.shiftKeys(pos);
            }

            if (this.size < this.maxFill / 4 && this.n > 16) {
                this.rehash(this.n / 2);
            }

            return (V) v;
        }
    }

    @SuppressWarnings("unchecked")
    public V removeLast() {
        if (this.size == 0) {
            throw new NoSuchElementException();
        } else {
            int pos = this.last;
            this.last = (int) (this.link[pos] >>> 32);
            if (0 <= this.last) {
                this.link[this.last] |= 0xFFFFFFFFL;
            }

            --this.size;
            Object v = this.value[pos];
            if (pos == this.n) {
                this.containsNullKey = false;
                this.value[this.n] = null;
            } else {
                this.shiftKeys(pos);
            }

            if (this.size < this.maxFill / 4 && this.n > 16) {
                this.rehash(this.n / 2);
            }

            return (V) v;
        }
    }

    private void moveIndexToFirst(int i) {
        if (this.size != 1 && this.first != i) {
            if (this.last == i) {
                this.last = (int) (this.link[i] >>> 32);
                this.link[this.last] |= 0xFFFFFFFFL;
            } else {
                long linki = this.link[i];
                int prev = (int) (linki >>> 32);
                int next = (int) linki;
                this.link[prev] ^= (this.link[prev] ^ linki & 0xFFFFFFFFL) & 0xFFFFFFFFL;
                this.link[next] ^= (this.link[next] ^ linki & 0xFFFFFFFF00000000L) & 0xFFFFFFFF00000000L;
            }

            this.link[this.first] ^= (this.link[this.first] ^ ((long) i & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
            this.link[i] = 0xFFFFFFFF00000000L | (long) this.first & 0xFFFFFFFFL;
            this.first = i;
        }
    }

    private void moveIndexToLast(int i) {
        if (this.size != 1 && this.last != i) {
            if (this.first == i) {
                this.first = (int) this.link[i];
                this.link[this.first] |= 0xFFFFFFFF00000000L;
            } else {
                long linki = this.link[i];
                int prev = (int) (linki >>> 32);
                int next = (int) linki;
                this.link[prev] ^= (this.link[prev] ^ linki & 0xFFFFFFFFL) & 0xFFFFFFFFL;
                this.link[next] ^= (this.link[next] ^ linki & 0xFFFFFFFF00000000L) & 0xFFFFFFFF00000000L;
            }

            this.link[this.last] ^= (this.link[this.last] ^ (long) i & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            this.link[i] = ((long) this.last & 0xFFFFFFFFL) << 32 | 0xFFFFFFFFL;
            this.last = i;
        }
    }

    @SuppressWarnings("unchecked")
    public V getAndMoveToFirst(K k) {
        if (k == null) {
            if (this.containsNullKey) {
                this.moveIndexToFirst(this.n);
                return (V) this.value[this.n];
            } else {
                return this.defRetValue;
            }
        } else {
            Object[] key = this.key;
            Object curr;
            int pos;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) == null) {
                return this.defRetValue;
            } else if (k.equals(curr)) {
                this.moveIndexToFirst(pos);
                return (V) this.value[pos];
            } else {
                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (k.equals(curr)) {
                        this.moveIndexToFirst(pos);
                        return (V) this.value[pos];
                    }
                }

                return this.defRetValue;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public V getAndMoveToLast(K k) {
        if (k == null) {
            if (this.containsNullKey) {
                this.moveIndexToLast(this.n);
                return (V) this.value[this.n];
            } else {
                return this.defRetValue;
            }
        } else {
            Object[] key = this.key;
            Object curr;
            int pos;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) == null) {
                return this.defRetValue;
            } else if (k.equals(curr)) {
                this.moveIndexToLast(pos);
                return (V) this.value[pos];
            } else {
                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (k.equals(curr)) {
                        this.moveIndexToLast(pos);
                        return (V) this.value[pos];
                    }
                }

                return this.defRetValue;
            }
        }
    }

    public V putAndMoveToFirst(K k, V v) {
        int pos;
        if (k == null) {
            if (this.containsNullKey) {
                this.moveIndexToFirst(this.n);
                return this.setValue(this.n, v);
            }

            this.containsNullKey = true;
            pos = this.n;
        } else {
            Object[] key = this.key;
            Object curr;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) != null) {
                if (curr.equals(k)) {
                    this.moveIndexToFirst(pos);
                    return this.setValue(pos, v);
                }

                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (curr.equals(k)) {
                        this.moveIndexToFirst(pos);
                        return this.setValue(pos, v);
                    }
                }
            }

            key[pos] = k;
        }

        this.value[pos] = v;
        if (this.size == 0) {
            this.first = this.last = pos;
            this.link[pos] = -1L;
        } else {
            this.link[this.first] ^= (this.link[this.first] ^ ((long) pos & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
            this.link[pos] = 0xFFFFFFFF00000000L | (long) this.first & 0xFFFFFFFFL;
            this.first = pos;
        }

        if (this.size++ >= this.maxFill) {
            this.rehash(arraySize(this.size, this.f));
        }

        return this.defRetValue;
    }

    public V putAndMoveToLast(K k, V v) {
        int pos;
        if (k == null) {
            if (this.containsNullKey) {
                this.moveIndexToLast(this.n);
                return this.setValue(this.n, v);
            }

            this.containsNullKey = true;
            pos = this.n;
        } else {
            Object[] key = this.key;
            Object curr;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) != null) {
                if (curr.equals(k)) {
                    this.moveIndexToLast(pos);
                    return this.setValue(pos, v);
                }

                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (curr.equals(k)) {
                        this.moveIndexToLast(pos);
                        return this.setValue(pos, v);
                    }
                }
            }

            key[pos] = k;
        }

        this.value[pos] = v;
        if (this.size == 0) {
            this.first = this.last = pos;
            this.link[pos] = -1L;
        } else {
            this.link[this.last] ^= (this.link[this.last] ^ (long) pos & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            this.link[pos] = ((long) this.last & 0xFFFFFFFFL) << 32 | 0xFFFFFFFFL;
            this.last = pos;
        }

        if (this.size++ >= this.maxFill) {
            this.rehash(arraySize(this.size, this.f));
        }

        return this.defRetValue;
    }

    @SuppressWarnings("unchecked")
    public V get(Object k) {
        if (k == null) {
            return containsNullKey ? (V) value[n] : defRetValue;
        }

        final Object[] key = this.key;
        Object curr;
        int pos;

        // The starting point
        if ((curr = key[pos = mix(k.hashCode()) & mask]) == null) {
            return defRetValue;
        }
        if (k.equals(curr)) {
            return (V) value[pos];
        }

        // There's always an usused entry
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == null) {
                return defRetValue;
            }
            if (k.equals(curr)) {
                return (V) value[pos];
            }
        }
    }

    public boolean containsKey(Object k) {
        if (k == null) {
            return this.containsNullKey;
        } else {
            Object[] key = this.key;
            Object curr;
            int pos;
            if ((curr = key[pos = mix(k.hashCode()) & this.mask]) == null) {
                return false;
            } else if (k.equals(curr)) {
                return true;
            } else {
                while ((curr = key[pos = pos + 1 & this.mask]) != null) {
                    if (k.equals(curr)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public boolean containsValue(Object v) {
        Object[] value = this.value;
        Object[] key = this.key;
        if (containsNullKey && (value[n] == null && v == null) || value[n].equals(v)) {
            return true;
        }
        for (int i = n; i-- != 0;) {
            if (!(key[i] == null) && (value[i] == null && v == null) || value[i].equals(v)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        if (size != 0) {
            size = 0;
            containsNullKey = false;
            Arrays.fill(key, (Object) null);
            Arrays.fill(value, (Object) null);
            first = last = -1;
        }
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    protected void fixPointers(int i) {
        if (size == 0) {
            first = last = -1;
        } else if (first == i) {
            first = (int) link[i];
            if (0 <= first) {
                link[first] |= 0xFFFFFFFF00000000L;
            }
        } else if (last == i) {
            last = (int) (link[i] >>> 32);
            if (0 <= last) {
                link[last] |= 0xFFFFFFFFL;
            }
        } else {
            long linki = link[i];
            int prev = (int) (linki >>> 32);
            int next = (int) linki;
            link[prev] ^= (link[prev] ^ linki & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            link[next] ^= (link[next] ^ linki & 0xFFFFFFFF00000000L) & 0xFFFFFFFF00000000L;
        }
    }

    protected void fixPointers(int s, int d) {
        if (size == 1) {
            first = last = d;
            link[d] = -1L;
        } else if (first == s) {
            first = d;
            link[(int) link[s]] ^= (link[(int) link[s]] ^ ((long) d & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
            link[d] = link[s];
        } else if (last == s) {
            last = d;
            link[(int) (link[s] >>> 32)] ^= (link[(int) (link[s] >>> 32)] ^ (long) d & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            link[d] = link[s];
        } else {
            long links = link[s];
            int prev = (int) (links >>> 32);
            int next = (int) links;
            link[prev] ^= (link[prev] ^ (long) d & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            link[next] ^= (link[next] ^ ((long) d & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
            link[d] = links;
        }
    }

    @SuppressWarnings("unchecked")
    public K firstKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        } else {
            return (K) key[first];
        }
    }

    @SuppressWarnings("unchecked")
    public K lastKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        } else {
            return (K) key[last];
        }
    }

    public Comparator<? super K> comparator() {
        return null;
    }

    public SortedMap<K, V> tailMap(K from) {
        throw new UnsupportedOperationException();
    }

    public SortedMap<K, V> headMap(K to) {
        throw new UnsupportedOperationException();
    }

    public SortedMap<K, V> subMap(K from, K to) {
        throw new UnsupportedOperationException();
    }

    public Iterable<Map.Entry<K, V>> fast() {
        if (fast == null) {
            fast = new Iterable<Entry<K, V>>() {
                public Iterator<Entry<K, V>> iterator() {
                    return new FastEntryIterator();
                }
            };
        }

        return fast;
    }

    public SortedSet<Map.Entry<K, V>> entrySet() {
        if (entries == null) {
            entries = new OpenHashMap.MapEntrySet();
        }

        return this.entries;
    }

    public SortedSet<K> keySet() {
        if (keys == null) {
            keys = new OpenHashMap.KeySet();
        }

        return keys;
    }

    public Collection<V> values() {
        if (values == null) {
            values = new AbstractObjectCollection<V>() {
                public Iterator<V> iterator() {
                    return new ValueIterator();
                }

                public int size() {
                    return size;
                }

                public boolean contains(Object v) {
                    return containsValue(v);
                }

                public void clear() {
                    OpenHashMap.this.clear();
                }
            };
        }

        return values;
    }

    /** Rehashes the map, making the table as small as possible.
     *
     * <P>This method rehashes the table to the smallest size satisfying the
     * load factor. It can be used when the set will not be changed anymore, so
     * to optimize access speed and size.
     *
     * <P>If the table size is already the minimum possible, this method
     * does nothing.
     *
     * @return true if there was enough memory to trim the map.
     * @see #trim(int)
     */
    public boolean trim() {
        int l = arraySize(size, f);
        if (l >= n) {
            return true;
        } else {
            try {
                rehash(l);
                return true;
            } catch (OutOfMemoryError cantDoIt) {
                return false;
            }
        }
    }

    /** Rehashes this map if the table is too large.
     *
     * <P>Let <var>N</var> be the smallest table size that can hold
     * <code>max(n,{@link #size()})</code> entries, still satisfying the load factor. If the current
     * table size is smaller than or equal to <var>N</var>, this method does
     * nothing. Otherwise, it rehashes this map in a table of size
     * <var>N</var>.
     *
     * <P>This method is useful when reusing maps.  {@linkplain #clear() Clearing a
     * map} leaves the table size untouched. If you are reusing a map
     * many times, you can call this method with a typical
     * size to avoid keeping around a very large table just
     * because of a few large transient maps.
     *
     * @param n the threshold for the trimming.
     * @return true if there was enough memory to trim the map.
     * @see #trim()
     */
    public boolean trim(int n) {
        int l = nextPowerOfTwo((int) Math.ceil((double) ((float) n / f)));
        if (n <= l) {
            return true;
        } else {
            try {
                rehash(l);
                return true;
            } catch (OutOfMemoryError cantDoIt) {
                return false;
            }
        }
    }

    /** Rehashes the map.
     *
     * <P>This method implements the basic rehashing strategy, and may be
     * overriden by subclasses implementing different rehashing strategies (e.g.,
     * disk-based rehashing). However, you should not override this method
     * unless you understand the internal workings of this class.
     *
     * @param newN the new size
     */
    protected void rehash(int newN) {
        Object[] key = this.key;
        Object[] value = this.value;

        int mask = newN - 1;
        Object[] newKey = new Object[newN + 1];
        Object[] newValue = new Object[newN + 1];

        int i = first, prev = -1, newPrev = -1, t, pos;
        final long[] link = this.link;
        final long[] newLink = new long[newN + 1];
        first = -1;

        for (int j = size; j-- != 0;) {
            if (key[i] == null) {
                pos = newN;
            } else {
                pos = mix(key[i].hashCode()) & mask;
                while (newKey[pos] != null) {
                    pos = ( pos + 1 ) & mask;
                }
                newKey[pos] = key[i];
            }

            newValue[pos] = value[i];

            if (prev != -1) {
                newLink[newPrev] ^= (newLink[newPrev] ^ (long) pos & 0xFFFFFFFFL) & 0xFFFFFFFFL;
                newLink[pos] ^= (newLink[pos] ^ ((long) newPrev & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
                newPrev = pos;
            } else {
                newPrev = first = pos;
                newLink[pos] = -1L;
            }

            t = i;
            i = (int) link[i];
            prev = t;
        }

        this.link = newLink;
        this.last = newPrev;
        if (newPrev != -1) {
            newLink[newPrev] |= -1 & 0xFFFFFFFFL;
        }

        n = newN;
        this.mask = mask;
        maxFill = maxFill(n, f);
        this.key = newKey;
        this.value = newValue;
    }

    @SuppressWarnings("unchecked")
    public OpenHashMap<K, V> clone() {
        OpenHashMap<K, V> c;
        try {
            c = (OpenHashMap<K, V>) super.clone();
        } catch (CloneNotSupportedException cantHappen) {
            throw new InternalError();
        }

        c.fast = null;
        c.keys = null;
        c.values = null;
        c.entries = null;
        c.containsNullKey = containsNullKey;
        c.key = key.clone();
        c.value = value.clone();
        c.link = link.clone();
        return c;
    }

    public int hashCode() {
        int h = 0;
        for( int j = realSize(), i = 0, t = 0; j-- != 0; ) {
            while (key[i] == null) {
                ++i;
            }

            if (this != key[i]) {
                t = key[i].hashCode();
            }

            if (this != value[i]) {
                t ^= value[i] == null ? 0 : value[i].hashCode();
            }

            h += t;
            i++;
        }

        if (containsNullKey) {
            h += value[n] == null ? 0 : value[n].hashCode();
        }

        return h;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        Object[] key = this.key;
        Object[] value = this.value;
        OpenHashMap.MapIterator i = new OpenHashMap.MapIterator(null);
        s.defaultWriteObject();
        int j = this.size;

        while (j-- != 0) {
            int e = i.nextEntry();
            s.writeObject(key[e]);
            s.writeObject(value[e]);
        }

    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.n = arraySize(this.size, this.f);
        this.maxFill = maxFill(this.n, this.f);
        this.mask = this.n - 1;
        Object[] key = this.key = new Object[this.n + 1];
        Object[] value = this.value = new Object[this.n + 1];
        long[] link = this.link = new long[this.n + 1];
        int prev = -1;
        this.first = this.last = -1;
        int i = this.size;

        while (i-- != 0) {
            Object k = s.readObject();
            Object v = s.readObject();
            int pos;
            if (k == null) {
                pos = this.n;
                this.containsNullKey = true;
            } else {
                for (pos = mix(k.hashCode()) & this.mask; key[pos] != null; pos = pos + 1 & this.mask) {
                    ;
                }

                key[pos] = k;
            }

            value[pos] = v;
            if (this.first != -1) {
                link[prev] ^= (link[prev] ^ (long) pos & 0xFFFFFFFFL) & 0xFFFFFFFFL;
                link[pos] ^= (link[pos] ^ ((long) prev & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
                prev = pos;
            } else {
                prev = this.first = pos;
                link[pos] |= 0xFFFFFFFF00000000L;
            }
        }

        this.last = prev;
        if (prev != -1) {
            link[prev] |= 0xFFFFFFFFL;
        }

    }

    private void checkTable() {
    }

    private final class ValueIterator extends MapIterator implements Iterator<V> {
        @SuppressWarnings("unchecked")
        public V previous() {
            return (V) value[this.previousEntry()];
        }

        public void set(V v) {
            throw new UnsupportedOperationException();
        }

        public void add(V v) {
            throw new UnsupportedOperationException();
        }

        public ValueIterator() {
            super();
        }

        @SuppressWarnings("unchecked")
        public V next() {
            return (V) value[this.nextEntry()];
        }
    }

    private final class KeySet extends AbstractObjectSet<K> implements SortedSet<K> {
        private KeySet() {
        }

        public Iterator<K> iterator(K from) {
            return new KeyIterator(from);
        }

        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains(Object k) {
            return containsKey(k);
        }

        public boolean remove(Object k) {
            int oldSize = size;
            OpenHashMap.this.remove(k);
            return size != oldSize;
        }

        public void clear() {
            OpenHashMap.this.clear();
        }

        @SuppressWarnings("unchecked")
        public K first() {
            if (size == 0) {
                throw new NoSuchElementException();
            } else {
                return (K) key[first];
            }
        }

        @SuppressWarnings("unchecked")
        public K last() {
            if (size == 0) {
                throw new NoSuchElementException();
            } else {
                return (K) key[last];
            }
        }

        public Comparator<? super K> comparator() {
            return null;
        }

        public final SortedSet<K> tailSet(K from) {
            throw new UnsupportedOperationException();
        }

        public final SortedSet<K> headSet(K to) {
            throw new UnsupportedOperationException();
        }

        public final SortedSet<K> subSet(K from, K to) {
            throw new UnsupportedOperationException();
        }
    }

    private final class KeyIterator extends MapIterator implements Iterator<K> {
        public KeyIterator(Object k) {
            super(k);
        }

        @SuppressWarnings("unchecked")
        public K previous() {
            return (K) key[this.previousEntry()];
        }

        public void set(K k) {
            throw new UnsupportedOperationException();
        }

        public void add(K k) {
            throw new UnsupportedOperationException();
        }

        public KeyIterator() {
            super();
        }

        @SuppressWarnings("unchecked")
        public K next() {
            return (K) key[this.nextEntry()];
        }
    }

    private final class MapEntrySet extends AbstractObjectSet<Entry<K, V>> implements SortedSet<Entry<K, V>> {
        private MapEntrySet() {
        }

        public EntryIterator iterator() {
            return new EntryIterator();
        }

        public Comparator<? super Entry<K, V>> comparator() {
            return null;
        }

        public SortedSet<Entry<K, V>> subSet(Entry<K, V> fromElement, Entry<K, V> toElement) {
            throw new UnsupportedOperationException();
        }

        public SortedSet<Entry<K, V>> headSet(Entry<K, V> toElement) {
            throw new UnsupportedOperationException();
        }

        public SortedSet<Entry<K, V>> tailSet(Entry<K, V> fromElement) {
            throw new UnsupportedOperationException();
        }

        public Entry<K, V> first() {
            if (size == 0) {
                throw new NoSuchElementException();
            } else {
                return new MapEntry(first);
            }
        }

        public Entry<K, V> last() {
            if (size == 0) {
                throw new NoSuchElementException();
            } else {
                return new MapEntry(last);
            }
        }

        public boolean contains(Object o) {
            if (!(o instanceof java.util.Map.Entry)) {
                return false;
            } else {
                java.util.Map.Entry e = (java.util.Map.Entry) o;
                Object k = e.getKey();
                if (k == null) {
                    if (containsNullKey) {
                        if (value[n] == null) {
                            if (e.getValue() != null) {
                                return false;
                            }
                        } else if (!value[n].equals(e.getValue())) {
                            return false;
                        }

                        return true;
                    }

                    return false;
                } else {
                    Object[] key = OpenHashMap.this.key;
                    Object curr;
                    int pos;
                    if ((curr = key[pos = mix(k.hashCode()) & mask]) == null) {
                        return false;
                    } else if (k.equals(curr)) {
                        return value[pos] == null ? e.getValue() == null : value[pos].equals(e.getValue());
                    } else {
                        while ((curr = key[pos = pos + 1 & mask]) != null) {
                            if (k.equals(curr)) {
                                return value[pos] == null ? e.getValue() == null : value[pos].equals(e.getValue());
                            }
                        }

                        return false;
                    }
                }
            }
        }

        public boolean remove(Object o) {
            if (!(o instanceof java.util.Map.Entry)) {
                return false;
            } else {
                java.util.Map.Entry e = (java.util.Map.Entry) o;
                Object k = e.getKey();
                Object v = e.getValue();
                if (k == null) {
                    if (containsNullKey) {
                        if (value[n] == null) {
                            if (v != null) {
                                return false;
                            }
                        } else if (!value[n].equals(v)) {
                            return false;
                        }

                        removeNullEntry();
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    Object[] key = OpenHashMap.this.key;
                    Object curr;
                    int pos;
                    if ((curr = key[pos = mix(k.hashCode()) & mask]) == null) {
                        return false;
                    } else if (curr.equals(k)) {
                        if (value[pos] == null) {
                            if (v != null) {
                                return false;
                            }
                        } else if (!value[pos].equals(v)) {
                            return false;
                        }

                        removeEntry(pos);
                        return true;
                    } else {
                        while (true) {
                            do {
                                if ((curr = key[pos = pos + 1 & mask]) == null) {
                                    return false;
                                }
                            } while (!curr.equals(k));

                            if (value[pos] == null) {
                                if (v == null) {
                                    break;
                                }
                            } else if (value[pos].equals(v)) {
                                break;
                            }
                        }

                        removeEntry(pos);
                        return true;
                    }
                }
            }
        }

        public int size() {
            return size;
        }

        public void clear() {
            OpenHashMap.this.clear();
        }

        public EntryIterator iterator(Entry<K, V> from) {
            return new EntryIterator(from.getKey());
        }

        public FastEntryIterator fastIterator() {
            return new FastEntryIterator();
        }

        public FastEntryIterator fastIterator(Entry<K, V> from) {
            return new FastEntryIterator(from.getKey());
        }
    }

    private class FastEntryIterator extends MapIterator implements Iterator<Entry<K, V>> {
        final MapEntry entry;

        public FastEntryIterator() {
            super();
            this.entry = new MapEntry();
        }

        public FastEntryIterator(Object from) {
            super(from);
            this.entry = new MapEntry();
        }

        public OpenHashMap.MapEntry next() {
            this.entry.index = this.nextEntry();
            return this.entry;
        }

        public OpenHashMap.MapEntry previous() {
            this.entry.index = this.previousEntry();
            return this.entry;
        }

        public void set(Entry<K, V> ok) {
            throw new UnsupportedOperationException();
        }

        public void add(Entry<K, V> ok) {
            throw new UnsupportedOperationException();
        }
    }

    private class EntryIterator extends MapIterator implements Iterator<Entry<K, V>> {
        private OpenHashMap.MapEntry entry;

        public EntryIterator() {
            super();
        }

        public EntryIterator(Object from) {
            super(from);
        }

        public OpenHashMap.MapEntry next() {
            return this.entry = new MapEntry(this.nextEntry());
        }

        public OpenHashMap.MapEntry previous() {
            return this.entry = new MapEntry(this.previousEntry());
        }

        public void remove() {
            super.remove();
            this.entry.index = -1;
        }

        public void set(Entry<K, V> ok) {
            throw new UnsupportedOperationException();
        }

        public void add(Entry<K, V> ok) {
            throw new UnsupportedOperationException();
        }
    }

    public static abstract class AbstractObjectSet<K> extends AbstractObjectCollection<K> implements Cloneable {
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof Set)) {
                return false;
            } else {
                Set s = (Set) o;
                return s.size() == this.size() && this.containsAll(s);
            }
        }

        public int hashCode() {
            int h = 0;
            int n = this.size();

            Object k;
            for (Iterator i = this.iterator(); n-- != 0; h += k == null ? 0 : k.hashCode()) {
                k = i.next();
            }

            return h;
        }
    }

    private class MapIterator {
        /**
         * The entry that will be returned by the next call to {@link java.util.ListIterator#previous()} (or <code>null</code> if no previous entry exists).
         */
        int prev = -1;
        /**
         * The entry that will be returned by the next call to {@link java.util.ListIterator#next()} (or <code>null</code> if no next entry exists).
         */
        int next = -1;
        /**
         * The last entry that was returned (or -1 if we did not iterate or used {@link java.util.Iterator#remove()}).
         */
        int curr = -1;
        /**
         * The current index (in the sense of a {@link java.util.ListIterator}). Note that this value is not meaningful when this iterator has been created using the nonempty constructor.
         */
        int index = -1;

        private MapIterator() {
            this.next = first;
            this.index = 0;
        }

        private MapIterator(Object from) {
            if (from == null) {
                if (containsNullKey) {
                    this.next = (int) link[n];
                    this.prev = n;
                } else {
                    throw new NoSuchElementException("The key " + from + " does not belong to this map.");
                }
            } else {
                if (key[last] == null ? from == null : (key[last].equals(from))) {
                    this.prev = last;
                    this.index = size;
                } else {
                    for (int pos = mix(from.hashCode()) & mask; key[pos] != null; pos = pos + 1 & mask) {
                        if (key[pos].equals(from)) {
                            this.next = (int) link[pos];
                            this.prev = pos;
                            return;
                        }
                    }
                    throw new NoSuchElementException("The key " + from + " does not belong to this map.");
                }
            }
        }

        public boolean hasNext() {
            return this.next != -1;
        }

        public boolean hasPrevious() {
            return this.prev != -1;
        }

        private void ensureIndexKnown() {
            if (index < 0) {
                if (prev == -1) {
                    index = 0;
                } else if (next == -1) {
                    index = size;
                } else {
                    int pos = first;
                    for (index = 1; pos != prev; ++index) {
                        pos = (int) link[pos];
                    }
                }
            }
        }

        public int nextIndex() {
            ensureIndexKnown();
            return index;
        }

        public int previousIndex() {
            ensureIndexKnown();
            return index - 1;
        }

        public int nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                curr = next;
                next = (int) link[curr];
                prev = curr;
                if (index >= 0) {
                    ++index;
                }
                return curr;
            }
        }

        public int previousEntry() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            } else {
                curr = prev;
                prev = (int) (link[curr] >>> 32);
                next = curr;
                if (index >= 0) {
                    --index;
                }
                return curr;
            }
        }

        public void remove() {
            this.ensureIndexKnown();
            if (curr == -1) throw new IllegalStateException();

            if (curr == prev) {
                    /* If the last operation was a next(), we are removing an entry that preceeds
				       the current index, and thus we must decrement it. */
                index--;
                prev = (int) (link[curr] >>> 32);
            } else {
                next = (int) link[curr];
            }

            size--;
    			/* Now we manually fix the pointers. Because of our knowledge of next
	    		   and prev, this is going to be faster than calling fixPointers(). */
            if (prev == -1) {
                first = next;
            } else {
                link[prev] ^= (link[prev] ^ (long) next & 0xFFFFFFFFL) & 0xFFFFFFFFL;
            }
            if (next == -1) {
                last = prev;
            } else {
                link[next] ^= (link[next] ^ ((long) prev & 0xFFFFFFFFL) << 32) & 0xFFFFFFFF00000000L;
            }

            int last, slot, pos = curr;
            curr = -1;

            if (pos == n) {
                containsNullKey = false;
                value[n] = null;
            } else {
                Object curr;
                Object[] key = OpenHashMap.this.key;
                // We have to horribly duplicate the shiftKeys() code because we need to update next/prev.
                for (; ; ) {
                    pos = ((last = pos) + 1) & mask;
                    for (; ; ) {
                        if ((curr = key[pos]) == null) {
                            key[last] = null;
                            value[last] = null;
                            return;
                        }
                        slot = mix(curr.hashCode()) & mask;
                        if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                        pos = (pos + 1) & mask;
                    }
                    key[last] = curr;
                    value[last] = value[pos];
                    if (next == pos) next = last;
                    if (prev == pos) prev = last;
                    fixPointers(pos, last);
                }
            }
        }

        public int skip(final int n) {
            int i = n;
            while (i-- != 0 && hasNext()) nextEntry();
            return n - i - 1;
        }

        public int back(final int n) {
            int i = n;
            while (i-- != 0 && hasPrevious()) previousEntry();
            return n - i - 1;
        }
    }

    final class MapEntry implements Entry<K, V> {
        int index;

        MapEntry(int index) {
            this.index = index;
        }

        MapEntry() {
        }

        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) key[this.index];
        }

        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) value[this.index];
        }

        @SuppressWarnings("unchecked")
        public V setValue(V v) {
            Object oldValue = value[this.index];
            value[this.index] = v;
            return (V) oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            } else {
                Entry e = (Entry) o;
                if (key[this.index] == null) {
                    if (e.getKey() != null) {
                        return false;
                    }
                } else if (!key[this.index].equals(e.getKey())) {
                    return false;
                }

                if (value[this.index] == null) {
                    if (e.getValue() != null) {
                        return false;
                    }
                } else if (!value[this.index].equals(e.getValue())) {
                    return false;
                }

                return true;
            }
        }

        public int hashCode() {
            return (key[this.index] == null ? 0 :
                    key[this.index].hashCode()) ^ (value[this.index] == null ? 0 :
                    value[this.index].hashCode());
        }

        public String toString() {
            return key[this.index] + "=>" + value[this.index];
        }
    }


    public static abstract class AbstractObjectCollection<K> extends AbstractCollection<K> {
        protected AbstractObjectCollection() {
        }

        public Object[] toArray() {
            Object[] a = new Object[this.size()];
            unwrap(this.iterator(), a);
            return a;
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            if (a.length < this.size()) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), this.size());
            }
            unwrap(this.iterator(), a);
            return a;
        }

        public boolean addAll(Collection<? extends K> c) {
            boolean retVal = false;
            Iterator<? extends K> i = c.iterator();
            int n = c.size();

            while (n-- != 0) {
                if (this.add(i.next())) {
                    retVal = true;
                }
            }

            return retVal;
        }

        public boolean add(K k) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> c) {
            int n = c.size();
            Iterator i = c.iterator();

            do {
                if (n-- == 0) {
                    return true;
                }
            } while (this.contains(i.next()));

            return false;
        }

        public boolean retainAll(Collection<?> c) {
            boolean retVal = false;
            int n = this.size();
            Iterator i = this.iterator();

            while (n-- != 0) {
                if (!c.contains(i.next())) {
                    i.remove();
                    retVal = true;
                }
            }

            return retVal;
        }

        public boolean removeAll(Collection<?> c) {
            boolean retVal = false;
            int n = c.size();
            Iterator i = c.iterator();

            while (n-- != 0) {
                if (this.remove(i.next())) {
                    retVal = true;
                }
            }

            return retVal;
        }

        public boolean isEmpty() {
            return this.size() == 0;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            Iterator i = this.iterator();
            int n = this.size();
            boolean first = true;
            s.append("{");

            while (n-- != 0) {
                if (first) {
                    first = false;
                } else {
                    s.append(", ");
                }

                Object k = i.next();
                if (this == k) {
                    s.append("(this collection)");
                } else {
                    s.append(String.valueOf(k));
                }
            }

            s.append("}");
            return s.toString();
        }
    }

    private static int arraySize(int expected, float f) {
        long s = Math.max(2L, nextPowerOfTwo((long) Math.ceil((double) ((float) expected / f))));
        if (s > 0x40000000L) {
            throw new IllegalArgumentException("Too large (" + expected + " expected elements with load factor " + f + ")");
        } else {
            return (int) s;
        }
    }

    private static int maxFill(int n, float f) {
        return Math.min((int) Math.ceil((double) ((float) n * f)), n - 1);
    }

    private static int nextPowerOfTwo(int x) {
        if (x == 0) {
            return 1;
        } else {
            --x;
            x |= x >> 1;
            x |= x >> 2;
            x |= x >> 4;
            x |= x >> 8;
            return (x | x >> 16) + 1;
        }
    }

    private static long nextPowerOfTwo(long x) {
        if (x == 0L) {
            return 1L;
        } else {
            --x;
            x |= x >> 1;
            x |= x >> 2;
            x |= x >> 4;
            x |= x >> 8;
            x |= x >> 16;
            return (x | x >> 32) + 1L;
        }
    }

    private static int mix(int x) {
        int h = x * -1640531527;
        return h ^ h >>> 16;
    }

    private static <K> int unwrap(Iterator<? extends K> i, K[] array, int offset, int max) {
        if (max < 0) {
            throw new IllegalArgumentException("The maximum number of elements (" + max + ") is negative");
        } else if (offset >= 0 && offset + max <= array.length) {
            int j;
            for (j = max; j-- != 0 && i.hasNext(); array[offset++] = i.next()) {
                ;
            }

            return max - j - 1;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static <K> int unwrap(Iterator<? extends K> i, K[] array) {
        return unwrap(i, array, 0, array.length);
    }
}
