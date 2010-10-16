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
package org.apache.felix.ipojo.test;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;

/**
 * Test the good behaviour of the EventAdminHandler.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MBeanTests extends OSGiTestCase {

    /**
     * Helper used to create test components.
     */
    private IPOJOHelper m_helper;

    /**
     * The MBean server used to access exposed MBeans.
     */
    private MBeanServer m_server;

    /**
     * Initialize test.
     */
    public void setUp() {
        m_helper = new IPOJOHelper(this);
        m_server = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Clean test.
     */
    public void tearDown() {
        m_helper.dispose();
    }

    /**
     * Test the MBean exposed by the simple component, defined without
     * annotations and with the brand new JMX handler syntax.
     */
    public void testMBeanWithoutAnnotations() throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ListenerNotFoundException, ReflectionException, MBeanException {
        // Create an instance of the component
        ComponentInstance componentInstance = m_helper
                .createComponentInstance("org.apache.felix.ipojo.test.component.SimpleManagedComponent");
        String instanceName = componentInstance.getInstanceName();
        ObjectName objectName = new ObjectName(
                "org.apache.felix.ipojo.test.component:type=org.apache.felix.ipojo.test.component.SimpleManagedComponent,instance="
                        + instanceName);
        doTest(objectName);
    }

    /**
     * Test the MBean exposed by the simple component, defined without
     * annotations and with the deprecated JMX handler syntax.
     */
    public void testMBeanWithoutAnnotationsDeprecated() throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ListenerNotFoundException, ReflectionException, MBeanException {
        // Create an instance of the component
        ComponentInstance componentInstance = m_helper
                .createComponentInstance("org.apache.felix.ipojo.test.component.SimpleManagedComponentDeprecated");
        String instanceName = componentInstance.getInstanceName();
        ObjectName objectName = new ObjectName(
                "org.apache.felix.ipojo.test.component:type=org.apache.felix.ipojo.test.component.SimpleManagedComponent,instance="
                        + instanceName);
        doTest(objectName);
    }


    /**
     * Test the MBean exposed by the simple component, defined with
     * annotations.
     */
    public void testMBeanWithAnnotations() throws MalformedObjectNameException, IntrospectionException,
            InstanceNotFoundException, ListenerNotFoundException,
            ReflectionException, MBeanException {
        // Create an instance of the component
        ComponentInstance componentInstance = m_helper
                .createComponentInstance("org.apache.felix.ipojo.test.component.SimpleManagedComponentAnnotated");
        String instanceName = componentInstance.getInstanceName();
        ObjectName objectName = new ObjectName(
                "org.apache.felix.ipojo.test.component:type=org.apache.felix.ipojo.test.component.SimpleManagedComponentAnnotated,instance="
                        + instanceName);
        doTest(objectName);
    }

    /**
     * Test the MBean exposed by the simple component, defined with
     * annotations.
     */
    public void testMBeanWithAnnotationsDeprecated() throws MalformedObjectNameException, IntrospectionException,
            InstanceNotFoundException, ListenerNotFoundException,
            ReflectionException, MBeanException {
        // Create an instance of the component
        ComponentInstance componentInstance = m_helper
                .createComponentInstance("org.apache.felix.ipojo.test.component.SimpleManagedComponentAnnotatedDeprecated");
        String instanceName = componentInstance.getInstanceName();
        ObjectName objectName = new ObjectName(
                "org.apache.felix.ipojo.test.component:type=org.apache.felix.ipojo.test.component.SimpleManagedComponentAnnotatedDeprecated,instance="
                        + instanceName);
        doTest(objectName);
    }

    /**
     * Utility method used to test the MBean with the given objectName.
     * @param objectName the objectName of the MBean to test.
     */
    private void doTest(ObjectName objectName) throws IntrospectionException, InstanceNotFoundException, ReflectionException, MBeanException, ListenerNotFoundException {

        // Get the MBean from the platform MBean server

        MBeanInfo mBeanInfo = m_server.getMBeanInfo(objectName);
        ObjectInstance objectInstance = m_server.getObjectInstance(objectName);
        assertNotNull(mBeanInfo);
        assertNotNull(objectInstance);
        // Check that the property is exposed
        MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
        assertEquals(1, attributes.length);
        MBeanAttributeInfo attribute = attributes[0];
        assertEquals("integer", attribute.getName());
        assertEquals("int", attribute.getType());
        assertTrue(attribute.isReadable());
        assertFalse(attribute.isWritable());
        // Check that both methods are exposed
        MBeanOperationInfo[] operations = mBeanInfo.getOperations();
        assertEquals(2, operations.length);
        MBeanOperationInfo getOperation;
        MBeanOperationInfo setOperation;
        // Order is not important
        if (operations[0].getName().equals("getIntegerValue")) {
            getOperation = operations[0];
            setOperation = operations[1];
        } else {
            setOperation = operations[0];
            getOperation = operations[1];
        }
        // Check the 'get' operation
        assertEquals("getIntegerValue", getOperation.getName());
        assertEquals("Get the value of the integer",
                getOperation.getDescription());
        assertEquals("int", getOperation.getReturnType());
        MBeanParameterInfo[] getOperationParams = getOperation.getSignature();
        assertEquals(0, getOperationParams.length);
        // Check the 'set' operation
        assertEquals("setIntegerValue", setOperation.getName());
        assertEquals("Set the value of the integer",
                setOperation.getDescription());
        assertEquals("int", setOperation.getReturnType());
        MBeanParameterInfo[] setOperationParams = setOperation.getSignature();
        assertEquals(1, setOperationParams.length);
        assertEquals("int", setOperationParams[0].getType());
        // Call the methods and test the result, also test notifications
        CustomNotificationListener listener = new CustomNotificationListener();
        m_server.addNotificationListener(objectName, listener, null, null);
        int value1 = 123;
        int value2 = 456;
        m_server.invoke(objectName, "setIntegerValue", new Object[] { value1 },
                new String[] { "int" });
        m_server.invoke(objectName, "setIntegerValue", new Object[] { value2 },
                new String[] { "int" });
        int result = (Integer) m_server.invoke(objectName, "getIntegerValue",
                new Object[0], new String[0]);
        assertEquals(value2, result);
        m_server.removeNotificationListener(objectName, listener, null, null);
        //assertEquals(2, listener.getCount());
    }

    /**
     * Custom listener used to count MBean notifications.
     * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
     */
    private class CustomNotificationListener implements NotificationListener {

        /**
         * Counter for the notifications.
         */
        private int m_counter = 0;

        /**
         * Notified !
         * @param notification the notification
         * @param handback ignored
         */
        public void handleNotification(Notification notification,
                Object handback) {
            m_counter++;
        }

        /**
         * Return the notification count of this listener.
         * @return the notification count of this listener.
         */
        public int getCount() {
            return m_counter;
        }
    }

}
