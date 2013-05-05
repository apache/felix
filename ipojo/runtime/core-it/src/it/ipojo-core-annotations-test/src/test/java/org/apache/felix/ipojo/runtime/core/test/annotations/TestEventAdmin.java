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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestEventAdmin extends Common {
    String type = "org.apache.felix.ipojo.runtime.core.test.components.event.PubSub";
    String deprecated = "org.apache.felix.ipojo.runtime.core.test.components.event.PubSubDeprecated";
    String publishes = "org.apache.felix.ipojo.runtime.core.test.components.event.PubSubWithPublishes";

    String namespace = "org.apache.felix.ipojo.handlers.event";

    Element component;
    Element componentDeprecated;
    Element componentWithPublishes;

    @Before
    public void setUp() {
        component = ipojoHelper.getMetadata(getTestBundle(),  type);
        componentDeprecated = ipojoHelper.getMetadata(getTestBundle(),  deprecated);
        componentWithPublishes = ipojoHelper.getMetadata(getTestBundle(),  publishes);
        assertNotNull("Check component", component);
        assertNotNull("Check deprecated", componentDeprecated);
        assertNotNull("Check publishes", componentWithPublishes);

    }

    @After
    public void tearDown() {
        component = null;
        componentDeprecated = null;
        componentWithPublishes = null;
    }

    @Test
    public void testP1() {
        //P1, synchronous
        Element elem = getElementByName("p1");
        checkPublisher(elem);
        assertNull("Check topics", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher1", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("dataKey"));
    }

    @Test
    public void testP1WithPublishes() {
        //P1, synchronous
        Element elem = getPublishesByName("p1");
        checkPublishes(elem);
        assertNull("Check topics", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher1", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("dataKey"));
    }

    @Test
    public void testP1Deprecated() {
        //P1, synchronous
        Element elem = getDeprecatedElementByName("p1");
        checkPublisher(elem);
        assertNull("Check topics", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher1", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("data_key"));
    }

    @Test
    public void testP2() {
        //name="p2", synchronous=false, topics="foo,bar", data_key="data"
        Element elem = getElementByName("p2");
        checkPublisher(elem);
        assertEquals("Check topics (" + elem.getAttribute("topics") + ")", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "false", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher2", elem.getAttribute("field"));
        assertEquals("Check data_key", "data", elem.getAttribute("dataKey"));
    }

    @Test
    public void testP2WithPublishes() {
        //name="p2", synchronous=false, topics="foo,bar", data_key="data"
        Element elem = getPublishesByName("p2");
        checkPublishes(elem);
        assertEquals("Check topics (" + elem.getAttribute("topics") + ")", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "false", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher2", elem.getAttribute("field"));
        assertEquals("Check data_key", "data", elem.getAttribute("dataKey"));
    }

    @Test
    public void testP2Deprecated() {
        //name="p2", synchronous=false, topics="foo,bar", data_key="data"
        Element elem = getDeprecatedElementByName("p2");
        checkPublisher(elem);
        assertEquals("Check topics (" + elem.getAttribute("topics") + ")", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "false", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher2", elem.getAttribute("field"));
        assertEquals("Check data_key", "data", elem.getAttribute("data_key"));
    }

    @Test
    public void testP3() {
        //name="p3", synchronous=true, topics="bar"
        Element elem = getElementByName("p3");
        checkPublisher(elem);
        assertEquals("Check topics", "bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher3", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("dataKey"));
    }

    @Test
    public void testWithPublishesP3() {
        //name="p3", synchronous=true, topics="bar"
        Element elem = getPublishesByName("p3");
        checkPublishes(elem);
        assertEquals("Check topics", "bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher3", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("dataKey"));
    }

    @Test
    public void testP3Deprecated() {
        //name="p3", synchronous=true, topics="bar"
        Element elem = getDeprecatedElementByName("p3");
        checkPublisher(elem);
        assertEquals("Check topics", "bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher3", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("data_key"));
    }

    @Test
    public void testS1() {
        //name="s1", data_key="data"
        Element elem = getElementByName("s1");
        checkSubscriber(elem);
        assertNull("Check topics", elem.getAttribute("topics"));
        assertEquals("Check method", "receive1", elem.getAttribute("method"));
        assertEquals("Check data_key", "data", elem.getAttribute("dataKey"));
        assertNull("Check data_type", elem.getAttribute("dataType"));
        assertNull("Check filter", elem.getAttribute("filter"));
    }

    @Test
    public void testS1Deprecated() {
        //name="s1", data_key="data"
        Element elem = getDeprecatedElementByName("s1");
        checkSubscriber(elem);
        assertNull("Check topics", elem.getAttribute("topics"));
        assertEquals("Check method", "receive1", elem.getAttribute("method"));
        assertEquals("Check data_key", "data", elem.getAttribute("data_key"));
        assertNull("Check data_type", elem.getAttribute("data_type"));
        assertNull("Check filter", elem.getAttribute("filter"));
    }

    @Test
    public void testS2() {
        //name="s2", topics="foo,bar", filter="(foo=true)"
        Element elem = getElementByName("s2");
        checkSubscriber(elem);
        assertEquals("Check topics", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check method", "receive2", elem.getAttribute("method"));
        assertNull("Check data_key", elem.getAttribute("dataKey"));
        assertNull("Check data_type", elem.getAttribute("dataType"));
        assertEquals("Check filter", "(foo=true)", elem.getAttribute("filter"));
    }

    @Test
    public void testS2Deprecated() {
        //name="s2", topics="foo,bar", filter="(foo=true)"
        Element elem = getDeprecatedElementByName("s2");
        checkSubscriber(elem);
        assertEquals("Check topics", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check method", "receive2", elem.getAttribute("method"));
        assertNull("Check data_key", elem.getAttribute("data_key"));
        assertNull("Check data_type", elem.getAttribute("data_type"));
        assertEquals("Check filter", "(foo=true)", elem.getAttribute("filter"));
    }

    @Test
    public void testS3() {
        //name="s3", topics="foo", data_key="data", data_type="java.lang.String"
        Element elem = getElementByName("s3");
        checkSubscriber(elem);
        assertEquals("Check topics", "foo", elem.getAttribute("topics"));
        assertEquals("Check method", "receive3", elem.getAttribute("method"));
        assertEquals("Check data_key", "data", elem.getAttribute("dataKey"));
        assertEquals("Check data_type", "java.lang.String", elem.getAttribute("dataType"));
        assertNull("Check filter", elem.getAttribute("filter"));
    }

    @Test
    public void testS3Deprecated() {
        //name="s3", topics="foo", data_key="data", data_type="java.lang.String"
        Element elem = getDeprecatedElementByName("s3");
        checkSubscriber(elem);
        assertEquals("Check topics", "foo", elem.getAttribute("topics"));
        assertEquals("Check method", "receive3", elem.getAttribute("method"));
        assertEquals("Check data_key", "data", elem.getAttribute("data_key"));
        assertEquals("Check data_type", "java.lang.String", elem.getAttribute("data_type"));
        assertNull("Check filter", elem.getAttribute("filter"));
    }


    public Element getElementByName(String name) {
        Element[] elems = component.getElements();
        for (int i = 0; i < elems.length; i++) {
            if (elems[i].containsAttribute("name") && elems[i].getAttribute("name").equals(name)) {
                return elems[i];
            }
        }
        return null;
    }

    public Element getPublishesByName(String name) {
        Element[] elems = componentWithPublishes.getElements();
        for (int i = 0; i < elems.length; i++) {
            if (elems[i].containsAttribute("name") && elems[i].getAttribute("name").equals(name)) {
                return elems[i];
            }
        }
        return null;
    }

    public Element getDeprecatedElementByName(String name) {
        Element[] elems = componentDeprecated.getElements();
        for (int i = 0; i < elems.length; i++) {
            if (elems[i].containsAttribute("name") && elems[i].getAttribute("name").equals(name)) {
                return elems[i];
            }
        }
        return null;
    }

    public void checkSubscriber(Element elem) {
        assertNotNull("Can't check subscriber : null element", elem);
        String ns = elem.getNameSpace();
        String nm = elem.getName();
        assertEquals("Elem is not a subscriber : bad namespace", namespace, ns);
        assertEquals("Elem is not a subscriber : bad name", "subscriber", nm);

    }

    public void checkPublisher(Element elem) {
        assertNotNull("Can't check publisher : null element", elem);
        String ns = elem.getNameSpace();
        String nm = elem.getName();
        assertEquals("Elem is not a publisher : bad namespace", namespace, ns);
        assertEquals("Elem is not a publisher : bad name", "publisher", nm);
    }

    public void checkPublishes(Element elem) {
        assertNotNull("Can't check publisher : null element", elem);
        String ns = elem.getNameSpace();
        String nm = elem.getName();
        assertEquals("Elem is not a publisher : bad namespace", namespace, ns);
        assertEquals("Elem is not a publisher : bad name", "publishes", nm);
    }

}
