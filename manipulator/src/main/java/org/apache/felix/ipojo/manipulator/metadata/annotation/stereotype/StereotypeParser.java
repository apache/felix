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

package org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.replay.RootAnnotationRecorder;
import org.objectweb.asm.ClassReader;

/**
 * User: guillaume
 * Date: 30/05/13
 * Time: 12:42
 */
public class StereotypeParser {
    private boolean stereotype = false;
    private List<RootAnnotationRecorder> replays = new ArrayList<RootAnnotationRecorder>();

    public void read(byte[] resource) {
        ClassReader reader = new ClassReader(resource);
        reader.accept(new StereotypeVisitor(this), ClassReader.SKIP_CODE);
    }

    public boolean isStereotype() {
        return stereotype;
    }

    public void setStereotype(final boolean stereotype) {
        this.stereotype = stereotype;
    }

    public List<RootAnnotationRecorder> getRecorders() {
        return replays;
    }
}
