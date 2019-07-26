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
package org.apache.felix.configadmin.plugin.substitution;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class K8SSecretsConfigurationPlugin implements ConfigurationPlugin {
    private static final String PREFIX = "$[secret:";
    private static final String SUFFIX = "]";
    private static final Pattern SECRET_PATTERN =
            Pattern.compile("\\Q" + PREFIX + "\\E.+\\Q" + SUFFIX + "\\E");

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
                if (sv.contains(PREFIX)) {
                    try {
                        Object newVal = replaceVariables(sv);
                        properties.put(key, newVal);
                        getLog().info("Replaced value of configuration property '{}' for PID {}",
                                key, properties.get(Constants.SERVICE_PID));
                    } catch (IOException e) {
                        getLog().error("Problem replacing configuration property '{}' for PID {}",
                                key, properties.get(Constants.SERVICE_PID), e);
                    }
                }
            }
        }
    }

    Object replaceVariables(final Object value) throws IOException {
        if (!(value instanceof String)) {
            return value;
        }

        final String textWithVars = (String) value;

        final Matcher m = SECRET_PATTERN.matcher(textWithVars.toString());
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String var = m.group();

            final int len = var.length();
            final String fname = var.substring(PREFIX.length(), len - SUFFIX.length());
            if (fname.contains("..")) {
                getLog().error("Illegal secret location: " + fname + " Going up in the directory structure is not allowed");
                continue;
            }

            File file = new File(directory, fname);
            if (!file.isFile()) {
                getLog().warn("Cannot replace variable. Configured path is not a regular file: " + file);
                continue;
            }
            String replacement = new String(Files.readAllBytes(file.toPath())).trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);

        return sb.toString();
    }
}
