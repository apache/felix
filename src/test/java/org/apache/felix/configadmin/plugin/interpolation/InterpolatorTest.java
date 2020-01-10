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
package org.apache.felix.configadmin.plugin.interpolation;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.felix.configadmin.plugin.interpolation.Interpolator.Provider;
import org.junit.Test;

public class InterpolatorTest {

    @Test
    public void testNoValue() {
        assertEquals("$[foo:hello]", Interpolator.replace("$[foo:hello]", (type, name, dir) -> null));
    }

    @Test
    public void testValue() {
        assertEquals("hello world", Interpolator.replace("$[foo:hello]", (type, name, dir) -> {
            if ("foo".equals(type) && "hello".equals(name)) {
                return "hello world";
            }
            return null;
        }));
    }

    @Test
    public void testValueAndConstantText() {
        assertEquals("beforehello worldafter", Interpolator.replace("before$[foo:hello]after", (type, name, dir) -> {
            if ("foo".equals(type) && "hello".equals(name)) {
                return "hello world";
            }
            return null;
        }));
    }

    @Test
    public void testRecursion() {
        assertEquals("beforehello worldafter",
                Interpolator.replace("before$[foo:$[foo:inner]]after", (type, name, dir) -> {
                    if ("foo".equals(type) && "hello".equals(name)) {
                        return "hello world";
                    } else if ("foo".equals(type) && "inner".equals(name)) {
                        return "hello";
                    }
                    return null;
                }));
    }

    @Test
    public void testEscaping() {
        final Provider p = new Provider() {

            @Override
            public Object provide(String type, String name, Map<String, String> directives) {
                return "value";
            }
        };
        assertEquals("$[no:replacement]", Interpolator.replace("\\$[no:replacement]", p));
    }
}
