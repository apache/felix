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

package org.apache.felix.ipojo.manipulator.metadata.annotation.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.replay.RootAnnotationRecorder;
import org.objectweb.asm.Type;

/**
 * User: guillaume
 * Date: 30/05/13
 * Time: 20:35
 */
public class AnnotationRegistry {

    /**
     * Contains the annotations definitions for the given Stereotype annotation.
     */
    private Map<Type, List<RootAnnotationRecorder>> stereotypes = new HashMap<Type, List<RootAnnotationRecorder>>();

    /**
     * Other annotations.
     */
    private List<Type> unbound = new ArrayList<Type>();

    public void addStereotype(Type type, List<RootAnnotationRecorder> recorders) {
        stereotypes.put(type, recorders);
    }

    public void addUnbound(Type type) {
        unbound.add(type);
    }

    public List<RootAnnotationRecorder> getRecorders(Type type) {
        List<RootAnnotationRecorder> recorders = stereotypes.get(type);
        if (recorders == null) {
            return Collections.emptyList();
        }
        return recorders;
    }

    public boolean isStereotype(Type type) {
        return stereotypes.get(type) != null;
    }

    public boolean isUnbound(Type type) {
        return unbound.contains(type);
    }

    public boolean isUnknown(Type type) {
        return !isStereotype(type) && !isUnbound(type);
    }
}
