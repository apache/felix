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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

/**
 * User: guillaume
 * Date: 27/06/13
 * Time: 13:37
 */
public class AnnotationType {
    private final Type type;
    private final List<Playback> m_playbacks = new ArrayList<Playback>();

    public AnnotationType(final Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public List<Playback> getPlaybacks() {
        return m_playbacks;
    }

    public void traverse(AnnotationDiscovery visitor) {
        for (Playback playback : m_playbacks) {
            playback.accept(visitor);
        }
    }

}
