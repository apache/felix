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
package org.apache.felix.configurator.impl.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.felix.configurator.impl.TypeConverter;
import org.apache.felix.configurator.impl.Util;
import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.apache.felix.configurator.impl.model.BundleState;
import org.apache.felix.configurator.impl.model.Config;
import org.apache.felix.configurator.impl.model.ConfigPolicy;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.osgi.framework.Bundle;
import org.osgi.util.converter.TypeReference;

public class JSONUtil {

    private static final String INTERNAL_PREFIX = ":configurator:";

    private static final String PROP_VERSION = INTERNAL_PREFIX + "version";

    private static final String PROP_ENVIRONMENTS = "environments";

    private static final String PROP_RANKING = "ranking";

    private static final String PROP_POLICY = "policy";

    private static final String PROP_PID = "service.pid";

    public static BundleState readConfigurationsFromBundle(final Bundle bundle, final Set<String> paths) {
        final BundleState config = new BundleState();

        final List<ConfigurationFile> allFiles = new ArrayList<>();
        for(final String path : paths) {
            final List<ConfigurationFile> files = readJSON(bundle, path);
            allFiles.addAll(files);
        }
        Collections.sort(allFiles);

        config.addFiles(allFiles);

        return config;
    }

    /**
     * Read all json files from a given path in the bundle
     * @param bundle The bundle
     * @param path The path
     * @return A list of configuration files - sorted by url, might be empty.
     */
    public static List<ConfigurationFile> readJSON(final Bundle bundle, final String path) {
        final List<ConfigurationFile> result = new ArrayList<>();
        final Enumeration<URL> urls = bundle.findEntries(path, "*.json", false);
        if ( urls != null ) {
            while ( urls.hasMoreElements() ) {
                final URL url = urls.nextElement();

                final String filePath = url.getPath();
                final int pos = filePath.lastIndexOf('/');
                final String name = path + filePath.substring(pos);

                final String contents = Util.getResource(name, url);
                if ( contents != null ) {
                    boolean done = false;
                    final TypeConverter converter = new TypeConverter(bundle);
                    try {
                        final ConfigurationFile file = readJSON(converter, name, url, bundle.getBundleId(), contents);
                        if ( file != null ) {
                            result.add(file);
                            done = true;
                        }
                    } finally {
                        if ( !done ) {
                            converter.cleanupFiles();
                        }
                    }
                }
            }
            Collections.sort(result);
        } else {
            SystemLogger.error("No configurations found at path " + path);
        }
        return result;
    }

    /**
     * Read a single JSON file
     * @param converter type converter
     * @param name The name of the file
     * @param url The url to that file or {@code null}
     * @param bundleId The bundle id of the bundle containing the file
     * @param contents The contents of the file
     * @return The configuration file or {@code null}.
     */
    public static ConfigurationFile readJSON(
            final TypeConverter converter,
            final String name,
            final URL url,
            final long bundleId,
            final String contents) {
        final String identifier = (url == null ? name : url.toString());
        final JsonObject json = parseJSON(name, contents);
        final List<?> configs = verifyJSON(name, json);
        if ( configs != null ) {
            final List<Config> configurations = new ArrayList<>();
            for(final Object obj : configs) {
                if ( ! (obj instanceof JsonObject) ) {
                    SystemLogger.error("Ignoring configuration in '" + identifier + "' (not a configuration) : " + obj);
                } else {
                    final JsonObject mainMap = (JsonObject)obj;
                    final Object pid = getValue(mainMap, PROP_PID);
                    if ( ! (pid instanceof String) ) {
                        SystemLogger.error("Ignoring configuration in '" + identifier + "' (no service.pid) : " + obj);
                    } else {
                        Set<String> environments = null;
                        int ranking = 0;
                        ConfigPolicy policy = ConfigPolicy.DEFAULT;

                        final Dictionary<String, Object> properties = new Hashtable<>();
                        boolean valid = true;
                        for(final String mapKey : mainMap.keySet()) {
                            final Object value = getValue(mainMap, mapKey);

                            if ( mapKey.equals(PROP_PID) ) {
                                continue;
                            }

                            final boolean internalKey = mapKey.startsWith(INTERNAL_PREFIX);
                            String key = mapKey;
                            if ( internalKey ) {
                                key = key.substring(INTERNAL_PREFIX.length());
                            }
                            final int pos = key.indexOf(':');
                            String typeInfo = null;
                            if ( pos != -1 ) {
                                typeInfo = key.substring(pos + 1);
                                key = key.substring(0, pos);
                            }

                            if ( internalKey ) {
                                // no need to do type conversion based on typeInfo for internal props, type conversion is done directly below
                                if ( key.equals(PROP_ENVIRONMENTS) ) {
                                    environments = TypeConverter.getConverter().convert(value).defaultValue(null).to(new TypeReference<Set<String>>() {});
                                    if ( environments == null ) {
                                        SystemLogger.warning("Invalid environments for configuration in '" + identifier + "' : " + pid + " - " + value);
                                    }
                                } else if ( key.equals(PROP_RANKING) ) {
                                    final Integer intObj = TypeConverter.getConverter().convert(value).defaultValue(null).to(Integer.class);
                                    if ( intObj == null ) {
                                        SystemLogger.warning("Invalid ranking for configuration in '" + identifier + "' : " + pid + " - " + value);
                                    } else {
                                        ranking = intObj.intValue();
                                    }
                                } else if ( key.equals(PROP_POLICY) ) {
                                    final String stringVal = TypeConverter.getConverter().convert(value).defaultValue(null).to(String.class);
                                    if ( stringVal == null ) {
                                        SystemLogger.error("Invalid policy for configuration in '" + identifier + "' : " + pid + " - " + value);
                                    } else {
                                        if ( value.equals("default") || value.equals("force") ) {
                                            policy = ConfigPolicy.valueOf(stringVal.toUpperCase());
                                        } else {
                                            SystemLogger.error("Invalid policy for configuration in '" + identifier + "' : " + pid + " - " + value);
                                        }
                                    }
                                }
                            } else {
                                try {
                                    Object convertedVal = converter.convert(value, typeInfo);
                                    if ( convertedVal == null ) {
                                        convertedVal = value.toString();
                                    }
                                    properties.put(mapKey, convertedVal);
                                } catch ( final IOException io ) {
                                    SystemLogger.error("Invalid value/type for configuration in '" + identifier + "' : " + pid + " - " + mapKey);
                                    valid = false;
                                    break;
                                }
                            }
                        }

                        if ( valid ) {
                            final Config c = new Config((String)pid, environments, properties, bundleId, ranking, policy);
                            c.setFiles(converter.flushFiles());
                            configurations.add(c);
                        }
                    }
                }
            }
            final ConfigurationFile file = new ConfigurationFile(url, configurations);

            return file;
        }
        return null;
    }

    /**
     * Parse a JSON content
     * @param name The name of the file
     * @param contents The contents
     * @return The parsed JSON object or {@code null} on failure,
     */
    public static JsonObject parseJSON(final String name, String contents) {
        // minify JSON first (remove comments)
        try (final Reader in = new StringReader(contents);
             final Writer out = new StringWriter()) {
            final JSMin min = new JSMin(in, out);
            min.jsmin();
            contents = out.toString();
        } catch ( final IOException ioe) {
            SystemLogger.error("Invalid JSON from " + name);
            return null;
        }
        try (final JsonReader reader = Json.createReader(new StringReader(contents)) ) {
            final JsonStructure obj = reader.read();
            if ( obj != null && obj.getValueType() == ValueType.OBJECT ) {
                return (JsonObject)obj;
            }
            SystemLogger.error("Invalid JSON from " + name);
        }
        return null;
    }

    /**
     * Get the value of a JSON property
     * @param root The JSON Object
     * @param key The key in the JSON Obejct
     * @return The value or {@code null}
     */
    public static Object getValue(final JsonObject root, final String key) {
        if ( !root.containsKey(key) ) {
            return null;
        }
        final JsonValue value = root.get(key);
        return getValue(value);
    }

    public static Object getValue(final JsonValue value) {
        switch ( value.getValueType() ) {
            // type NULL -> return null
            case NULL : return null;
            // type TRUE or FALSE -> return boolean
            case FALSE : return false;
            case TRUE : return true;
            // type String -> return String
            case STRING : return ((JsonString)value).getString();
            // type Number -> return long or double
            case NUMBER : final JsonNumber num = (JsonNumber)value;
                          if (num.isIntegral()) {
                               return num.longValue();
                          }
                          return num.doubleValue();
            // type ARRAY -> return list and call this method for each value
            case ARRAY : final List<Object> array = new ArrayList<>();
                         for(final JsonValue x : ((JsonArray)value)) {
                             array.add(getValue(x));
                         }
                         return array;
            // type OBJECT -> return object
            case OBJECT : return value;
        }
        return null;
    }

    /**
     * Verify the JSON according to the rules
     * @param name The JSON name
     * @param root The JSON root object.
     * @return JSON array with configurations or {@code null}
     */
    public static List<?> verifyJSON(final String name, final JsonObject root) {
        if ( root == null ) {
            return null;
        }
        final Object version = getValue(root, PROP_VERSION);
        if ( version != null ) {

            final int v = TypeConverter.getConverter().convert(version).defaultValue(-1).to(Integer.class);
            if ( v == -1 ) {
                SystemLogger.error("Invalid version information in " + name + " : " + version);
                return null;
            }
            // we only support version 1
            if ( v != 1 ) {
                SystemLogger.error("Invalid version number in " + name + " : " + version);
                return null;
            }
        }
        final Object configs = getValue(root, "configurations");
        if ( configs == null ) {
            // short cut, we just return false as we don't have to process this file
            return null;
        }
        if ( !(configs instanceof List) ) {
            SystemLogger.error("Configurations must be an array of configurations in " + name);
            return null;
        }
        return (List<?>) configs;
    }
}
