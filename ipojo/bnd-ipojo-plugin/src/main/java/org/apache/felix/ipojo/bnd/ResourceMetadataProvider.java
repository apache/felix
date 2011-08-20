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
package org.apache.felix.ipojo.bnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import aQute.lib.osgi.Resource;
import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.StreamMetadataProvider;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code ResourceMetadataProvider} is ...
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceMetadataProvider implements MetadataProvider {

    private Resource resource;

    private List<Element> cached;

    private boolean validateUsingLocalSchemas = false;

    private Reporter reporter;

    public ResourceMetadataProvider(Resource resource, Reporter reporter) {
        this.resource = resource;
        this.reporter = reporter;
    }

    public void setValidateUsingLocalSchemas(boolean validateUsingLocalSchemas) {
        this.validateUsingLocalSchemas = validateUsingLocalSchemas;
    }

    public List<Element> getMetadatas() throws IOException {
        if (cached == null) {
            cached = new ArrayList<Element>();
            InputStream stream = null;
            try {
                stream = resource.openInputStream();
            } catch (Exception e) {
                reporter.error(e.getMessage());
                throw new IOException("Cannot read metadata");
            }
            StreamMetadataProvider provider = new StreamMetadataProvider(stream, reporter);
            provider.setValidateUsingLocalSchemas(validateUsingLocalSchemas);
            cached.addAll(provider.getMetadatas());
        }

        return cached;
    }
}
