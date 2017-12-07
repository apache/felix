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
package org.osgi.util.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConverterCollectionsTest {
    @Test
    public void testLiveBackingList() {
        List<Integer> l = Arrays.asList(9, 8, 7);
        Converter converter = Converters.standardConverter();
        List<Short> sl = converter.convert(l).view()
                .to(new TypeReference<List<Short>>() {});

        assertEquals(Short.valueOf((short) 9), sl.get(0));
        assertEquals(Short.valueOf((short) 8), sl.get(1));
        assertEquals(Short.valueOf((short) 7), sl.get(2));
        assertEquals(3, sl.size());

        l.set(1, 11);
        assertEquals(Short.valueOf((short) 9), sl.get(0));
        assertEquals(Short.valueOf((short) 11), sl.get(1));
        assertEquals(Short.valueOf((short) 7), sl.get(2));
        assertEquals(3, sl.size());

        List<Short> sl2 = converter.convert(l).view()
                .to(new TypeReference<List<Short>>() {});
        List<Short> sl3 = converter.convert(l).view()
                .to(new TypeReference<List<Short>>() {});
        sl3.add(Short.valueOf((short) 6));

        assertEquals(sl.hashCode(), sl2.hashCode());
        assertTrue(sl.hashCode() != sl3.hashCode());

        assertEquals(sl, sl2);
        assertFalse(sl.equals(sl3));
    }

    @Test
    public void testLiveBackingList1() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        a[0] = 7l;
        l.addAll(Arrays.asList(7, 6));
        a[0] = 1l;
        assertEquals(Arrays.asList(7, 8, 7, 6), l);
    }

    @Test
    public void testLiveBackingList2() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        l.addAll(1, Arrays.asList(7, 6));
        a[0] = 1l;
        assertEquals(Arrays.asList(9, 7, 6, 8), l);
    }

    @Test
    public void testLiveBackingList3() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        l.removeAll(Collections.singleton(8));
        a[0] = 1l;
        assertEquals(Collections.singletonList(9), l);
    }

    @Test
    public void testLiveBackingList4() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        l.retainAll(Collections.singleton(8));
        a[1] = 1l;
        assertEquals(Collections.singletonList(8), l);
    }

    @Test
    public void testLiveBackingList5() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        l.clear();
        l.add(10);
        a[0] = 1l;
        assertEquals(Collections.singletonList(10), l);
    }

    @Test
    public void testLiveBackingList6() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        l.add(10);
        a[0] = 1l;
        assertEquals(Arrays.asList(9, 8, 10), l);
    }

    @Test
    public void testLiveBackingList7() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        l.add(0, 10);
        a[0] = 1l;
        assertEquals(Arrays.asList(10, 9, 8), l);
    }

    @Test
    public void testLiveBackingList8() {
        long[] a = new long[] {
                9l, 8l
        };

        List<Integer> l = Converters.standardConverter().convert(a).view().to(
                new TypeReference<List<Integer>>() {});
        assertEquals(Integer.valueOf(8), l.remove(1));
        a[0] = 1l;
        assertEquals(Arrays.asList(9), l);
    }

    @Test
    public void testLiveBackingCollection() {
        Set<String> s = new LinkedHashSet<>(Arrays.asList("yo", "yo", "ma"));
        Converter converter = Converters.standardConverter();
        List<String> sl = converter.convert(s).view()
                .to(new TypeReference<List<String>>() {});

        assertEquals("yo", sl.get(0));
        assertEquals("ma", sl.get(1));
        assertEquals(2, sl.size());

        s.add("ha");
        s.add("yo");
        assertEquals("yo", sl.get(0));
        assertEquals("ma", sl.get(1));
        assertEquals("ha", sl.get(2));
        assertEquals(3, sl.size());
        assertFalse(sl.isEmpty());

        assertTrue(sl.contains("ma"));
        assertFalse(sl.contains("na"));

        String[] sa = sl.toArray(new String[] {});
        assertEquals(3, sa.length);
        assertEquals("yo", sa[0]);
        assertEquals("ma", sa[1]);
        assertEquals("ha", sa[2]);

        assertTrue(sl.containsAll(Arrays.asList("ma", "yo")));
        assertFalse(sl.containsAll(Arrays.asList("xxx")));
    }

    @Test
    public void testLiveBackingEmptyCollection() {
        Set<Long> s = Collections.emptySet();
        Collection< ? > l = Converters.standardConverter().convert(s).view().to(
                Collection.class);
        assertTrue(l.isEmpty());
        assertEquals(0, l.size());
    }

    @Test
    public void testLiveBackingArray() {
        Converter converter = Converters.standardConverter();
        int[] arr = new int[] {
                1, 2
        };

        @SuppressWarnings("rawtypes")
        List l = converter.convert(arr).view().to(List.class);
        assertEquals(2, l.size());
        assertFalse(l.isEmpty());
        assertEquals(1, l.get(0));
        assertEquals(2, l.get(1));

        assertTrue(l.contains(1));
        assertTrue(l.contains(2));
        assertFalse(l.contains(3));
        assertFalse(l.contains(0));

        arr[0] = -3;
        arr[1] = 3;
        assertEquals(-3, l.get(0));
        assertEquals(3, l.get(1));
    }

    @Test
    public void testLiveBackingMixedArrayWithNulls() {
        Object[] oa = new Object[] {
                "hi", null, 'x'
        };
        List< ? > l = Converters.standardConverter().convert(oa).view().to(List.class);
        assertTrue(l.contains("hi"));
        assertTrue(l.contains(null));
        assertTrue(l.contains('x'));
        assertFalse(l.containsAll(Arrays.asList('x', 7)));
        assertTrue(l.containsAll(Arrays.asList('x', null, null, "hi", "hi")));
        assertEquals(0, l.indexOf("hi"));
        assertEquals(1, l.indexOf(null));
        assertEquals(2, l.indexOf('x'));
        assertEquals(-1, l.indexOf("test"));

        List< ? > l0 = l.subList(1, 1);
        assertEquals(0, l0.size());
        List< ? > l1 = l.subList(1, 2);
        assertEquals(Arrays.asList((Object) null), l1);
        List< ? > l2 = l.subList(1, 3);
        assertEquals(Arrays.asList(null, 'x'), l2);
        List< ? > l3 = l.subList(0, 2);
        assertEquals(Arrays.asList("hi", null), l3);
    }

    @Test
    public void testLiveStringArray() {
        String[] sa = new String[] {
                "yo", "ho", "yo", null, "yo"
        };

        List<String> l = Converters.standardConverter().convert(sa).view().to(
                new TypeReference<List<String>>() {});
        Object[] oa1 = l.toArray();
        String[] sa1 = l.toArray(new String[] {});
        assertEquals("yo", sa1[0]);
        assertEquals("ho", sa1[1]);
        assertEquals("yo", sa1[2]);
        assertNull(sa1[3]);
        assertEquals("yo", sa1[4]);
        assertEquals(oa1[0], sa1[0]);
        assertEquals(oa1[1], sa1[1]);
        assertEquals(oa1[2], sa1[2]);
        assertEquals(oa1[3], sa1[3]);
        assertEquals(oa1[4], sa1[4]);
        assertEquals(5, oa1.length);
        assertEquals(5, sa1.length);

        String[] sa2 = l.toArray(new String[6]);
        assertEquals(oa1[0], sa2[0]);
        assertEquals(oa1[1], sa2[1]);
        assertEquals(oa1[2], sa2[2]);
        assertEquals(oa1[3], sa2[3]);
        assertEquals(oa1[4], sa2[4]);
        assertNull(sa2[5]);
        assertEquals(6, sa2.length);

        assertEquals(4, l.lastIndexOf("yo"));
        assertEquals(1, l.lastIndexOf("ho"));
        assertEquals(3, l.lastIndexOf(null));
        assertEquals(-1, l.lastIndexOf(123));
    }

    @Test
    public void testLiveBackingArray0() {
        Converter converter = Converters.standardConverter();
        List< ? > l = converter.convert(new double[] {}).view().to(List.class);
        assertTrue(l.isEmpty());
        assertEquals(0, l.size());
    }

    @Test
    public void testLiveBackingArray1() {
        Converter converter = Converters.standardConverter();
        Integer[] arr = new Integer[] {1, 2};

        @SuppressWarnings("rawtypes")
        List l = converter.convert(arr).view().to(List.class);
        assertEquals(1, l.get(0));
        assertEquals(2, l.get(1));

        arr[0] = -3;
        arr[1] = 3;
        assertEquals(-3, l.get(0));
        assertEquals(3, l.get(1));
    }

    @Test
    public void testLiveBackingArray2() {
        Converter converter = Converters.standardConverter();
        Integer[] arr = new Integer[] {1, 2};

        List<Long> l = converter.convert(arr).view().to(new TypeReference<List<Long>>() {});
        assertTrue(l.contains(Long.valueOf(2)));
        assertTrue(
                l.containsAll(Arrays.asList(Long.valueOf(2), Long.valueOf(1))));
        assertFalse(l.contains(Long.valueOf(3)));
        assertFalse(
                l.containsAll(Arrays.asList(Long.valueOf(2), Long.valueOf(3))));

        arr[0] = Integer.valueOf(3);
        assertTrue(l.contains(Long.valueOf(2)));
        assertFalse(
                l.containsAll(Arrays.asList(Long.valueOf(2), Long.valueOf(1))));
        assertTrue(l.contains(Long.valueOf(3)));
        assertTrue(
                l.containsAll(Arrays.asList(Long.valueOf(2), Long.valueOf(3))));

        l.add(Long.valueOf(4));
        l.add(Long.valueOf(5));
        arr[0] = Integer.valueOf(1);
        assertTrue(l.containsAll(Arrays.asList(Long.valueOf(2), Long.valueOf(3),
                Long.valueOf(4), Long.valueOf(5))));
    }

    @Test
    public void testLiveBackingArray3() {
        Converter converter = Converters.standardConverter();
        Integer[] arr = new Integer[] {
                1, 2
        };

        List<Long> l = converter.convert(arr).view()
                .to(new TypeReference<List<Long>>() {});
        assertTrue(l.remove(Long.valueOf(1)));
        arr[1] = Integer.valueOf(3);
        assertEquals(Collections.singletonList(Long.valueOf(2)), l);
    }

    @Test
    public void testLiveArrayBackingSet() {
        char[] ca = new char[] {
                'a', 'b', 'c'
        };

        Set<Character> s = Converters.standardConverter().convert(ca).view().to(
                new TypeReference<Set<Character>>() {});
        assertTrue(s.containsAll(Arrays.asList(Character.valueOf('a'),
                Character.valueOf('b'), Character.valueOf('c'))));

        ca[0] = 'd';
        assertTrue(s.containsAll(Arrays.asList(Character.valueOf('b'),
                Character.valueOf('c'), Character.valueOf('d'))));
    }

    @Test
    public void testLiveBackingSet() {
        List<Double> l = new ArrayList<>();
        l.add(3.1415);

        Set<Float> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<Float>>() {});
        Float f1 = Float.valueOf(3.1415f);
        Float f2 = Float.valueOf(1.0f);
        assertEquals(1, s.size());
        assertFalse(s.isEmpty());
        assertTrue(s.contains(f1));
        assertFalse(s.contains(f2));
        assertEquals(f1, s.iterator().next());

        l.set(0, null);
        assertEquals(1, s.size());
        assertFalse(s.isEmpty());
        assertTrue(s.contains(null));
        assertFalse(s.contains(f2));
        assertFalse(s.contains(f1));
        assertNull(s.iterator().next());

        Float f3 = Float.valueOf(2.7182f);
        s.add(f3);
        assertEquals("Original should not be modified", 1, l.size());
        l.set(0, -1.0);
        assertEquals(2, s.size());
        assertTrue(s.contains(null));
        assertTrue(s.contains(f3));
        assertFalse(s.contains(f2));
        assertFalse(s.contains(f1));
    }

    @Test
    public void testLiveBackingSet0() {
        List<String> l = new ArrayList<>();
        l.addAll(Arrays.asList("hi", "there"));

        Set<String> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<String>>() {});
        l.set(0, "ho");

        String[] sa = s.toArray(new String[1]);
        assertEquals(Arrays.asList("ho", "there"), Arrays.asList(sa));

        String[] sa2 = s.toArray(new String[4]);
        assertEquals(Arrays.asList("ho", "there", null, null),
                Arrays.asList(sa2));

        Set<String> s2 = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<String>>() {});
        Set<String> s3 = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<String>>() {});
        s3.add("!!");
        assertEquals(s.hashCode(), s2.hashCode());
        assertFalse(s.hashCode() == s3.hashCode());

        assertTrue(s.equals(s2));
        assertFalse(s.equals(s3));
    }

    @Test
    public void testLiveBackingSet1() {
        List<String> l = new ArrayList<>();
        l.addAll(Arrays.asList("hi", "there"));

        Set<CharSequence> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<CharSequence>>() {});
        assertTrue(s.containsAll(Arrays.asList("there", "hi")));
        s.clear();
        assertEquals("Original should not be modified", Arrays.asList("hi", "there"), l);
        assertEquals(0, s.size());
        assertTrue(s.isEmpty());
    }

    @Test
    public void testLiveBackingSet2() {
        List<String> l = new ArrayList<>();
        l.addAll(Arrays.asList("hi", "there"));

        Set<CharSequence> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<CharSequence>>() {});
        s.remove("yo");
        l.set(0, "xxx"); // Should not have an effect since 'remove' was called
        assertTrue(s.containsAll(Arrays.asList("there", "hi")));

        s.remove("hi");
        assertEquals(Collections.singleton("there"), s);
    }

    @Test
    public void testLiveBackingSet3() {
        List<String> l = new ArrayList<>();
        l.addAll(Arrays.asList("hi", "there"));

        Set<CharSequence> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<CharSequence>>() {});
        assertFalse(s.addAll(Collections.singleton("there")));
        assertTrue(s.addAll(Arrays.asList("there", "!!")));
        l.remove("hi");
        assertTrue(s.containsAll(Arrays.asList("there", "hi", "!!")));
    }

    @Test
    public void testLiveBackingSet4() {
        List<String> l = new ArrayList<>();
        l.addAll(Arrays.asList("hi", "there"));

        Set<CharSequence> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<CharSequence>>() {});
        assertFalse(s.removeAll(Collections.singleton("yo")));
        l.remove("hi");
        assertTrue(s.containsAll(Arrays.asList("there", "hi")));
        assertTrue(s.removeAll(Arrays.asList("there", "hi")));
        assertEquals(0, s.size());
    }

    @Test
    public void testLiveBackingSet5() {
        List<String> l = new ArrayList<>();
        l.addAll(Arrays.asList("hi", "there"));

        Set<CharSequence> s = Converters.standardConverter().convert(l).view().to(
                new TypeReference<Set<CharSequence>>() {});

        assertTrue(s.retainAll(Arrays.asList("hi", "!!")));
        assertEquals(new HashSet<>(Collections.singleton("hi")), s);
    }
}
