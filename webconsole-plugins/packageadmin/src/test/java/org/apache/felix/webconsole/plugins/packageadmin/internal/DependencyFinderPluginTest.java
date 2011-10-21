/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.webconsole.plugins.packageadmin.internal;

import java.util.Iterator;
import java.util.SortedSet;

import static org.junit.Assert.*;
import org.junit.Test;

public class DependencyFinderPluginTest {
    
    @Test
    public void single() {
        SortedSet<String> r = DependencyFinderPlugin.getPackageNames("com.foo.bar");
        assertEquals(1, r.size());
        assertEquals("com.foo.bar", r.first());
    }
    
    @Test
    public void singleClass() {
        SortedSet<String> r = DependencyFinderPlugin.getPackageNames("com.foo.bar.MyClass");
        assertEquals(1, r.size());
        assertEquals("com.foo.bar", r.first());
    }
    
    @Test
    public void multiple() {
        SortedSet<String> r = DependencyFinderPlugin.getPackageNames("com.foo.bar\ncom.foo.bar2\ncom.foo.bar3.MyClass\ncom.foo.bar.MyClass");
        assertEquals(3, r.size());
        Iterator<String> it = r.iterator();
        assertEquals("com.foo.bar", it.next());
        assertEquals("com.foo.bar2", it.next());
        assertEquals("com.foo.bar3", it.next());
    }

}