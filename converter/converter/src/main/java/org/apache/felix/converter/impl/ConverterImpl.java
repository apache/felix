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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.osgi.util.converter.ConverterBuilder;

public class ConverterImpl implements InternalConverter {
    @Override
    public InternalConverting convert(Object obj) {
        return new ConvertingImpl(this, obj);
    }

    public void addStandardRules(ConverterBuilder cb) {
        cb.rule(Byte.class, String.class, v -> v.toString(), Byte::parseByte);
        cb.rule(Calendar.class, String.class, v -> v.getTime().toInstant().toString(),
                v -> {
                    Calendar cc = Calendar.getInstance();
                    cc.setTime(Date.from(Instant.parse(v)));
                    return cc;
                });
        cb.rule(Character.class, Boolean.class, v -> v.charValue() != 0,
                v -> v.booleanValue() ? (char) 1 : (char) 0);
        cb.rule(Character.class, String.class, v -> v.toString(),
                v -> v.length() > 0 ? v.charAt(0) : 0);
        cb.rule(Class.class, String.class, Class::toString,
                this::loadClassUnchecked);
        cb.rule(Date.class, Long.class, d -> d.getTime(), l -> new Date(l));
        cb.rule(Date.class, String.class, v -> v.toInstant().toString(),
                v -> Date.from(Instant.parse(v)));
        cb.rule(Double.class, String.class, v -> v.toString(), Double::parseDouble);
        cb.rule(Float.class, String.class, v -> v.toString(), Float::parseFloat);
        cb.rule(Integer.class, String.class, v -> v.toString(), Integer::parseInt);
        cb.rule(LocalDateTime.class, String.class, LocalDateTime::toString, LocalDateTime::parse);
        cb.rule(LocalDate.class, String.class, LocalDate::toString, LocalDate::parse);
        cb.rule(LocalTime.class, String.class, LocalTime::toString, LocalTime::parse);
        cb.rule(Long.class, String.class, v -> v.toString(), Long::parseLong);
        cb.rule(OffsetDateTime.class, String.class, OffsetDateTime::toString, OffsetDateTime::parse);
        cb.rule(OffsetTime.class, String.class, OffsetTime::toString, OffsetTime::parse);
        cb.rule(Pattern.class, String.class, Pattern::toString, Pattern::compile);
        cb.rule(Short.class, String.class, v -> v.toString(), Short::parseShort);
        cb.rule(UUID.class, String.class, UUID::toString, UUID::fromString);
        cb.rule(ZonedDateTime.class, String.class, ZonedDateTime::toString, ZonedDateTime::parse);
    }

    private Class<?> loadClassUnchecked(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(className);
        }
    }

    @Override
    public ConverterBuilderImpl newConverterBuilder() {
        return new ConverterBuilderImpl(this);
    }
}
