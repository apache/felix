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
package test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyFilterIndex;
import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyKey;
import org.junit.Test;

public class MultiPropertyKeyTest {

	@Test
	public void sameKeysAndValues() {
		MultiPropertyKey actual = new MultiPropertyKey(2);
		actual.add("key1", "abc");
		actual.add("key2", "efg");

		MultiPropertyKey other = new MultiPropertyKey(2);
		other.add("key1", "abc");
		other.add("key2", "efg");

		assertTrue(actual.equals(other));
		assertTrue(actual.hashCode() == other.hashCode());
	}

	@Test
	public void sameKeysAndValuesDifferentOrder() {
		MultiPropertyKey actual = new MultiPropertyKey(2);
		actual.add("key1", "abc");
		actual.add("key2", "efg");

		MultiPropertyKey other = new MultiPropertyKey(2);
		other.add("key2", "efg");
		other.add("key1", "abc");

		assertTrue(actual.equals(other));
		assertTrue(actual.hashCode() == other.hashCode());
	}

	@Test
	public void sameKeysDifferentValues() {
		MultiPropertyKey actual = new MultiPropertyKey(2);
		actual.add("key1", "abc");
		actual.add("key2", "efg");

		MultiPropertyKey other = new MultiPropertyKey(2);
		other.add("key1", "efg");
		other.add("key2", "abc");

		assertFalse(actual.equals(other));
		assertFalse(actual.hashCode() == other.hashCode());
	}

	@Test
	public void subSetOfKeyValues() {
		MultiPropertyKey actual = new MultiPropertyKey(3);
		actual.add("key1", "abc");
		actual.add("key2", "efg");
		actual.add("key3", "hij");

		MultiPropertyKey other = new MultiPropertyKey(2);
		other.add("key1", "abc");
		other.add("key2", "efg");

		assertFalse(actual.equals(other));
		assertFalse(actual.hashCode() == other.hashCode());
	}

	@Test
	public void noPermutations() {
		MultiPropertyFilterIndex singleValueFilterIndex = new MultiPropertyFilterIndex("objectClass,#nopermutation");

		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		ref1.addProperty("nopermutation", new String[] { "a", "b", "c", "d" });

		List<MultiPropertyKey> keys = singleValueFilterIndex.createKeys(ref1);

		assertTrue(keys.size() == 4);
	}

	@Test
	public void permutations() {
		MultiPropertyFilterIndex singleValueFilterIndex = new MultiPropertyFilterIndex("objectClass,permutation");

		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		ref1.addProperty("permutation", new String[] { "a", "b", "c", "d" });

		List<MultiPropertyKey> keys = singleValueFilterIndex.createKeys(ref1);

		assertTrue(keys.size() == 10);
	}
}
