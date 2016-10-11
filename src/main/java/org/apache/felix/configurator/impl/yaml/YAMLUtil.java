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
package org.apache.felix.configurator.impl.yaml;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.configurator.impl.TypeConverter;
import org.apache.felix.configurator.impl.Util;
import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.apache.felix.configurator.impl.model.BundleState;
import org.apache.felix.configurator.impl.model.Config;
import org.apache.felix.configurator.impl.model.ConfigPolicy;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.osgi.framework.Bundle;
import org.osgi.converter.TypeReference;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class YAMLUtil {

    private static final String INTERNAL_PREFIX = ":configurator:";

    private static final String PROP_VERSION = INTERNAL_PREFIX + "version";

    private static final String PROP_ENVIRONMENTS = "environments";

    private static final String PROP_RANKING = "ranking";

    private static final String PROP_POLICY = "policy";

    public static BundleState readConfigurationsFromBundle(final Bundle bundle, final Set<String> paths) {
        final BundleState config = new BundleState();

        final List<ConfigurationFile> allFiles = new ArrayList<>();
        for(final String path : paths) {
            final List<ConfigurationFile> files = org.apache.felix.configurator.impl.yaml.YAMLUtil.readYAML(bundle, path);
            allFiles.addAll(files);
        }
        Collections.sort(allFiles);

        config.addFiles(allFiles);

        return config;
    }

    /**
     * Read all yaml files from a given path in the bundle
     * @param bundle The bundle
     * @param path The path
     * @return A list of configuration files - sorted by url, might be empty.
     */
    public static List<ConfigurationFile> readYAML(final Bundle bundle, final String path) {
        final List<ConfigurationFile> result = new ArrayList<>();
        final Enumeration<URL> urls = bundle.findEntries(path, "*.yaml", false);
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
                        final ConfigurationFile file = readYAML(converter, name, url, bundle.getBundleId(), contents);
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
     * Read a single YAML file
     * @param converter type converter
     * @param name The name of the file
     * @param url The url to that file or {@code null}
     * @param bundleId The bundle id of the bundle containing the file
     * @param contents The contents of the file
     * @return The configuration file or {@code null}.
     */
    public static ConfigurationFile readYAML(
            final TypeConverter converter,
            final String name,
            final URL url,
            final long bundleId,
            final String contents) {
        final String identifier = (url == null ? name : url.toString());
        final Map<String, Object> yaml = parseYAML(name, contents);
        final List<Object> configs = verifyYAML(name, yaml);
        if ( configs != null ) {
            final List<Config> configurations = new ArrayList<>();
            for(final Object obj : configs) {
                if ( ! (obj instanceof Map) ) {
                    SystemLogger.error("Ignoring configuration in '" + identifier + "' (not a configuration) : " + obj);
                } else {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> mainMap = (Map<String, Object>)obj;
                    if ( mainMap.size() != 1 ) {
                        SystemLogger.error("Ignoring configuration in '" + identifier + "' (more than one PID) : " + obj);
                    } else {
                        final String pid = mainMap.keySet().iterator().next();
                        final Object configObj = mainMap.values().iterator().next();
                        if ( ! (configObj instanceof Map) ) {
                            SystemLogger.error("Ignoring configuration in '" + identifier + "' (no configuration map) : " + obj);
                        } else {
                            Set<String> environments = null;
                            int ranking = 0;
                            ConfigPolicy policy = ConfigPolicy.DEFAULT;

                            @SuppressWarnings("unchecked")
                            final Map<String, Object> config = (Map<String, Object>)configObj;
                            final Dictionary<String, Object> properties = new Hashtable<>();
                            boolean valid = true;
                            for(final Map.Entry<String, Object> entry : config.entrySet()) {
                                String key = entry.getKey();
                                final boolean internalKey = key.startsWith(INTERNAL_PREFIX);
                                if ( internalKey ) {
                                    key = key.substring(INTERNAL_PREFIX.length());
                                }
                                final int pos = key.indexOf(':');
                                String typeInfo = null;
                                if ( pos != -1 ) {
                                    typeInfo = key.substring(pos + 1);
                                    key = key.substring(0, pos);
                                }

                                final Object value = entry.getValue();

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
                                            convertedVal = createYAML(value);
                                        }
                                        properties.put(key, convertedVal);
                                    } catch ( final IOException io ) {
                                        SystemLogger.error("Invalid value/type for configuration in '" + identifier + "' : " + pid + " - " + entry.getKey());
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
                }
            }
            final ConfigurationFile file = new ConfigurationFile(url, configurations);

            return file;
        }
        return null;
    }

    /**
     * Parse a YAML content
     * @param name The name of the file
     * @param contents The contents
     * @return The parsed YAML map or {@code null} on failure,
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseYAML(final String name, final String contents) {
        try {
            final Yaml yaml = new Yaml();
            try (final StringReader reader = new StringReader(contents) ) {
                final Object obj = yaml.load(reader);
                if ( obj instanceof Map ) {
                    return (Map<String, Object>)obj;
                }
                SystemLogger.error("Invalid YAML from " + name);
            }
        } catch ( final YAMLException ignore ) {
            SystemLogger.error("Unable to read YAML from " + name, ignore);
        }
        return null;
    }

    private static String createYAML(final Object data) throws IOException {
        final DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        try {
            final Yaml yaml = new Yaml(options);
            return yaml.dump(data);
        } catch ( final YAMLException ye) {
            throw new IOException(ye);
        }
    }

    /**
     * Verify the YAML according to the rules
     * @param name The YAML name
     * @param root The YAML root object.
     * @return List of objects if valid
     */
    @SuppressWarnings("unchecked")
    public static List<Object> verifyYAML(final String name, final Map<String, Object> root) {
        if ( root == null ) {
            return null;
        }
        final Object version = root.get(PROP_VERSION);
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
        final Object configs = root.get("configurations");
        if ( configs == null ) {
            // short cut, we just return false as we don't have to process this file
            return null;
        }
        if ( !(configs instanceof List) ) {
            SystemLogger.error("Configurations must be a list of configurations in " + name);
            return null;
        }
        return (List<Object>) configs;
    }
}
