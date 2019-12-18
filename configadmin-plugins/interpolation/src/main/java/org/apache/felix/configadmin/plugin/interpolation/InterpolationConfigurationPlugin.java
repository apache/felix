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
package org.apache.felix.configadmin.plugin.interpolation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.util.converter.Converters;
import org.slf4j.Logger;

class InterpolationConfigurationPlugin implements ConfigurationPlugin {
    private static final String PREFIX = "$[";
    private static final String SUFFIX = "]";

    private static final String ENV_PREFIX = PREFIX + "env:";
    private static final Pattern ENV_PATTERN = createPattern(ENV_PREFIX);
    private static final String PROP_PREFIX = PREFIX + "prop:";
    private static final Pattern PROP_PATTERN = createPattern(PROP_PREFIX);
    private static final String SECRET_PREFIX = PREFIX + "secret:";
    private static final Pattern SECRET_PATTERN = createPattern(SECRET_PREFIX);

    private static final Map<String, Class<?>> TYPE_MAP = new HashMap<>();
    static {
        // scalar types and primitive types
        TYPE_MAP.put("String", String.class);
        TYPE_MAP.put("Integer", Integer.class);
        TYPE_MAP.put("int", Integer.class);
        TYPE_MAP.put("Long", Long.class);
        TYPE_MAP.put("long", Long.class);
        TYPE_MAP.put("Float", Float.class);
        TYPE_MAP.put("float", Float.class);
        TYPE_MAP.put("Double", Double.class);
        TYPE_MAP.put("double", Double.class);
        TYPE_MAP.put("Byte", Byte.class);
        TYPE_MAP.put("byte", Byte.class);
        TYPE_MAP.put("Short", Short.class);
        TYPE_MAP.put("short", Short.class);
        TYPE_MAP.put("Character", Character.class);
        TYPE_MAP.put("char", Character.class);
        TYPE_MAP.put("Boolean", Boolean.class);
        TYPE_MAP.put("boolean", Boolean.class);
         // array of scalar types and primitive types
        TYPE_MAP.put("String[]", String[].class);
        TYPE_MAP.put("Integer[]", Integer[].class);
        TYPE_MAP.put("int[]", int[].class);
        TYPE_MAP.put("Long[]", Long[].class);
        TYPE_MAP.put("long[]", long[].class);
        TYPE_MAP.put("Float[]", Float[].class);
        TYPE_MAP.put("float[]", float[].class);
        TYPE_MAP.put("Double[]", Double[].class);
        TYPE_MAP.put("double[]", double[].class);
        TYPE_MAP.put("Byte[]", Byte[].class);
        TYPE_MAP.put("byte[]", byte[].class);
        TYPE_MAP.put("Short[]", Short[].class);
        TYPE_MAP.put("short[]", short[].class);
        TYPE_MAP.put("Boolean[]", Boolean[].class);
        TYPE_MAP.put("boolean[]", boolean[].class);
        TYPE_MAP.put("Character[]", Character[].class);
        TYPE_MAP.put("char[]", char[].class);
    }

    private static Pattern createPattern(String prefix) {
        return Pattern.compile("\\Q" + prefix + "\\E.+?\\Q" + SUFFIX + "\\E");
    }

    private final BundleContext context;
    private final File directory;

    InterpolationConfigurationPlugin(BundleContext bc, String dir) {
        context = bc;
        if (dir != null) {
            directory = new File(dir);
            getLog().info("Configured directory for secrets: {}", dir);
        } else {
            directory = null;
        }
    }

    private Logger getLog() {
        return Activator.LOG;
    }

    @Override
    public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
        final Object pid = properties.get(Constants.SERVICE_PID);
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            Object val = properties.get(key);
            if (val instanceof String) {
                Object newVal = replace(key, pid, (String) val);
                if (newVal != null && !newVal.equals(val)) {
                    properties.put(key, newVal);
                    getLog().info("Replaced value of configuration property '{}' for PID {}", key, pid);
                }
            } else if (val instanceof String[]) {
                String[] array = (String[]) val;
                String[] newArray = null;
                for (int i = 0; i < array.length; i++) {
                    Object newVal = replace(key, pid, array[i]);
                    if (newVal != null && !newVal.equals(array[i])) {
                        if (newArray == null) {
                            newArray = new String[array.length];
                            System.arraycopy(array, 0, newArray, 0, array.length);
                        }
                        newArray[i] = newVal.toString();
                    }
                }
                if (newArray != null) {
                    properties.put(key, newArray);
                    getLog().info("Replaced value of configuration property '{}' for PID {}", key, pid);
                }
            }
        }
    }

    private Object replace(final String key, Object pid, String sv) {
        int idx = sv.indexOf(PREFIX);
        if (idx != -1) {
            String varStart = sv.substring(idx);
            Object newVal = null;
            if (varStart.startsWith(SECRET_PREFIX)) {
                newVal = replaceVariablesFromFile(key, sv, pid);
            } else if (varStart.startsWith(ENV_PREFIX)) {
                newVal = replaceVariablesFromEnvironment(sv);
            } else if (varStart.startsWith(PROP_PREFIX)) {
                newVal = replaceVariablesFromProperties(sv);
            }

            return newVal;
        }
        return null;
    }

    Object replaceVariablesFromEnvironment(final String value) {
        return replaceVariables(ENV_PREFIX, ENV_PATTERN, value, n -> System.getenv(n));
    }

    Object replaceVariablesFromProperties(final String value) {
        return replaceVariables(PROP_PREFIX, PROP_PATTERN, value, n -> context.getProperty(n));
    }

    Object replaceVariablesFromFile(final String key, final String value, final Object pid) {
        if (directory == null) {
            getLog().warn("Cannot replace property value {} for PID {}. No directory configured via framework property " +
                    Activator.DIR_PROPERTY, key, pid);
            return null;
        }

        return replaceVariables(SECRET_PREFIX, SECRET_PATTERN, value, n -> {
            if (n.contains("..")) {
                getLog().error("Illegal secret location: " + n + " Going up in the directory structure is not allowed");
                return null;
            }

            File file = new File(directory, n);
            if (!file.isFile()) {
                getLog().warn("Cannot replace variable. Configured path is not a regular file: " + file);
                return null;
            }
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                getLog().error("Problem replacing configuration property '{}' for PID {} from file {}",
                        key, pid, file, e);

                return null;
            }
            return new String(bytes).trim();
        });
    }

    Object replaceVariables(final String prefix, final Pattern pattern,
            final String value, 
            final Function<String, String> valueSource) {
        final Matcher m = pattern.matcher(value);
        final StringBuffer sb = new StringBuffer();
        String type = null;
        while (m.find()) {
            final String var = m.group();

            final int len = var.length();
            final int idx = var.indexOf(';');

            final Map<String, String> directives;
            final int endIdx;
            if (idx >= 0) {
                endIdx = idx;
                directives = parseDirectives(var.substring(idx, len - SUFFIX.length()));
            } else {
                endIdx = len - SUFFIX.length();
                directives = Collections.emptyMap();
            }

            final String varName = var.substring(prefix.length(), endIdx);
            String replacement = valueSource.apply(varName);
            if (replacement != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                String defVal = directives.get("default");
                if (defVal != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(defVal));
                }
            }
            type = directives.get("type");
        }
        m.appendTail(sb);

        return convertType(type, sb.toString());
    }

    private Map<String, String> parseDirectives(String dirString) {
        Map<String, String> dirs = new HashMap<>();

        for (String dir : dirString.split(";")) {
            String[] kv = dir.split("=");
            if (kv.length == 2) {
                dirs.put(kv[0], kv[1]);
            }
        }

        return dirs;
    }

    private Object convertType(String type, String s) {
        if (type == null) {
            return s;
        }

        Class<?> cls = TYPE_MAP.get(type);
        if (cls != null) {
            return Converters.standardConverter().convert(s).to(cls);
        }

        getLog().warn("Cannot convert to type: " + type);
        return s;
    }
}
