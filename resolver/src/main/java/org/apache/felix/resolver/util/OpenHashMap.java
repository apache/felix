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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An open addressing map.
 * Low memory consumption compared to a HashMap.
 *
 * Based on the mahout collection classes.
 */
public class OpenHashMap<K, V> extends AbstractMap<K, V> implements Cloneable {

    static final int[] primeCapacities = {
            3, 5, 7, 11, 17, 23, 31, 37, 43, 47, 67, 79, 89, 97, 137, 163, 179, 197, 277, 311,
            331, 359, 379, 397, 433, 557, 599, 631, 673, 719, 761, 797, 877, 953, 1039, 1117,
            1201, 1277, 1361, 1439, 1523, 1597, 1759, 1907, 2081, 2237, 2411, 2557, 2729, 2879,
            3049, 3203, 3527, 3821, 4177, 4481, 4831, 5119, 5471, 5779, 6101, 6421, 7057, 7643,
            8363, 8963, 9677, 10243, 10949, 11579, 12203, 12853, 14143, 15287, 16729, 17929,
            19373, 20507, 21911, 23159, 24407, 25717, 28289, 30577, 33461, 35863, 38747, 41017,
            43853, 46327, 48817, 51437, 56591, 61169, 66923, 71741, 77509, 82037, 87719, 92657,
            97649, 102877, 113189, 122347, 133853, 143483, 155027, 164089, 175447, 185323,
            195311, 205759, 226379, 244703, 267713, 286973, 310081, 328213, 350899, 370661,
            390647, 411527, 452759, 489407, 535481, 573953, 620171, 656429, 701819, 741337,
            781301, 823117, 905551, 978821, 1070981, 1147921, 1240361, 1312867, 1403641, 1482707,
            1562611, 1646237, 1811107, 1957651, 2141977, 2295859, 2480729, 2625761, 2807303,
            2965421, 3125257, 3292489, 3622219, 3915341, 4283963, 4591721, 4961459, 5251529,
            5614657, 5930887, 6250537, 6584983, 7244441, 7830701, 8567929, 9183457, 9922933,
            10503061, 11229331, 11861791, 12501169, 13169977, 14488931, 15661423, 17135863,
            18366923, 19845871, 21006137, 22458671, 23723597, 25002389, 26339969, 28977863,
            31322867, 34271747, 36733847, 39691759, 42012281, 44917381, 47447201, 50004791,
            52679969, 57955739, 62645741, 68543509, 73467739, 79383533, 84024581, 89834777,
            94894427, 100009607, 105359939, 115911563, 125291483, 137087021, 146935499,
            158767069, 168049163, 179669557, 189788857, 200019221, 210719881, 231823147,
            250582987, 274174111, 293871013, 317534141, 336098327, 359339171, 379577741,
            400038451, 421439783, 463646329, 501165979, 548348231, 587742049, 635068283,
            672196673, 718678369, 759155483, 800076929, 842879579, 927292699, 1002331963,
            1096696463, 1175484103, 1270136683, 1344393353, 1437356741, 1518310967,
            1600153859, 1685759167, 1854585413, 2004663929, 2147483647
    };
    static final int largestPrime = primeCapacities[primeCapacities.length - 1];

    protected static final int defaultCapacity = 277;
    protected static final double defaultMinLoadFactor = 0.2;
    protected static final double defaultMaxLoadFactor = 0.5;

    protected static final Object FREE = null;
    protected static final Object REMOVED = new Object();

    /** The number of distinct associations in the map; its "size()". */
    protected int distinct;

    /**
     * The table capacity c=table.length always satisfies the invariant <tt>c * minLoadFactor <= s <= c *
     * maxLoadFactor</tt>, where s=size() is the number of associations currently contained. The term "c * minLoadFactor"
     * is called the "lowWaterMark", "c * maxLoadFactor" is called the "highWaterMark". In other words, the table capacity
     * (and proportionally the memory used by this class) oscillates within these constraints. The terms are precomputed
     * and cached to avoid recalculating them each time put(..) or removeKey(...) is called.
     */
    protected int lowWaterMark;
    protected int highWaterMark;

    /** The minimum load factor for the hashtable. */
    protected double minLoadFactor;

    /** The maximum load factor for the hashtable. */
    protected double maxLoadFactor;

    /** The hash table keys. */
    protected Object[] table;

    /** The hash table values. */
    protected Object[] values;

    /** The number of table entries in state==FREE. */
    protected int freeEntries;


    /** Constructs an empty map with default capacity and default load factors. */
    public OpenHashMap() {
        this(defaultCapacity);
    }

    /**
     * Constructs an empty map with the specified initial capacity and default load factors.
     *
     * @param initialCapacity the initial capacity of the map.
     * @throws IllegalArgumentException if the initial capacity is less than zero.
     */
    public OpenHashMap(int initialCapacity) {
        this(initialCapacity, defaultMinLoadFactor, defaultMaxLoadFactor);
    }

    /**
     * Constructs an empty map with the specified initial capacity and the specified minimum and maximum load factor.
     *
     * @param initialCapacity the initial capacity.
     * @param minLoadFactor   the minimum load factor.
     * @param maxLoadFactor   the maximum load factor.
     * @throws IllegalArgumentException if <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) ||
     *                                  (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >=
     *                                  maxLoadFactor)</tt>.
     */
    public OpenHashMap(int initialCapacity, double minLoadFactor, double maxLoadFactor) {
        setUp(initialCapacity, minLoadFactor, maxLoadFactor);
    }

    /** Removes all (key,value) associations from the receiver. Implicitly calls <tt>trimToSize()</tt>. */
    @Override
    public void clear() {
        Arrays.fill(this.table, FREE);
        Arrays.fill(this.values, null);
        distinct = 0;
        freeEntries = table.length; // delta
        trimToSize();
    }

    /**
     * Returns a deep copy of the receiver.
     *
     * @return a deep copy of the receiver.
     */
    @Override
    @SuppressWarnings("unchecked")
    public OpenHashMap<K, V> clone() {
        try {
            OpenHashMap<K,V> copy = (OpenHashMap<K,V>) super.clone();
            copy.table = copy.table.clone();
            copy.values = copy.values.clone();
            return copy;
        } catch (CloneNotSupportedException exc) {
            InternalError e = new InternalError();
            e.initCause(exc);
            throw e; //should never happen since we are cloneable
        }
    }

    /**
     * Returns <tt>true</tt> if the receiver contains the specified key.
     *
     * @return <tt>true</tt> if the receiver contains the specified key.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        return indexOfKey((K) key) >= 0;
    }

    /**
     * Returns <tt>true</tt> if the receiver contains the specified value.
     *
     * @return <tt>true</tt> if the receiver contains the specified value.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean containsValue(Object value) {
        return indexOfValue((V)value) >= 0;
    }

    /**
     * Ensures that the receiver can hold at least the specified number of associations without needing to allocate new
     * internal memory. If necessary, allocates new internal memory and increases the capacity of the receiver. <p> This
     * method never need be called; it is for performance tuning only. Calling this method before <tt>put()</tt>ing a
     * large number of associations boosts performance, because the receiver will grow only once instead of potentially
     * many times and hash collisions get less probable.
     *
     * @param minCapacity the desired minimum capacity.
     */
    public void ensureCapacity(int minCapacity) {
        if (table.length < minCapacity) {
            int newCapacity = nextPrime(minCapacity);
            rehash(newCapacity);
        }
    }

    /**
     * Returns the value associated with the specified key. It is often a good idea to first check with {@link
     * #containsKey(Object)} whether the given key has a value associated or not, i.e. whether there exists an association
     * for the given key or not.
     *
     * @param key the key to be searched for.
     * @return the value associated with the specified key; <tt>0</tt> if no such key is present.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        int i = indexOfKey((K)key);
        if (i < 0) {
            return null;
        } //not contained
        return (V)values[i];
    }

    /**
     * @param key the key to be added to the receiver.
     * @return the index where the key would need to be inserted, if it is not already contained. Returns -index-1 if the
     *         key is already contained at slot index. Therefore, if the returned index < 0, then it is already contained
     *         at slot -index-1. If the returned index >= 0, then it is NOT already contained and should be inserted at
     *         slot index.
     */
    protected int indexOfInsertion(K key) {
        Object[] tab = table;
        int length = tab.length;

        int hash = key.hashCode() & 0x7FFFFFFF;
        int i = hash % length;
        int decrement = hash % (length - 2); // double hashing, see http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        //int decrement = (hash / length) % length;
        if (decrement == 0) {
            decrement = 1;
        }

        // stop if we find a removed or free slot, or if we find the key itself
        // do NOT skip over removed slots (yes, open addressing is like that...)
        while (table[i] != FREE && table[i] != REMOVED && !equalsMindTheNull(key, tab[i])) {
            i -= decrement;
            //hashCollisions++;
            if (i < 0) {
                i += length;
            }
        }

        if (table[i] == REMOVED) {
            // stop if we find a free slot, or if we find the key itself.
            // do skip over removed slots (yes, open addressing is like that...)
            // assertion: there is at least one FREE slot.
            int j = i;
            while (table[i] != FREE && (table[i] == REMOVED || tab[i] != key)) {
                i -= decrement;
                //hashCollisions++;
                if (i < 0) {
                    i += length;
                }
            }
            if (table[i] == FREE) {
                i = j;
            }
        }


        if (table[i] != FREE && table[i] != REMOVED) {
            // key already contained at slot i.
            // return a negative number identifying the slot.
            return -i - 1;
        }
        // not already contained, should be inserted at slot i.
        // return a number >= 0 identifying the slot.
        return i;
    }

    /**
     * @param key the key to be searched in the receiver.
     * @return the index where the key is contained in the receiver, returns -1 if the key was not found.
     */
    protected int indexOfKey(K key) {
        Object[] tab = table;
        int length = tab.length;

        int hash = key.hashCode() & 0x7FFFFFFF;
        int i = hash % length;
        int decrement = hash % (length - 2); // double hashing, see http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        //int decrement = (hash / length) % length;
        if (decrement == 0) {
            decrement = 1;
        }

        // stop if we find a free slot, or if we find the key itself.
        // do skip over removed slots (yes, open addressing is like that...)
        while (tab[i] != FREE && (tab[i] == REMOVED || !equalsMindTheNull(key, tab[i]))) {
            i -= decrement;
            //hashCollisions++;
            if (i < 0) {
                i += length;
            }
        }

        if (tab[i] == FREE) {
            return -1;
        } // not found
        return i; //found, return index where key is contained
    }

    /**
     * @param value the value to be searched in the receiver.
     * @return the index where the value is contained in the receiver, returns -1 if the value was not found.
     */
    protected int indexOfValue(V value) {
        Object[] val = values;

        for (int i = values.length; --i >= 0;) {
            if (table[i] != FREE && table[i] != REMOVED && equalsMindTheNull(val[i], value)) {
                return i;
            }
        }

        return -1; // not found
    }

    /**
     * Associates the given key with the given value. Replaces any old <tt>(key,someOtherValue)</tt> association, if
     * existing.
     *
     * @param key   the key the value shall be associated with.
     * @param value the value to be associated.
     * @return <tt>true</tt> if the receiver did not already contain such a key; <tt>false</tt> if the receiver did
     *         already contain such a key - the new value has now replaced the formerly associated value.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value) {
        int i = indexOfInsertion(key);
        if (i < 0) { //already contained
            i = -i - 1;
            V previous = (V) this.values[i];
            this.values[i] = value;
            return previous;
        }

        if (this.distinct > this.highWaterMark) {
            int newCapacity = chooseGrowCapacity(this.distinct + 1, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
            return put(key, value);
        }

        if (this.table[i] == FREE) {
            this.freeEntries--;
        }
        this.table[i] = key;
        this.values[i] = value;
        this.distinct++;

        if (this.freeEntries < 1) { //delta
            int newCapacity = chooseGrowCapacity(this.distinct + 1, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return null;
    }

    /**
     * Rehashes the contents of the receiver into a new table with a smaller or larger capacity. This method is called
     * automatically when the number of keys in the receiver exceeds the high water mark or falls below the low water
     * mark.
     */
    @SuppressWarnings("unchecked")
    protected void rehash(int newCapacity) {
        int oldCapacity = table.length;
        //if (oldCapacity == newCapacity) return;

        Object[] oldTable = table;
        Object[] oldValues = values;

        Object[] newTable = new Object[newCapacity];
        Object[] newValues = new Object[newCapacity];

        this.lowWaterMark = chooseLowWaterMark(newCapacity, this.minLoadFactor);
        this.highWaterMark = chooseHighWaterMark(newCapacity, this.maxLoadFactor);

        this.table = newTable;
        this.values = newValues;
        this.freeEntries = newCapacity - this.distinct; // delta

        for (int i = oldCapacity; i-- > 0;) {
            if (oldTable[i] != FREE && oldTable[i] != REMOVED) {
                Object element = oldTable[i];
                int index = indexOfInsertion((K)element);
                newTable[index] = element;
                newValues[index] = oldValues[i];
            }
        }
    }

    /**
     * Removes the given key with its associated element from the receiver, if present.
     *
     * @param key the key to be removed from the receiver.
     * @return <tt>true</tt> if the receiver contained the specified key, <tt>false</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        int i = indexOfKey((K)key);
        if (i < 0) {
            return null;
        }
        // key not contained
        V removed = (V) values[i];

        this.table[i] = REMOVED;
        this.values[i] = null;
        this.distinct--;

        if (this.distinct < this.lowWaterMark) {
            int newCapacity = chooseShrinkCapacity(this.distinct, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return removed;
    }

    /**
     * Initializes the receiver.
     *
     * @param initialCapacity the initial capacity of the receiver.
     * @param minLoadFactor   the minLoadFactor of the receiver.
     * @param maxLoadFactor   the maxLoadFactor of the receiver.
     * @throws IllegalArgumentException if <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) ||
     *                                  (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >=
     *                                  maxLoadFactor)</tt>.
     */
    protected void setUp(int initialCapacity, double minLoadFactor, double maxLoadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial Capacity must not be less than zero: " + initialCapacity);
        }
        if (minLoadFactor < 0.0 || minLoadFactor >= 1.0) {
            throw new IllegalArgumentException("Illegal minLoadFactor: " + minLoadFactor);
        }
        if (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) {
            throw new IllegalArgumentException("Illegal maxLoadFactor: " + maxLoadFactor);
        }
        if (minLoadFactor >= maxLoadFactor) {
            throw new IllegalArgumentException(
                    "Illegal minLoadFactor: " + minLoadFactor + " and maxLoadFactor: " + maxLoadFactor);
        }
        int capacity = initialCapacity;
        capacity = nextPrime(capacity);
        if (capacity == 0) {
            capacity = 1;
        } // open addressing needs at least one FREE slot at any time.

        this.table = new Object[capacity];
        this.values = new Object[capacity];

        // memory will be exhausted long before this pathological case happens, anyway.
        this.minLoadFactor = minLoadFactor;
        if (capacity == largestPrime) {
            this.maxLoadFactor = 1.0;
        } else {
            this.maxLoadFactor = maxLoadFactor;
        }

        this.distinct = 0;
        this.freeEntries = capacity; // delta

        // lowWaterMark will be established upon first expansion.
        // establishing it now (upon instance construction) would immediately make the table shrink upon first put(...).
        // After all the idea of an "initialCapacity" implies violating lowWaterMarks when an object is young.
        // See ensureCapacity(...)
        this.lowWaterMark = 0;
        this.highWaterMark = chooseHighWaterMark(capacity, this.maxLoadFactor);
    }

    /**
     * Trims the capacity of the receiver to be the receiver's current size. Releases any superfluous internal memory. An
     * application can use this operation to minimize the storage of the receiver.
     */
    public void trimToSize() {
        // * 1.2 because open addressing's performance exponentially degrades beyond that point
        // so that even rehashing the table can take very long
        int newCapacity = nextPrime((int) (1 + 1.2 * size()));
        if (table.length > newCapacity) {
            rehash(newCapacity);
        }
    }

    public void concat() {
        int newCap = nextPrime(size() + 1); // +1 as we always need a free slot
        if (newCap != table.length) {
            rehash(newCap);
        }
    }

    /**
     * Allocate a set to contain Map.Entry objects for the pairs and return it.
     */
    @Override
    public Set<Entry<K,V>> entrySet() {
        return new EntrySet();
    }

    /**
     * Chooses a new prime table capacity optimized for growing that (approximately) satisfies the invariant <tt>c *
     * minLoadFactor <= size <= c * maxLoadFactor</tt> and has at least one FREE slot for the given size.
     */
    protected int chooseGrowCapacity(int size, double minLoad, double maxLoad) {
        return nextPrime(Math.max(size + 1, (int) ((4 * size / (3 * minLoad + maxLoad)))));
    }

    /**
     * Returns new high water mark threshold based on current capacity and maxLoadFactor.
     *
     * @return int the new threshold.
     */
    protected int chooseHighWaterMark(int capacity, double maxLoad) {
        return Math.min(capacity - 2, (int) (capacity * maxLoad)); //makes sure there is always at least one FREE slot
    }

    /**
     * Returns new low water mark threshold based on current capacity and minLoadFactor.
     *
     * @return int the new threshold.
     */
    protected int chooseLowWaterMark(int capacity, double minLoad) {
        return (int) (capacity * minLoad);
    }

    /**
     * Chooses a new prime table capacity optimized for shrinking that (approximately) satisfies the invariant <tt>c *
     * minLoadFactor <= size <= c * maxLoadFactor</tt> and has at least one FREE slot for the given size.
     */
    protected int chooseShrinkCapacity(int size, double minLoad, double maxLoad) {
        return nextPrime(Math.max(size + 1, (int) ((4 * size / (minLoad + 3 * maxLoad)))));
    }

    /**
     * Returns a prime number which is <code>&gt;= desiredCapacity</code> and very close to <code>desiredCapacity</code>
     * (within 11% if <code>desiredCapacity &gt;= 1000</code>).
     *
     * @param desiredCapacity the capacity desired by the user.
     * @return the capacity which should be used for a hashtable.
     */
    protected int nextPrime(int desiredCapacity) {
        int i = java.util.Arrays.binarySearch(primeCapacities, desiredCapacity);
        if (i < 0) {
            // desired capacity not found, choose next prime greater than desired capacity
            i = -i - 1; // remember the semantics of binarySearch...
        }
        return primeCapacities[i];
    }

    /**
     * Returns the number of (key,value) associations currently contained.
     *
     * @return the number of (key,value) associations currently contained.
     */
    public int size() {
        return distinct;
    }

    protected static boolean equalsMindTheNull(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public int size() {
            return OpenHashMap.this.size();
        }
    }

    private class EntrySetIterator implements Iterator<Entry<K, V>> {
        int idx = -1;
        Entry<K,V> next;

        EntrySetIterator() {
            forward();
        }

        @SuppressWarnings("unchecked")
        private void forward() {
            next = null;
            while (next == null && ++idx < table.length) {
                if (table[idx] != FREE && table[idx] != REMOVED) {
                    next = new SimpleImmutableEntry<K, V>((K) table[idx], (V) values[idx]);
                }
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public Entry<K, V> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Entry<K,V> n = next;
            forward();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
