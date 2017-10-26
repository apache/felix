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

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Functioning;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.TypeRule;
import org.osgi.util.function.Function;

/**
 * Top-level implementation of the Converter. This class contains a number of rules
 * that cover 'special cases'. <p>
 *
 * Note that this class avoids lambda's and hard dependencies on Java-8 (or later) types
 * to also work under Java 7.
 */
public class ConverterImpl implements InternalConverter {
    private static final SimpleDateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    static {
        ISO8601_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public InternalConverting convert(Object obj) {
        return new ConvertingImpl(this, obj);
    }

    @Override
    public Functioning function() {
        return new FunctioningImpl(this);
    }

    public void addStandardRules(ConverterBuilder cb) {
        // Not written using lambda's because this code needs to run with Java 7
        cb.rule(new Rule<Calendar, String>(new Function<Calendar, String>() {
            @Override
            public String apply(Calendar f) {
                return ISO8601_DATE_FORMAT.format(f.getTime());
            }
        }) {});

        cb.rule(new Rule<String, Calendar>(new Function<String, Calendar>() {
            @Override
            public Calendar apply(String f) {
                try {
                    Calendar cc = Calendar.getInstance();
                    cc.setTime(ISO8601_DATE_FORMAT.parse(f));
                    return cc;
                } catch (ParseException e) {
                    throw new ConversionException("Cannot convert " + f + " to Date", e);
                }
            }
        }) {});

        cb.rule(new Rule<Calendar, Long>(new Function<Calendar, Long>() {
            @Override
            public Long apply(Calendar f) {
                return f.getTime().getTime();
            }
        }) {});

        cb.rule(new Rule<Long, Calendar>(new Function<Long, Calendar>() {
            @Override
            public Calendar apply(Long f) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(f);
                return c;
            }
        }) {});

        cb.rule(new Rule<Character, Boolean>(new Function<Character, Boolean>() {
            @Override
            public Boolean apply(Character c) {
                return c.charValue() != 0;
            }
        }) {});

        cb.rule(new Rule<Boolean, Character>(new Function<Boolean, Character>() {
            @Override
            public Character apply(Boolean b) {
                return b.booleanValue() ? (char) 1: (char) 0;
            }
        }) {});

        cb.rule(new Rule<Character, Integer>(new Function<Character, Integer>() {
            @Override
            public Integer apply(Character c) {
                return (int) c.charValue();
            }
        }) {});

        cb.rule(new Rule<Character, Long>(new Function<Character, Long>() {
            @Override
            public Long apply(Character c) {
                return (long) c.charValue();
            }
        }) {});

        cb.rule(new Rule<String, Character>(new Function<String, Character>() {
            @Override
            public Character apply(String f) {
                return f.length() > 0 ? f.charAt(0) : 0;
            }
        }) {});

        cb.rule(new Rule<String, Class<?>>(new Function<String, Class<?>>() {
            @Override
            public Class<?> apply(String cn) {
                return loadClassUnchecked(cn);
            }
        }) {});

        cb.rule(new Rule<Date, Long>(new Function<Date, Long>() {
            @Override
            public Long apply(Date d) {
                return d.getTime();
            }
        }) {});

        cb.rule(new Rule<Long, Date>(new Function<Long, Date>() {
            @Override
            public Date apply(Long f) {
                return new Date(f);
            }
        }) {});

        cb.rule(new Rule<Date, String>(new Function<Date, String>() {
            @Override
            public String apply(Date d) {
                return ISO8601_DATE_FORMAT.format(d);
            }
        }) {});

        cb.rule(new Rule<String, Date>(new Function<String, Date>() {
            @Override
            public Date apply(String f) {
                try {
                    return ISO8601_DATE_FORMAT.parse(f);
                } catch (ParseException e) {
                    throw new ConversionException("Cannot convert " + f + " to Date", e);
                }
            }
        }) {});

        cb.rule(new Rule<String, Pattern>(new Function<String, Pattern>() {
            @Override
            public Pattern apply(String ps) {
                return Pattern.compile(ps);
            }
        }) {});

        cb.rule(new Rule<String, UUID>(new Function<String, UUID>() {
            @Override
            public UUID apply(String uuid) {
                return UUID.fromString(uuid);
            }
        }) {});

        // Special conversions between character arrays and String
        cb.rule(new Rule<char[], String>(new Function<char[], String>() {
            @Override
            public String apply(char[] ca) {
                return charArrayToString(ca);
            }
        }) {});

        cb.rule(new Rule<Character[], String>(new Function<Character[], String>() {
            @Override
            public String apply(Character[] ca) {
                return characterArrayToString(ca);
            }
        }) {});

        cb.rule(new Rule<String, char[]>(new Function<String, char[]>() {
            @Override
            public char[] apply(String s) {
                return stringToCharArray(s);
            }
        }) {});

        cb.rule(new Rule<String, Character[]>(new Function<String, Character[]>() {
            @Override
            public Character[] apply(String s) {
                return stringToCharacterArray(s);
            }
        }) {});

        cb.rule(new Rule<Number, Boolean>(new Function<Number, Boolean>() {
            @Override
            public Boolean apply(Number obj) {
                return obj.longValue() != 0;
            }
        }) {});

        cb.rule(new Rule<Number, Character>(new Function<Number, Character>() {
            @Override
            public Character apply(Number obj) {
                return Character.valueOf((char) obj.intValue());
            }
        }) {});

        reflectiveAddRule(cb, "java.time.LocalDateTime", "parse");
        reflectiveAddRule(cb, "java.time.LocalDate", "parse");
        reflectiveAddRule(cb, "java.time.LocalTime", "parse");
        reflectiveAddRule(cb, "java.time.OffsetDateTime", "parse");
        reflectiveAddRule(cb, "java.time.OffsetTime", "parse");
        reflectiveAddRule(cb, "java.time.ZonedDateTime", "parse");
    }

    private void reflectiveAddRule(ConverterBuilder cb, String toClsName, String methodName) {
        try {
            final Class<?> toCls = getClass().getClassLoader().loadClass(toClsName);
            final Method toMethod = toCls.getMethod(methodName, CharSequence.class);

            cb.rule(new TypeRule<String, Object>(String.class, toCls, new Function<String, Object>() {
                @Override
                public Object apply(String f) {
                    try {
                        return toMethod.invoke(null, f);
                    } catch (Exception e) {
                        throw new ConversionException("Problem converting to " + toCls, e);
                    }
                }
            }));
        } catch (Exception ex) {
            // Class not available, do not add rule for it
        }
    }

    private String charArrayToString(char[] ca) {
        StringBuilder sb = new StringBuilder(ca.length);
        for (char c : ca) {
            sb.append(c);
        }
        return sb.toString();
    }

    private String characterArrayToString(Character[] ca) {
        return charArrayToString(convert(ca).to(char[].class));
    }

    private char[] stringToCharArray(String s) {
        char[] ca = new char[s.length()];

        for (int i=0; i<s.length(); i++) {
            ca[i] = s.charAt(i);
        }
        return ca;
    }

    private Character[] stringToCharacterArray(String s) {
        return convert(stringToCharArray(s)).to(Character[].class);
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
