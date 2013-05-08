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
package org.apache.felix.scrplugin.description;

public enum PropertyType {

    String,
    Long,
    Double,
    Float,
    Integer,
    Byte,
    Char,
    Character,
    Boolean,
    Short,
    Password;

    public static PropertyType from(final Class<?> javaClass) {
        if ( javaClass.getName().equals(Long.class.getName())) {
            return PropertyType.Long;
        }
        if ( javaClass.getName().equals(Double.class.getName())) {
            return PropertyType.Double;
        }
        if ( javaClass.getName().equals(Float.class.getName())) {
            return PropertyType.Float;
        }
        if ( javaClass.getName().equals(Integer.class.getName())) {
            return PropertyType.Integer;
        }
        if ( javaClass.getName().equals(Byte.class.getName())) {
            return PropertyType.Byte;
        }
        if ( javaClass.getName().equals(Character.class.getName())) {
            return PropertyType.Character;
        }
        if ( javaClass.getName().equals(Boolean.class.getName())) {
            return PropertyType.Boolean;
        }
        if ( javaClass.getName().equals(Short.class.getName())) {
            return PropertyType.Short;
        }
        // default
        return PropertyType.String;
    }
}
