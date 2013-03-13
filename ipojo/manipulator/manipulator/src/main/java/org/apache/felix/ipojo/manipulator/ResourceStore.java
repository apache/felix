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

package org.apache.felix.ipojo.manipulator;

import java.io.IOException;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Abstract input/output for the manipulation process.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ResourceStore {

    /**
     * Return the bytecode of the given class name.
     * @param path normalized resource path (format: {@literal org/objectweb/asm/Visitor.class})
     * @return the byte array representing the given class
     * @throws IOException if resource was not found
     */
    byte[] read(String path) throws IOException;

    /**
     * Browse all resources available in this store.
     * @param visitor is called for each available resource
     */
    void accept(ResourceVisitor visitor);

    /**
     * Notify the store that resource will be written.
     * @throws IOException if there was an error
     */
    void open() throws IOException;

    /**
     * Writes the given Element into this store.
     * Typically a store implementation will use this to build a Manifest.
     * @param metadata Element metadata to be inserted
     */
    void writeMetadata(Element metadata);

    /**
     * Notify the builder that a new resource has been built and should
     * be stored in the resulting bundle.
     * @param resourcePath resource name of the class (format: {@literal org/objectweb/asm/Visitor.class})
     * @param resource content of the resource
     * @throws IOException if there was an error storing the resource
     */
    void write(String resourcePath, byte[] resource) throws IOException;

    /**
     * Close the store: no methods will be called anymore on this instance.
     * @throws IOException if close failed
     */
    void close() throws IOException;


}
