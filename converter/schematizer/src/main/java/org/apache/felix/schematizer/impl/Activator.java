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
package org.apache.felix.schematizer.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.schematizer.Schematizer;
import org.apache.felix.serializer.impl.json.JsonSerializerImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.serializer.Serializer;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> jsonProps = new Hashtable<>();
        jsonProps.put("mimetype", new String[] {
                "application/json", "application/x-javascript", "text/javascript",
                "text/x-javascript", "text/x-json" });
        jsonProps.put("provider", "felix");
        context.registerService(Serializer.class, new JsonSerializerImpl(), jsonProps);

        context.registerService(Schematizer.class, new SchematizerImpl(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
