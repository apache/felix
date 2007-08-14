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
package org.apache.felix.sandbox.scrplugin.om.metatype;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>Definitions</code>...
 *
 * Definitions is just a collection of {@link OCD}s and
 * {@link Designate}s.
 */
public class Definitions {

    /** The list of {@link OCD}s. */
    protected List ocds = new ArrayList();

    /**
     * Return the list of {@link OCD}s.
     */
    public List getOCDs() {
        return this.ocds;
    }

    /**
     * Set the list of {@link OCDs}s.
     */
    public void setOCDs(List c) {
        this.ocds = c;
    }
}
