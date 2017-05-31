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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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

import org.apache.felix.configurator.impl.model.BundleState;
import org.apache.felix.configurator.impl.model.Config;
import org.apache.felix.configurator.impl.model.ConfigPolicy;
import org.apache.felix.configurator.impl.model.ConfigurationFile;

public class JSONUtil {

    private static final String INTERNAL_PREFIX = ":configurator:";

    private static final String PROP_VERSION = INTERNAL_PREFIX + "json-version";

    private static final String PROP_RANKING = "ranking";

    private static final String PROP_POLICY = "policy";

    public static final class Report {

        public final List<String> warnings = new ArrayList<>();

        public final List<String> errors = new ArrayList<>();
    }

    /**
     * Read all configurations from a bundle
     * @param provider The bundle provider
     * @param paths The paths to read from
     * @param report The report for errors and warnings
     * @return The bundle state.
     */
    public static BundleState readConfigurationsFromBundle(final BinUtil.ResourceProvider provider,
            final Set<String> paths,
            final Report report) {
        final BundleState config = new BundleState();

        final List<ConfigurationFile> allFiles = new ArrayList<>();
        for(final String path : paths) {
            final List<ConfigurationFile> files = readJSON(provider, path, report);
            allFiles.addAll(files);
        }
        Collections.sort(allFiles);

        config.addFiles(allFiles);

        return config;
    }

    /**
     * Read all json files from a given path in the bundle
     *
     * @param provider The bundle provider
     * @param path The path
     * @param report The report for errors and warnings
     * @return A list of configuration files - sorted by url, might be empty.
     */
    public static List<ConfigurationFile> readJSON(final BinUtil.ResourceProvider provider,
            final String path,
            final Report report) {
        final List<ConfigurationFile> result = new ArrayList<>();
        final Enumeration<URL> urls = provider.findEntries(path, "*.json");
        if ( urls != null ) {
            while ( urls.hasMoreElements() ) {
                final URL url = urls.nextElement();

                final String filePath = url.getPath();
                final int pos = filePath.lastIndexOf('/');
                final String name = path + filePath.substring(pos);

                try {
                    final String contents = getResource(name, url);
                    boolean done = false;
                    final TypeConverter converter = new TypeConverter(provider);
                    try {
                        final ConfigurationFile file = readJSON(converter, name, url, provider.getBundleId(), contents, report);
                        if ( file != null ) {
                            result.add(file);
                            done = true;
                        }
                    } finally {
                        if ( !done ) {
                            converter.cleanupFiles();
                        }
                    }
                } catch ( final IOException ioe ) {
                    report.errors.add("Unable to read " + name + " : " + ioe.getMessage());
                }
            }
            Collections.sort(result);
        } else {
            report.errors.add("No configurations found at path " + path);
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
     * @param report The report for errors and warnings
     * @return The configuration file or {@code null}.
     */
    public static ConfigurationFile readJSON(
            final TypeConverter converter,
            final String name,
            final URL url,
            final long bundleId,
            final String contents,
            final Report report) {
        final String identifier = (url == null ? name : url.toString());
        final JsonObject json = parseJSON(name, contents, report);
        final Map<String, ?> configs = verifyJSON(name, json, report);
        if ( configs != null ) {
            final List<Config> list = readConfigurationsJSON(converter, bundleId, identifier, configs, report);
            if ( !list.isEmpty() ) {
                final ConfigurationFile file = new ConfigurationFile(url, list);

                return file;
            }
        }
        return null;
    }

    /**
     * Read the configurations JSON
     * @param converter The converter to use
     * @param bundleId The bundle id
     * @param identifier The identifier
     * @param configs The map containing the configurations
     * @param report The report for errors and warnings
     * @return The list of {@code Config}s or {@code null}
     */
    public static List<Config> readConfigurationsJSON(final TypeConverter converter,
            final long bundleId,
            final String identifier,
            final Map<String, ?> configs,
            final Report report) {
        final List<Config> configurations = new ArrayList<>();
        for(final Map.Entry<String, ?> entry : configs.entrySet()) {
            if ( ! (entry.getValue() instanceof Map) ) {
                report.errors.add("Ignoring configuration in '" + identifier + "' (not a configuration) : " + entry.getKey());
            } else {
                @SuppressWarnings("unchecked")
                final Map<String, ?> mainMap = (Map<String, ?>)entry.getValue();
                final int envIndex = entry.getKey().indexOf('[');
                if ( envIndex != -1 && !entry.getKey().endsWith("]") ) {
                    report.errors.add("Ignoring configuration in '" + identifier + "' (invalid environments definition) : " + entry.getKey());
                    continue;
                }
                final String pid;
                final Set<String> environments;
                if ( envIndex == -1 ) {
                    pid = entry.getKey();
                    environments = null;
                } else {
                    pid = entry.getKey().substring(0, envIndex);
                    environments = new HashSet<>(Arrays.asList(entry.getKey().substring(envIndex + 1, entry.getKey().length()).split(",")));
                    if ( environments.isEmpty() ) {
                        report.warnings.add("Invalid environments for configuration in '" + identifier + "' : " + pid);
                    }
                }

                int ranking = 0;
                ConfigPolicy policy = ConfigPolicy.DEFAULT;

                final Dictionary<String, Object> properties = new Hashtable<>();
                boolean valid = true;
                for(final String mapKey : mainMap.keySet()) {
                    final Object value = mainMap.get(mapKey);

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
                        if ( key.equals(PROP_RANKING) ) {
                            final Integer intObj = TypeConverter.getConverter().convert(value).defaultValue(null).to(Integer.class);
                            if ( intObj == null ) {
                                report.warnings.add("Invalid ranking for configuration in '" + identifier + "' : " + pid + " - " + value);
                            } else {
                                ranking = intObj.intValue();
                            }
                        } else if ( key.equals(PROP_POLICY) ) {
                            final String stringVal = TypeConverter.getConverter().convert(value).defaultValue(null).to(String.class);
                            if ( stringVal == null ) {
                                report.errors.add("Invalid policy for configuration in '" + identifier + "' : " + pid + " - " + value);
                            } else {
                                if ( value.equals("default") || value.equals("force") ) {
                                    policy = ConfigPolicy.valueOf(stringVal.toUpperCase());
                                } else {
                                    report.errors.add("Invalid policy for configuration in '" + identifier + "' : " + pid + " - " + value);
                                }
                            }
                        }
                    } else {
                        try {
                            Object convertedVal = converter.convert(pid, value, typeInfo);
                            if ( convertedVal == null ) {
                                convertedVal = value.toString();
                            }
                            properties.put(mapKey, convertedVal);
                        } catch ( final IOException io ) {
                            report.errors.add("Invalid value/type for configuration in '" + identifier + "' : " + pid + " - " + mapKey + " : " + io.getMessage());
                            valid = false;
                            break;
                        }
                    }
                }

                if ( valid ) {
                    final Config c = new Config(pid, environments, properties, bundleId, ranking, policy);
                    c.setFiles(converter.flushFiles());
                    configurations.add(c);
                }
            }
        }
        return configurations;
    }

    /**
     * Parse a JSON content
     * @param name The name of the file
     * @param contents The contents
     * @param report The report for errors and warnings
     * @return The parsed JSON object or {@code null} on failure,
     */
    public static JsonObject parseJSON(final String name,
            String contents,
            final Report report) {
        // minify JSON first (remove comments)
        try (final Reader in = new StringReader(contents);
             final Writer out = new StringWriter()) {
            final JSMin min = new JSMin(in, out);
            min.jsmin();
            contents = out.toString();
        } catch ( final IOException ioe) {
            report.errors.add("Invalid JSON from " + name);
            return null;
        }
        try (final JsonReader reader = Json.createReader(new StringReader(contents)) ) {
            final JsonStructure obj = reader.read();
            if ( obj != null && obj.getValueType() == ValueType.OBJECT ) {
                return (JsonObject)obj;
            }
            report.errors.add("Invalid JSON from " + name);
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
             // type OBJECT -> return map
             case OBJECT : final Map<String, Object> map = new HashMap<>();
                           final JsonObject obj = (JsonObject)value;
                           for(final Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                               map.put(entry.getKey(), getValue(entry.getValue()));
                           }
                           return map;
        }
        return null;
    }

    /**
     * Verify the JSON according to the rules
     * @param name The JSON name
     * @param root The JSON root object.
     * @param report The report for errors and warnings
     * @return JSON map with configurations or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ?> verifyJSON(final String name,
            final JsonObject root,
            final Report report) {
        if ( root == null ) {
            return null;
        }
        final Object version = getValue(root, PROP_VERSION);
        if ( version != null ) {

            final int v = TypeConverter.getConverter().convert(version).defaultValue(-1).to(Integer.class);
            if ( v == -1 ) {
                report.errors.add("Invalid version information in " + name + " : " + version);
                return null;
            }
            // we only support version 1
            if ( v != 1 ) {
                report.errors.add("Invalid version number in " + name + " : " + version);
                return null;
            }
        }
        final Object configs = getValue(root, "configurations");
        if ( configs == null ) {
            // short cut, we just return false as we don't have to process this file
            return null;
        }
        if ( !(configs instanceof Map) ) {
            report.errors.add("Configurations must be a map of configurations in " + name);
            return null;
        }
        return (Map<String, ?>) configs;
    }

    /**
     * Read the contents of a resource, encoded as UTF-8
     * @param name The resource name
     * @param url The resource URL
     * @return The contents
     * @throws IOException If anything goes wrong
     */
    public static String getResource(final String name, final URL url)
    throws IOException {
        final URLConnection connection = url.openConnection();

        try(final BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    connection.getInputStream(), "UTF-8"))) {

            final StringBuilder sb = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            return sb.toString();
        }
    }
}
