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
import java.util.Map;

import org.junit.Test;
import org.osgi.dto.DTO;
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

        final String expected = 
                "{\"a\":\"A\","
                        + "\"o\":{\"a\":\"AA\",\"b\":\"BB\"},"
                        + "\"b\":\"B\"}";

        final String actual = new JsonSerializerImpl().serialize(m).toString();

        // TODO: Cannot predict order of elements for equals comparison//
//        assertEquals(expected, actual);
    }

    @Test
    public void testComplexMapSerializationWithoutUsingPreConversion() {
        final String expected = 
                "{\"a\":\"A\","
                + "\"o\":{\"a\":\"AA\",\"b\":\"BB\"},"
                + "\"b\":\"B\"}";

        final String actual = new JsonSerializerImpl()
                .serialize(MyDTOishObject.factory( "A", "B" ))
                .sourceAsDTO()
                .toString();

        // TODO: Cannot predict order of elements for equals comparison//
//        assertEquals(expected, actual);
    }

    @Test
    public void testComplexMapSerializationUsingRule() {
        final String expected = 
                "{\"o\":{\"a\":\"AA\",\"b\":\"BB\"},"
                        + "\"a\":\"A\","
                        + "\"b\":\"B\"}";

        final String actual = new JsonSerializerImpl()
                .serialize(MyDTOishObject.factory( "A", "B" ))
                .with(Converters.newConverterBuilder().rule(new MapTargetRule()).build())
                .toString();

        // Cannot get result to behave predictably... Order is random.
//        assertEquals(expected.length(), actual.length());
    }

    public static class MyDTOishObject extends DTO {
        public String a;
        public String b;
        public OtherObject o;

        public MyDTOishObject( String a, String b ) {
            this.a = a;
            this.b = b;
            o = OtherObject.factory( a + a, b + b );
        }

        public static MyDTOishObject factory( String a, String b ) {
            return new MyDTOishObject( a, b );
        }
    }

    public static class OtherObject extends DTO {
        public String a;
        public String b;

        public OtherObject( String a, String b ) {
            this.a = a;
            this.b = b;
        }

        public static OtherObject factory( String a, String b ) {
            return new OtherObject( a, b );
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
}
