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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.discovery;

import org.apache.felix.ipojo.annotations.Ignore;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationDiscovery;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

/**
 * User: guillaume
 * Date: 09/07/13
 * Time: 14:52
 */
public class IgnoredDiscovery implements AnnotationDiscovery {

    public static final String IGNORE_DESCRIPTOR = Type.getType(Ignore.class).getDescriptor();

    private boolean m_ignore = false;

    public AnnotationVisitor visitAnnotation(final String desc) {
        if (IGNORE_DESCRIPTOR.equals(desc)) {
            m_ignore = true;
        }
        return null;
    }

    public boolean isIgnore() {
        return m_ignore;
    }
}
