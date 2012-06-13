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
package org.apache.felix.scrplugin;

import java.util.List;


/**
 * The result of the generation.
 */
public class Result {

    /** The list of metatype files. */
    private List<String> metatypeFiles;

    /** The list of scr files. */
    private List<String> scrFiles;

    /**
     * Set the list of metatype files
     */
    public void setMetatypeFiles(List<String> metatypeFiles) {
        this.metatypeFiles = metatypeFiles;
    }

    /**
     * Set the list of scr files
     */
    public void setScrFiles(List<String> scrFiles) {
        this.scrFiles = scrFiles;
    }

    /**
     * Return a list of generated metatype files
     * @return A list of relative paths or <code>null</code>
     */
    public List<String> getMetatypeFiles() {
        return metatypeFiles;
    }

    /**
     * Return a list of generated scr files
     * @return A list of relative paths or <code>null</code>
     */
    public List<String> getScrFiles() {
        return scrFiles;
    }
}
