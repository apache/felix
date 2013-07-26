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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationDiscovery;
import org.apache.felix.ipojo.manipulator.util.ChainedAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;

/**
 * User: guillaume
 * Date: 10/07/13
 * Time: 10:48
 */
public class ChainedAnnotationDiscovery implements AnnotationDiscovery {

    private List<AnnotationDiscovery> m_discoveries = new ArrayList<AnnotationDiscovery>();

    public List<AnnotationDiscovery> getDiscoveries() {
        return m_discoveries;
    }

    public AnnotationVisitor visitAnnotation(final String desc) {
        ChainedAnnotationVisitor chain = null;
        for (AnnotationDiscovery discovery : m_discoveries) {
            AnnotationVisitor visitor = discovery.visitAnnotation(desc);
            if (visitor != null) {
                if (chain == null) {
                    chain = new ChainedAnnotationVisitor();
                }
                chain.getVisitors().add(visitor);
            }
        }
        return chain;
    }


}
