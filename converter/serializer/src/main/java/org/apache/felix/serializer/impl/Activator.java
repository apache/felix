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
package org.apache.felix.serializer.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.serializer.Serializer;
import org.apache.felix.serializer.WriterFactory;
import org.apache.felix.serializer.impl.json.JsonSerializerImpl;
import org.apache.felix.serializer.impl.yaml.YamlSerializerImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    public static final String[] jsonArray = new String[] {
            "application/json", "application/x-javascript", "text/javascript",
            "text/x-javascript", "text/x-json" };
    public static final Set<String> jsonSet = new HashSet<>(Arrays.asList(jsonArray));

    public static final String[] yamlArray = new String[] {
            "text/yaml", "text/x-yaml", "application/yaml",
            "application/x-yaml" };
    public static final Set<String> yamlSet = new HashSet<>(Arrays.asList(yamlArray));

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public void start(BundleContext context) throws Exception {
        // JSON
        Dictionary<String, Object> jsonProps = new Hashtable<>();
        jsonProps.put("mimetype", jsonArray);
        context.registerService(
                new String[]{Serializer.class.getName(), Serializer.JsonSerializer.class.getName()}, 
                new JsonSerializerImpl(), 
                jsonProps);
        context.registerService(
                WriterFactory.JsonWriterFactory.class, 
                new PrototypeWriterFactory(), 
                jsonProps);

        // YAML
        Dictionary<String, Object> yamlProps = new Hashtable<>();
        yamlProps.put("mimetype", yamlArray);
        context.registerService(
                new String[]{Serializer.class.getName(), Serializer.YamlSerializer.class.getName()}, 
                new YamlSerializerImpl(), 
                yamlProps);
        context.registerService(
                WriterFactory.YamlWriterFactory.class, 
                new PrototypeWriterFactory(), 
                yamlProps);

        // Not-specified (default will be JSON)
        context.registerService(
                WriterFactory.class, 
                new PrototypeWriterFactory(), 
                null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
