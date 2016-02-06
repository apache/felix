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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to describe a property key-value(s) pair. It is used for example when
 * declaring {@link Component#properties()} attribute.<p>
 * 
 * Property value(s) type is String by default, and the type is scalar if the value is single-valued, 
 * or an array if the value is multi-valued. You can apply this annotation on a component class multiple times
 * (it's a java8 repeatable property).
 * 
 * Eight primitive types are supported:
 * <ul>
 * <li> String (default type)
 * <li> Long
 * <li> Double
 * <li> Float
 * <li> Integer
 * <li> Byte
 * <li> Boolean
 * <li> Short
 * </ul>
 * 
 * You can specify the type of a property either using a combination of <code>value</code> and <code>type</code> attributes,
 * or using one of the <code>longValue/doubleValue/floatValue/intValue/byteValue/charValue/booleanValue/shortValue</code> attributes.
 * 
 * Notice that you can also specify service properties dynamically by returning a Map from a method
 * annotated with {@link Start}.
 * 
 * <h3>Usage Examples</h3>
 * <blockquote>
 * <pre>
 * &#64;Component
 * &#64;Property(name="p1", value="v")                      // String value type (scalar)
 * &#64;Property(name="p2", value={"s1", "s2"})             // Array of Strings
 * &#64;Property(name="service.ranking", intValue=10)       // Integer value type (scalar)
 * &#64;Property(name="p3", intValue={1,2})                 // Array of Integers
 * &#64;Property(name="p3", value="1", type=Long.class)     // Long value (scalar)
 * class ServiceImpl implements Service {
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target( { ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Repeatable(RepeatableProperty.class)
public @interface Property
{
    /**
     * Returns the property name.
     * @return this property name
     */
    String name();

    /**
     * Returns the property value(s). The property value(s) is (are) 
     * parsed using the <code>valueOf</code> method of the class specified in the #type attribute 
     * (which is <code>String</code> by default). When the property value is single-value, then 
     * the value type is scalar (not an array). If the property value is multi-valued, then the value type 
     * is an array of the type specified in the {@link #type()} attribute (String by default).
     * 
     * @return this property value(s).
     */
    String[] value() default {};
    
    /**
     * Specifies how the {@link #value()} or {@link #values()} attributes are parsed.
     * @return the property value type (String by default) used to parse {@link #value()} or {@link #values()} 
     * attribtues
     */
    Class<?> type() default String.class;    
    
    /**
     * A Long value or an array of Long values. 
     * @return the long value(s). 
     */
    long[] longValue() default {};

    /**
     * A Double value or an array of Double values. 
     * @return the double value(s). 
     */
    double[] doubleValue() default {};

    /**
     * A Float value or an array of Float values. 
     * @return the float value(s). 
     */
    float[] floatValue() default {};

    /**
     * An Integer value or an array of Integer values. 
     * @return the int value(s). 
     */
    int[] intValue() default {};

    /**
     * A Byte value or an array of Byte values. 
     * @return the byte value(s). 
     */
    byte[] byteValue() default {};

    /**
     * A Character value or an array of Character values. 
     * @return the char value(s). 
     */
    char[] charValue() default {};

    /**
     * A Boolean value or an array of Boolean values.
     * @return the boolean value(s). 
     */
    boolean[] booleanValue() default {};

    /**
     * A Short value or an array of Short values. 
     * @return the short value(s). 
     */
    short[] shortValue() default {};
    
    /**
     * Returns an array of property values.
     * The property value are parsed using the <code>valueOf</code> method of the class specified in the #type attribute 
     * (which is <code>String</code> by default).
     * 
     * @return an array of property values. 
     * @deprecated use {@link #value()} attribute.
     */
    String[] values() default {};    
}
