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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* User: guillaume
* Date: 08/07/13
* Time: 17:37
*/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleTypes {
    boolean aBoolean() default true;
    byte aByte() default 42;
    long aLong() default 42;
    int anInt() default 42;
    short aShort() default 42;
    float aFloat() default 42;
    double aDouble() default 42;
    char aChar() default 'a';

    String aString() default "42";
    Class<?> aClass() default String.class;

    boolean[] arrayOfBoolean() default {true, true};
    byte[] arrayOfByte() default {42};
    long[] arrayOfLong() default {42};
    int[] arrayOfInt() default {42};
    short[] arrayOfShort() default {42};
    float[] arrayOfFloat() default 42;
    double[] arrayOfDouble() default {};
    char[] arrayOfChar() default {'a', 'b', 'c'};

}
