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
import org.osgi.util.converter.Rule;

public class ConverterImpl implements InternalConverter {
    @Override
    public InternalConverting convert(Object obj) {
        return new ConvertingImpl(this, obj);
    }

    public void addStandardRules(ConverterBuilder cb) {
        cb.rule(new Rule<Calendar, String>(f -> f.getTime().toInstant().toString()) {});
        cb.rule(new Rule<String, Calendar>(f -> {
            Calendar cc = Calendar.getInstance();
            cc.setTime(Date.from(Instant.parse(f)));
            return cc;
        }) {});
        cb.rule(new Rule<Calendar,Long>(f -> f.getTime().getTime()) {});
        cb.rule(new Rule<Long,Calendar>(f -> new Calendar.Builder().setInstant(f).build()) {});
        cb.rule(new Rule<Character,Boolean>(c -> c.charValue() != 0) {});
        cb.rule(new Rule<Boolean,Character>(b -> b.booleanValue() ? (char) 1 : (char) 0) {});
        cb.rule(new Rule<String,Character>(f -> f.length() > 0 ? f.charAt(0) : 0) {});
        cb.rule(new Rule<String,Class<?>>(this::loadClassUnchecked) {});
        cb.rule(new Rule<Date,Long>(Date::getTime) {});
        cb.rule(new Rule<Long,Date>(f -> new Date(f)) {});
        cb.rule(new Rule<Date,String>(f -> f.toInstant().toString()) {});
        cb.rule(new Rule<String,Date>(f -> Date.from(Instant.parse(f))) {});
        cb.rule(new Rule<String, LocalDateTime>(LocalDateTime::parse) {});
        cb.rule(new Rule<String, LocalDate>(LocalDate::parse) {});
        cb.rule(new Rule<String, LocalTime>(LocalTime::parse) {});
        cb.rule(new Rule<String, OffsetDateTime>(OffsetDateTime::parse) {});
        cb.rule(new Rule<String, OffsetTime>(OffsetTime::parse) {});
        cb.rule(new Rule<String, Pattern>(Pattern::compile) {});
        cb.rule(new Rule<String, UUID>(UUID::fromString) {});
        cb.rule(new Rule<String, ZonedDateTime>(ZonedDateTime::parse) {});
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
