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
package org.apache.felix.ipojo.runtime.bad.test;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestBadLFCCallback extends Common {

    private String clazz = "org.apache.felix.ipojo.runtime.bad.components.CallbackCheckService";
    private String type = "BAD-CallbackCheckService";
    private Element manipulation;
    private Properties props;

    @Before
    public void setUp() {
        manipulation = getManipulationForComponent();
        props = new Properties();
        props.put("instance.name", "BAD");
    }


    private Element getNothing() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));

        Element callback = new Element("callback", "");
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }

    private Element getNoTransition() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("method", "start"));
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }

    private Element getNoMethod() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("transition", "validate"));
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }

    private Element getBadMethod() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("transition", "validate"));
        callback.addAttribute(new Attribute("method", "start_")); // Missing method.
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }

    private Element getBadMethod2() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("transition", "invalidate"));
        callback.addAttribute(new Attribute("method", "stop_")); // Missing method.
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }

    private Element getBadTransition() {
        Element elem = new Element("component", "");
        elem.addAttribute(new Attribute("classname", clazz));

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("method", "start"));
        callback.addAttribute(new Attribute("transition", "validate_"));
        elem.addElement(callback);
        elem.addElement(manipulation);
        return elem;
    }

    private Element getManipulationForComponent() {
        String header = testedBundle.getHeaders().get("iPOJO-Components");
        Element elem = null;
        try {
            elem = ManifestMetadataParser.parseHeaderMetadata(header);
        } catch (ParseException e) {
            fail("Parse Exception when parsing iPOJO-Component");
        }

        assertNotNull("Check elem not null", elem);

        Element manip = getManipulationForComponent(elem, type);
        assertNotNull("Check manipulation metadata not null for " + type, manip);
        return manip;
    }

    private Element getManipulationForComponent(Element metadata, String comp_name) {
        Element[] comps = metadata.getElements("component");
        for (int i = 0; i < comps.length; i++) {
            if (comps[i].containsAttribute("factory") && comps[i].getAttribute("factory").equals(comp_name)) {
                return comps[i].getElements("manipulation")[0];
            }
            if (comps[i].containsAttribute("name") && comps[i].getAttribute("name").equals(comp_name)) {
                return comps[i].getElements("manipulation")[0];
            }
        }
        return null;
    }

    @Test
    public void testNothing() {
        try {
            ComponentFactory cf = new ComponentFactory(osgiHelper.getContext(), getNothing());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A lifecycle callback with a missing method and transition must be rejected " + cf);
        } catch (ConfigurationException e) {
            // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }

    @Test
    public void testNoTransition() {
        try {
            ComponentFactory cf = new ComponentFactory(osgiHelper.getContext(), getNoTransition());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A lifecycle callback with a missing transition must be rejected " + cf);
        } catch (ConfigurationException e) {
            // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }

    @Test
    public void testNoMethod() {
        try {
            ComponentFactory cf = new ComponentFactory(osgiHelper.getContext(), getNoMethod());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A lifecycle callback with a missing method must be rejected " + cf);
        } catch (ConfigurationException e) {
            // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }

    @Test
    public void testBadMethod() {
        try {
            ComponentFactory cf = new ComponentFactory(testedBundle.getBundleContext(), getBadMethod());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            if (ci.isStarted()) {
                fail("A lifecycle callback with a bad method must be rejected (instance stills valid)" + cf);
            }
            ci.dispose();
            cf.stop();
        } catch (ConfigurationException e) {
            //The check does not happen in the configure method.
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }

    @Test
    public void testBadMethod2() {
        try {
            ComponentFactory cf = new ComponentFactory(testedBundle.getBundleContext(), getBadMethod2());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.stop();
            if (ci.isStarted()) {
                fail("A lifecycle callback with a bad method must be rejected (instance stills valid)" + cf);
            }
            ci.dispose();
            cf.stop();
        } catch (ConfigurationException e) {
            //The check does not happen in the configure method.
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }

    @Test
    public void testBadTransition() {
        try {
            ComponentFactory cf = new ComponentFactory(osgiHelper.getContext(), getBadTransition());
            cf.start();
            ComponentInstance ci = cf.createComponentInstance(props);
            ci.dispose();
            cf.stop();
            fail("A lifecycle callback with a bad transition must be rejected " + cf);
        } catch (ConfigurationException e) {
            // OK
        } catch (UnacceptableConfiguration e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        } catch (MissingHandlerException e) {
            fail("Unexpected exception when creating an instance : " + e.getMessage());
        }
    }

}
