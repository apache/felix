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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.converter.impl.json.JsonCodecImpl;
import org.apache.felix.converter.impl.yaml.YamlCodecImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.converter.Codec;
import org.osgi.service.converter.Converter;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        context.registerService(Converter.class, new ConverterService(), null);

        Dictionary<String, Object> jsonProps = new Hashtable<>();
        jsonProps.put("osgi.codec.mimetype", new String[] {
                "application/json", "application/x-javascript", "text/javascript",
                "text/x-javascript", "text/x-json" });
        context.registerService(Codec.class, new JsonCodecImpl(), jsonProps);

        Dictionary<String, Object> yamlProps = new Hashtable<>();
        yamlProps.put("osgi.codec.mimetype", new String[] {
                "text/yaml", "text/x-yaml", "application/yaml",
                "application/x-yaml" });
        context.registerService(Codec.class, new YamlCodecImpl(), yamlProps);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
