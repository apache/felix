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
package org.apache.felix.converter.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Converting;

public class ConverterService implements Converter {
    private final Adapter adapter;

    public ConverterService() {
        Adapter a = new ConverterImpl().getAdapter();
        a.rule(Byte.class, String.class, v -> v.toString(), Byte::parseByte); // TODO test
        a.rule(Character.class, Boolean.class, v -> v.charValue() != 0,
                v -> v.booleanValue() ? (char) 1 : (char) 0);
        a.rule(Character.class, String.class, v -> v.toString(),
                v -> v.length() > 0 ? v.charAt(0) : 0);
        a.rule(Class.class, String.class, Class::toString,
                v -> getClass().getClassLoader().loadClass(v));
        a.rule(Double.class, String.class, v -> v.toString(), Double::parseDouble); // TODO test
        a.rule(Float.class, String.class, v -> v.toString(), Float::parseFloat); // TODO test
        a.rule(Integer.class, String.class, v -> v.toString(), Integer::parseInt);
        a.rule(LocalDateTime.class, String.class, LocalDateTime::toString, LocalDateTime::parse);
        a.rule(LocalDate.class, String.class, LocalDate::toString, LocalDate::parse);
        a.rule(LocalTime.class, String.class, LocalTime::toString, LocalTime::parse);
        a.rule(Long.class, String.class, v -> v.toString(), Long::parseLong); // TODO test
        a.rule(OffsetDateTime.class, String.class, OffsetDateTime::toString, OffsetDateTime::parse);
        a.rule(OffsetTime.class, String.class, OffsetTime::toString, OffsetTime::parse);
        a.rule(Pattern.class, String.class, Pattern::toString, Pattern::compile);
        a.rule(Short.class, String.class, v -> v.toString(), Short::parseShort); // TODO test
        a.rule(UUID.class, String.class, UUID::toString, UUID::fromString);
        a.rule(ZonedDateTime.class, String.class, ZonedDateTime::toString, ZonedDateTime::parse);
        adapter = a;
    }

    @Override
    public Converting convert(Object obj) {
        return adapter.convert(obj);
    }

    @Override
    public Adapter getAdapter() {
        return adapter.getAdapter();
    }
}
