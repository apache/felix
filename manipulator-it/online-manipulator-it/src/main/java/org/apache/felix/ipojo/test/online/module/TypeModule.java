/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.test.online.module;

import java.lang.annotation.Annotation;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.manipulator.spi.AnnotationLiteral;
import org.apache.felix.ipojo.manipulator.spi.AbsBindingModule;

/**
 * User: guillaume
 * Date: 27/02/2014
 * Time: 14:26
 */
public class TypeModule extends AbsBindingModule {
    @Override
    public void configure() {
        // @Type is equivalent to @Component
        bindStereotype(Type.class)
                .with(new ComponentLiteral());
    }

    public static class ComponentLiteral extends AnnotationLiteral<Component> implements Component {

        public boolean public_factory() {
            return true;
        }

        public boolean publicFactory() {
            return true;
        }

        public String name() {
            return "";
        }

        public boolean architecture() {
            return false;
        }

        public boolean immediate() {
            return false;
        }

        public boolean propagation() {
            return true;
        }

        public String managedservice() {
            return "";
        }

        public String factory_method() {
            return "";
        }

        public String factoryMethod() {
            return "";
        }

        public String version() {
            return "";
        }
    }
}
