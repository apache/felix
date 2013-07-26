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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util;

import static java.lang.String.format;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Elements {

    /**
     * Build the {@link Element} object from the given descriptor.
     * It splits the annotation's classname in 2 parts (up to the last '.')
     * first part (package's name) becomes the Element's namespace, and second
     * part (class simple name) becomes the Element's name.
     * @param type annotation descriptor
     * @return the new element
     */
    public static Element buildElement(Type type) {
        String name = type.getClassName();
        int index = name.lastIndexOf('.');
        String local = name.substring(index + 1);
        String namespace = name.substring(0, index);
        return buildElement(namespace, local);
    }

    /**
     * Build an {@link Element} using the provided namespace and local name.
     */
    public static Element buildElement(final String namespace, final String name) {
        return new Element(name, namespace);
    }

    /**
     * Build an {@link Element} using the provided binding information.
     * Expected format is {@literal [namespace:]name} (eg: {@literal com.acme:foo} or {@literal foo} if namespace
     * by default -org.apache.felix.ipojo- has to be used).
     * Notice that the ':' character usage in namespace or name part may lead to unexpected results.
     * In that case, the @HandlerBinding(namespace = "urn:my:namespace", value = "foo:handler") usage is preferred.
     * @param binding the condensed element name
     * @return the new element
     */
    public static Element buildElement(String binding) {
        String[] split = binding.split(":");
        if (split.length == 1) {
            return buildElement("", binding);
        }
        if (split.length > 2) {
            throw new IllegalArgumentException(
                    format("@HandlerBinding(\"%s\") is invalid: only 1 ':' char is authorized, please" +
                           " use the @HandlerBinding(namespace=\"...\", value=\"...\") form instead.",
                           binding)
            );
        }
        return buildElement(split[0], split[1]);
    }

    /**
     * Return the Element named {@literal properties}, creates one if missing.
     * @param workbench source for search
     * @return the {@literal properties} Element (never null).
     */
    public static Element getPropertiesElement(ComponentWorkbench workbench) {
        Element properties = workbench.getIds().get("properties");
        if (properties == null) {
            properties = buildElement("", "properties");
            workbench.getIds().put("properties", properties);
            workbench.getElements().put(properties, null);
        }
        return properties;
    }
}
