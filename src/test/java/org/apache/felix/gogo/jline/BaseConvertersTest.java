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
package org.apache.felix.gogo.jline;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaseConvertersTest {

    @Test
    public void testFunctionProxy() throws Exception {
        Function function = new Function() {
            @Override
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return "Hello ";
            }
            public String toString() {
                return "MyFunction";
            }
        };
        MyType myType = (MyType) new BaseConverters().convert(MyType.class, function);
        assertEquals("MyFunction", myType.toString());
        assertEquals("Hello ", myType.run(null));
        assertEquals("World !", myType.hello());
    }

    @FunctionalInterface
    public interface MyType {

        String toString();

        Object run(List<Object> args);

        default String hello() {
            return "World !";
        }

    }

}
