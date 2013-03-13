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

package org.apache.felix.ipojo.manipulator.store.mapper;

import java.io.File;

import org.apache.felix.ipojo.manipulator.store.ResourceMapper;

/**
 * ResourceMapper mapping from and to system specific path..
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FileSystemResourceMapper implements ResourceMapper {

    private ResourceMapper m_delegate;
    private char m_separator;

    public FileSystemResourceMapper(ResourceMapper delegate) {
        this(delegate, File.separatorChar);
    }

    public FileSystemResourceMapper(ResourceMapper delegate, char separator) {
        m_delegate = delegate;
        m_separator = separator;
    }

    public String internalize(String name) {
        // transform as system path the result of the internalization operation
        return systemPath(m_delegate.internalize(name));
    }

    public String externalize(String name) {
        // normalize he path before giving it to the delegate mapper
        return m_delegate.externalize(normalizePath(name));
    }

    /**
     * Normalize the given path. Normalization simply replace any
     * File separator (system dependant) with {@literal '/'}.
     * @param path system path
     * @return normalized path
     */
    private String normalizePath(String path) {
        return path.replace(m_separator, '/');
    }

    /**
     * Return a system path from the given normalized path.
     * @param path normalized path
     * @return system path
     */
    private String systemPath(String path) {
        return path.replace('/', m_separator);
    }


}
