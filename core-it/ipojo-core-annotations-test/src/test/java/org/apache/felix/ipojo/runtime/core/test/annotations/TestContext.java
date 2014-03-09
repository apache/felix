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

package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Before;
import org.junit.Test;
import org.ow2.chameleon.testing.helpers.MetadataHelper;

import static junit.framework.Assert.assertEquals;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks the parsing of the @Context annotation.
 */
public class TestContext extends Common {

    private Element[] contexts;



    @Before
    public void setUp() {
        Element meta = MetadataHelper.getMetadata(getTestBundle(),
                        "org.apache.felix.ipojo.runtime.core.test.components.context.ComponentUsingContext");
        contexts = meta.getElements("context");
    }

    @Test
    public void testFieldInjection() {
        Element tested = null;
        for (Element element : contexts) {
            if (element.containsAttribute("field")) {
                tested = element;
            }
        }
        assertThat(tested).isNotNull();

        assertThat(tested.getAttribute("field")).isEqualToIgnoringCase("field");
        assertThat(tested.getAttribute("constructor-parameter")).isNull();
        assertThat(tested.getAttribute("method")).isNull();
        assertThat(tested.getAttribute("value")).isNull(); // Not set
    }

    @Test
    public void testConstructorInjection() {
        Element tested = null;
        for (Element element : contexts) {
            if (element.containsAttribute("constructor-parameter")) {
                tested = element;
            }
        }
        assertThat(tested).isNotNull();

        assertThat(tested.getAttribute("field")).isNull();
        assertThat(tested.getAttribute("method")).isNull();
        // 0 : Property, 1 context
        assertThat(tested.getAttribute("constructor-parameter")).isEqualToIgnoringCase("1");
        assertThat(tested.getAttribute("value")).isEqualToIgnoringCase("COMPONENT");
    }

    @Test
    public void testMethodInjection() {
        Element tested = null;
        for (Element element : contexts) {
            if (element.containsAttribute("method")) {
                tested = element;
            }
        }
        assertThat(tested).isNotNull();

        assertThat(tested.getAttribute("field")).isNull();
        assertThat(tested.getAttribute("parameter-index")).isNull();
        assertThat(tested.getAttribute("method")).isEqualToIgnoringCase("setContext");
        assertThat(tested.getAttribute("value")).isEqualToIgnoringCase("INSTANCE");
    }

}
