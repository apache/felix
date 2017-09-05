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
package org.apache.felix.serializer.impl.json;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.osgi.dto.DTO;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterFunction;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TargetRule;

import static org.junit.Assert.assertEquals;

public class JsonBackingObjectSerializationTest {
    @Test
    @SuppressWarnings( "rawtypes" )
    public void testComplexMapSerializationFirstUsingConversion() {
        final MyDTOishObject obj = MyDTOishObject.factory( "A", "B" );
        final Map m = Converters
                .standardConverter()
                .convert(obj)
                .sourceAsDTO()
                .to(Map.class);

        final String actual = new JsonSerializerImpl()
                .serialize(m)
                .writeWith(new JsonWriterFactory().newDebugWriter(Converters.standardConverter()))
                .toString();

        assertEquals(EXPECTED, actual);
    }

    @Test
    public void testComplexMapSerializationWithoutUsingPreConversion() {
        final JsonWriterFactory factory = new JsonWriterFactory();
        final String actual = new JsonSerializerImpl()
                .serialize(MyDTOishObject.factory("A", "B"))
                .writeWith(factory.newDebugWriter(Converters.standardConverter()))
                .toString();

        assertEquals(EXPECTED, actual);
    }

    @Test
    public void testComplexMapSerializationUsingRule() {
        final Converter converter = Converters.newConverterBuilder().rule(new MapTargetRule()).build();
        final JsonWriterFactory factory = new JsonWriterFactory();
        final String actual = new JsonSerializerImpl()
                .serialize(MyDTOishObject.factory("A", "B"))
                .convertWith(converter)
                .writeWith(factory.newDebugWriter(converter))
                .toString();

        assertEquals(EXPECTED, actual);
    }

    @Test
    public void testOrderedSerialization() {
        final String actual = new JsonSerializerImpl()
                .serialize(MyDTOishObject.factory("A", "B"))
                .writeWith(new JsonWriterFactory()
                        .orderMap("/", Arrays.asList("b", "a", "o", "l2", "l1"))
                        .orderMap("/l2", Arrays.asList("b", "a"))
                        .orderArray("/l1")
                        .newDebugWriter(Converters.standardConverter()))
                .toString();

        assertEquals(ORDERED, actual);
    }

    public static class MyDTOishObject extends DTO {
        public String a;
        public String b;
        public OtherObject o;
        public List<String> l1;
        public List<OtherObject> l2;

        public MyDTOishObject( String a, String b ) {
            this.a = a;
            this.b = b;
            o = OtherObject.factory(a + a, b + b);
            l1 = Stream.of("one", "two", "three", "four").collect(Collectors.toList());
            l2 = Stream.of(OtherObject.factory(a, b), OtherObject.factory(a + a, b + b)).collect(Collectors.toList());
        }

        public static MyDTOishObject factory( String a, String b ) {
            return new MyDTOishObject(a, b);
        }
    }

    public static class OtherObject extends DTO {
        public String a;
        public String b;

        public OtherObject(String a, String b) {
            this.a = a;
            this.b = b;
        }

        public static OtherObject factory(String a, String b) {
            return new OtherObject(a, b);
        }
    }

    static class MapTargetRule implements TargetRule {

        @Override
        public ConverterFunction getFunction() {
            return new MapConverterFunction();
        }

        @Override
        public Type getTargetType() {
            return Map.class;
        }        
    }

    static class MapConverterFunction implements ConverterFunction {

        @Override
        public Object apply( Object obj, Type targetType ) throws Exception {
            return Converters
                    .standardConverter()
                    .convert(obj)
                    .sourceAsDTO()
                    .to(targetType);
        }
    }    

    private static final String EXPECTED =
            "{\n" +
            "  \"a\":\"A\",\n" +
            "  \"b\":\"B\",\n" +
            "  \"l1\":[\n" +
            "    \"one\",\n" +
            "    \"two\",\n" +
            "    \"three\",\n" +
            "    \"four\"\n" +
            "  ],\n" +
            "  \"l2\":[\n" +
            "    {\n" +
            "      \"a\":\"A\",\n" +
            "      \"b\":\"B\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"a\":\"AA\",\n" +
            "      \"b\":\"BB\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"o\":{\n" +
            "    \"a\":\"AA\",\n" +
            "    \"b\":\"BB\"\n" +
            "  }\n" +
            "}";

    private static final String ORDERED =
            "{\n" +
            "  \"b\":\"B\",\n" +
            "  \"a\":\"A\",\n" +
            "  \"o\":{\n" +
            "    \"a\":\"AA\",\n" +
            "    \"b\":\"BB\"\n" +
            "  },\n" +
            "  \"l2\":[\n" +
            "    {\n" +
            "      \"b\":\"B\",\n" +
            "      \"a\":\"A\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"b\":\"BB\",\n" +
            "      \"a\":\"AA\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"l1\":[\n" +
            "    \"four\",\n" +
            "    \"one\",\n" +
            "    \"three\",\n" +
            "    \"two\"\n" +
            "  ]\n" +
            "}";
}
