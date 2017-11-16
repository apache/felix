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

import org.junit.Test;
import org.osgi.util.function.Function;

import static org.junit.Assert.assertEquals;

public class ConverterFunctionTest {
    @Test
    public void testConverterFunction() {
        Converter c = Converters.standardConverter();
        assertEquals(12.5, c.convert("12.5").to(double.class), 0.001);

        Function<Object, Double> f = c.function().to(double.class);
        assertEquals(12.5, f.apply("12.5"), 0.001);
        assertEquals(50.505, f.apply("50.505"), 0.001);
    }

    @Test
    public void testConverterFunctionWithModifier() {
        Converter c = Converters.standardConverter();

        Function<Object, Integer> cf = c.function().defaultValue(999).to(Integer.class);

        assertEquals(Integer.valueOf(999),
                cf.apply(""));
        assertEquals(Integer.valueOf(999),
                c.convert("").defaultValue(999).to(Integer.class));

        assertEquals(Integer.valueOf(123),
                cf.apply("123"));
        assertEquals(Integer.valueOf(123),
                c.convert("123").defaultValue(999).to(Integer.class));
    }

    @Test
    public void testConverterFunctionWithRule() {
        Converter c = Converters.standardConverter();
        Function<Object, String> cf = c.function().to(String.class);

        String[] sa = new String [] {"h", "i"};
        assertEquals("h", cf.apply(sa));

        Converter ac = c.newConverterBuilder().
            rule(new Rule<String[],String>(v -> String.join("", v)) {}).
            build();

        Function<Object, String> af = ac.function().to(String.class);
        assertEquals("hi", af.apply(sa));
    }
}
