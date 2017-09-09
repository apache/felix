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

import org.apache.felix.serializer.WriterFactory;
import org.apache.felix.serializer.impl.json.JsonWriterFactory;
import org.apache.felix.serializer.impl.yaml.YamlWriterFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class PrototypeWriterFactory<T extends WriterFactory> implements PrototypeServiceFactory<T> {

    @SuppressWarnings( "unchecked" )
    @Override
    public T getService(Bundle bundle, ServiceRegistration<T> registration) {
        String[] mimetype = (String[])registration.getReference().getProperty("mimetype");
        if (isYaml(mimetype))
            return (T)new YamlWriterFactory();
        else
            return (T)new JsonWriterFactory();
    }

    private boolean isYaml(String[] mimetype) {
        if (mimetype == null || mimetype.length == 0)
            return false;
        for (String entry : mimetype) {
            if ("application/yaml".equals(entry))
                return true;
        }

        return false;
    }
    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<T> registration, T service ) {
    }
}
