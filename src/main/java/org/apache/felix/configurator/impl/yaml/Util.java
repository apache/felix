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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.apache.felix.configurator.impl.model.BundleState;
import org.apache.felix.configurator.impl.model.Config;
import org.apache.felix.configurator.impl.model.ConfigPolicy;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.apache.felix.converter.impl.ConverterService;
import org.osgi.framework.Bundle;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.TypeReference;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class Util {

    private static final String INTERNAL_PREFIX = ":configurator:";

    private static final String PROP_VERSION = INTERNAL_PREFIX + "version";

    private static final String PROP_ENVIRONMENTS = "environments";

    private static final String PROP_RANKING = "ranking";

    private static final String PROP_POLICY = "policy";

    public static Converter getConverter() {
        return new ConverterService(); // TODO use OSGi service
    }

    public static BundleState readConfigurationsFromBundle(final Bundle bundle, final Set<String> paths) {
        final BundleState config = new BundleState();

        final List<ConfigurationFile> allFiles = new ArrayList<>();
        for(final String path : paths) {
            final List<ConfigurationFile> files = org.apache.felix.configurator.impl.yaml.Util.readYAML(bundle, path);
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

                final String contents = getResource(name, url);
                if ( contents != null ) {
                    final ConfigurationFile file = readYAML(name, url, bundle.getBundleId(), contents);
                    if ( file != null ) {
                        result.add(file);
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
     * @param name The name of the file
     * @param url The url to that file or {@code null}
     * @param bundleId The bundle id of the bundle containing the file
     * @param contents The contents of the file
     * @return The configuration file or {@code null}.
     */
    public static ConfigurationFile readYAML(final String name, final URL url, final long bundleId, final String contents) {
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
                                        environments = Util.getConverter().convert(value).defaultValue(null).to(new TypeReference<Set<String>>() {});
                                        if ( environments == null ) {
                                            SystemLogger.warning("Invalid environments for configuration in '" + identifier + "' : " + pid + " - " + value);
                                        }
                                    } else if ( key.equals(PROP_RANKING) ) {
                                        final Integer intObj = Util.getConverter().convert(value).defaultValue(null).to(Integer.class);
                                        if ( intObj == null ) {
                                            SystemLogger.warning("Invalid ranking for configuration in '" + identifier + "' : " + pid + " - " + value);
                                        } else {
                                            ranking = intObj.intValue();
                                        }
                                    } else if ( key.equals(PROP_POLICY) ) {
                                        final String stringVal = Util.getConverter().convert(value).defaultValue(null).to(String.class);
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
                                        properties.put(key, convert(value, typeInfo));
                                    } catch ( final IOException io ) {
                                        SystemLogger.error("Invalid type for configuration in '" + identifier + "' : " + pid + " - " + entry.getKey());
                                        valid = false;
                                        break;
                                    }
                                }
                            }

                            if ( valid ) {
                                final Config c = new Config(pid, environments, properties, bundleId, ranking, policy);
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

    public static Object convert(final Object value, final String typeInfo) throws IOException {
        // TODO - binary
        final String yaml = createYAML(value);
        if ( typeInfo == null ) {
            if ( value instanceof String || value instanceof Boolean ) {
                return value;
            } else if ( value instanceof Long || value instanceof Double ) {
                return value;
            } else if ( value instanceof Integer ) {
                return ((Integer)value).longValue();
            } else if ( value instanceof Float ) {
                return ((Float)value).doubleValue();
            }
            if ( value instanceof List ) {
                @SuppressWarnings("unchecked")
                final List<Object> list = (List<Object>)value;
                if ( list.isEmpty() ) {
                    return new String[0];
                }
                final Object firstObject = list.get(0);
                if ( firstObject instanceof String ) {
                    return getConverter().convert(list).defaultValue(yaml).to(String[].class);
                } else if ( firstObject instanceof Boolean ) {
                    return getConverter().convert(list).defaultValue(yaml).to(Boolean[].class);
                } else if ( firstObject instanceof Long || firstObject instanceof Integer ) {
                    return getConverter().convert(list).defaultValue(yaml).to(Long[].class);
                } else if ( firstObject instanceof Double || firstObject instanceof Float ) {
                    return getConverter().convert(list).defaultValue(yaml).to(Double[].class);
                }
            }
            return yaml;
        }

        // scalar types and primitive types
        if ( "String".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(String.class);

        } else if ( "Integer".equals(typeInfo) || "int".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Integer.class);

        } else if ( "Long".equals(typeInfo) || "long".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Long.class);

        } else if ( "Float".equals(typeInfo) || "float".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Float.class);

        } else if ( "Double".equals(typeInfo) || "double".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Double.class);

        } else if ( "Byte".equals(typeInfo) || "byte".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Byte.class);

        } else if ( "Short".equals(typeInfo) || "short".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Short.class);

        } else if ( "Character".equals(typeInfo) || "char".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Character.class);

        } else if ( "Boolean".equals(typeInfo) || "boolean".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Boolean.class);

        }

        // array of scalar types and primitive types
        if ( "String[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(String[].class);

        } else if ( "Integer[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Integer[].class);

        } else if ( "int[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(int[].class);

        } else if ( "Long[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Long[].class);

        } else if ( "long[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(long[].class);

        } else if ( "Float[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(Float[].class);

        } else if ( "float[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(float[].class);

        } else if ( "Double[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(Double[].class);

        } else if ( "double[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(double[].class);

        } else if ( "Byte[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Byte[].class);

        } else if ( "byte[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(byte[].class);

        } else if ( "Short[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(Short[].class);

        } else if ( "short[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(short[].class);

        } else if ( "Character[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Character[].class);

        } else if ( "char[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(char[].class);

        } else if ( "Boolean[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(Boolean[].class);

        } else if ( "boolean[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(boolean[].class);
        }

        // Collections of scalar types
        if ( "Collection<String>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<String>>() {});

        } else if ( "Collection<Integer>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Integer>>() {});

        } else if ( "Collection<Long>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Long>>() {});

        } else if ( "Collection<Float>".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Float>>() {});

        } else if ( "Collection<Double>".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Double>>() {});

        } else if ( "Collection<Byte>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Byte>>() {});

        } else if ( "Collection<Short>".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Short>>() {});

        } else if ( "Collection<Character>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Character>>() {});

        } else if ( "Collection<Boolean>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(yaml).to(new TypeReference<List<Boolean>>() {});
        }

        // unknown type - ignore configuration
        throw new IOException("Invalid type information: " + typeInfo);
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

            final int v = getConverter().convert(version).defaultValue(-1).to(Integer.class);
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

    /**
     * Read the contents of a resource, encoded as UTF-8
     * @param name The resource name
     * @param url The resource URL
     * @return The contents or {@code null}
     */
    public static String getResource(final String name, final URL url) {
        URLConnection connection = null;
        try {
            connection = url.openConnection();

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
        } catch ( final IOException ioe ) {
            SystemLogger.error("Unable to read " + name, ioe);
        }
        return null;
    }
}
