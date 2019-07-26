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

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class K8SSecretsConfigurationPlugin implements ConfigurationPlugin {
    private static final String PREFIX = "$[";
    private static final String SUFFIX = "]";

    private static final String SECRET_PREFIX = PREFIX + "secret:";
    private static final Pattern SECRET_PATTERN = createPattern(SECRET_PREFIX);
    private static final String ENV_PREFIX = PREFIX + "env:";
    private static final Pattern ENV_PATTERN = createPattern(ENV_PREFIX);

    private static Pattern createPattern(String prefix) {
        return Pattern.compile("\\Q" + prefix + "\\E.+\\Q" + SUFFIX + "\\E");
    }

    private final File directory;

    K8SSecretsConfigurationPlugin(String dir) {
        directory = new File(dir);
        getLog().info("Configured directory for secrets: {}", dir);
    }

    private Logger getLog() {
        return Activator.LOG;
    }

    @Override
    public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            Object val = properties.get(key);
            if (val instanceof String) {
                String sv = (String) val;
                int idx = sv.indexOf(PREFIX);
                if (idx != -1) {
                    String varStart = sv.substring(idx);
                    Object pid = properties.get(Constants.SERVICE_PID);
                    Object newVal = null;
                    if (varStart.startsWith(SECRET_PREFIX)) {
                        newVal = replaceVariablesFromFile(key, sv, pid);
                    } else if (varStart.startsWith(ENV_PREFIX)) {
                        newVal = replaceVariablesFromEnvironment(key, sv, pid);
                    }

                    if (newVal != null)
                        properties.put(key, newVal);

                    getLog().info("Replaced value of configuration property '{}' for PID {}", key, pid);
                }
            }
        }
    }

    Object replaceVariablesFromEnvironment(final String key, final String value, final Object pid) {
        return replaceVariables(ENV_PREFIX, ENV_PATTERN, key, value, pid, n -> System.getenv(n));
    }

    Object replaceVariablesFromFile(final String key, final String value, final Object pid) {
        return replaceVariables(SECRET_PREFIX, SECRET_PATTERN, key, value, pid, n -> {
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
            final String key, final String value, final Object pid,
            final Function<String, String> valueSource) {
        final Matcher m = pattern.matcher(value);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String var = m.group();

            final int len = var.length();
            final String varName = var.substring(prefix.length(), len - SUFFIX.length());
            String replacement = valueSource.apply(varName);
            if (replacement != null)
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);

        return sb.toString();
    }
}
