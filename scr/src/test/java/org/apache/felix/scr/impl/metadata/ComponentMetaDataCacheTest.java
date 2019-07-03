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
package org.apache.felix.scr.impl.metadata;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.metadata.MetadataStoreHelper.MetaDataReader;
import org.apache.felix.scr.impl.metadata.MetadataStoreHelper.MetaDataWriter;
import org.xmlpull.v1.XmlPullParserException;

public class ComponentMetaDataCacheTest extends ComponentBase
{

    public void testCachedComponentMetadata10() throws Exception
    {
        doTestCachedComponentMetadata("/components_10.xml", DSVersion.DS10);
    }

    public void testCachedComponentMetadata11() throws Exception
    {
        doTestCachedComponentMetadata("/components_11.xml", DSVersion.DS11);
    }

    public void testCachedComponentMetadata12() throws Exception
    {
        doTestCachedComponentMetadata("/components_12.xml", DSVersion.DS12);
    }

    public void testCachedComponentMetadata13() throws Exception
    {
        doTestCachedComponentMetadata("/components_13.xml", DSVersion.DS13);
    }

    public void testCachedComponentMetadata14() throws Exception
    {
        doTestCachedComponentMetadata("/components_14.xml", DSVersion.DS14);
    }

    private void doTestCachedComponentMetadata(String xmlFile, DSVersion version)
        throws IOException, XmlPullParserException, Exception
    {
        final List<?> metadataList = readMetadata(xmlFile);
        assertEquals("Component Descriptors size not as expected", 1,
            metadataList.size());
        final ComponentMetadata actualCM = (ComponentMetadata) metadataList.get(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(out);
        MetaDataWriter metaDataWriter = new MetaDataWriter();
        actualCM.validate();
        actualCM.store(dataOut, metaDataWriter);

        MetaDataReader metaDataReader = new MetaDataReader();
        DataInputStream dataIn = new DataInputStream(
            new ByteArrayInputStream(out.toByteArray()));
        ComponentMetadata cachedCM = ComponentMetadata.load(dataIn, metaDataReader);
        assertEquals("Expected version not found", version, cachedCM.getDSVersion());

        assertEqualsCM(actualCM, cachedCM);
    }

    private static void assertEqualsCM(ComponentMetadata actualCM,
        ComponentMetadata cachedCM)
    {
        assertEquals("DS version is not equal", actualCM.getDSVersion(),
            cachedCM.getDSVersion());
        assertEquals("Component name is not equal", actualCM.getName(),
            cachedCM.getName());
        assertEquals("Activate method name not equal", actualCM.getActivate(),
            cachedCM.getActivate());
        assertEquals("Activate method declaration not equal",
            actualCM.isActivateDeclared(), cachedCM.isActivateDeclared());
        assertEquals("Activate fields not equal", actualCM.getActivationFields(),
            cachedCM.getActivationFields());
        assertEquals("Deactivate method name not equal", actualCM.getDeactivate(),
            cachedCM.getDeactivate());
        assertEquals("Deactivate method declaration not equal",
            actualCM.isDeactivateDeclared(), cachedCM.isDeactivateDeclared());
        assertEquals("Modified method name not equal", actualCM.getModified(),
            cachedCM.getModified());
        assertEquals("Configuration Policy not equal", actualCM.getConfigurationPolicy(),
            cachedCM.getConfigurationPolicy());
        assertEquals("Configuration Pid not equal", actualCM.getConfigurationPid(),
            cachedCM.getConfigurationPid());
        assertEquals("Factory Identifier not equal", actualCM.getFactoryIdentifier(),
            cachedCM.getFactoryIdentifier());
        assertEquals("Factory check not equal", actualCM.isFactory(),
            cachedCM.isFactory());
        assertEquals("Activation policy flag not equal", actualCM.isImmediate(),
            cachedCM.isImmediate());
        assertEquals("Implementation Class Name not equal",
            actualCM.getImplementationClassName(), cachedCM.getImplementationClassName());
        assertEquals("Number of Init parameters not equal",
            actualCM.getNumberOfConstructorParameters(),
            cachedCM.getNumberOfConstructorParameters());
        checkServiceMetadata(actualCM, cachedCM);

        checkReferenceMetadata(actualCM, cachedCM);

        checkProperties(actualCM.getProperties(), cachedCM.getProperties());

        checkProperties(actualCM.getFactoryProperties(), cachedCM.getFactoryProperties());
    }

    private static void checkServiceMetadata(ComponentMetadata actualCM,
        ComponentMetadata cachedCM)
    {
        ServiceMetadata sm1 = actualCM.getServiceMetadata();
        ServiceMetadata sm2 = cachedCM.getServiceMetadata();
        if (sm1 != null && sm2 != null)
        {
            sm2.validate(cachedCM);
            assertEquals("Service scope not equal", sm1.getScope(), sm2.getScope());
            String[] implementedInterfaces1 = sm1.getProvides();
            String[] implementedInterfaces2 = sm2.getProvides();
            assertEquals("Service Provides length not equal",
                implementedInterfaces1.length, implementedInterfaces2.length);
            for (int i = 0; i < implementedInterfaces1.length; i++)
            {
                assertEquals("Implemented interfaces not same", implementedInterfaces1[i],
                    implementedInterfaces2[i]);
            }
        }
    }

    private static void checkReferenceMetadata(ComponentMetadata actualCM,
        ComponentMetadata cachedCM)
    {
        List<ReferenceMetadata> references1 = actualCM.getDependencies();
        List<ReferenceMetadata> references2 = cachedCM.getDependencies();
        assertEquals("Number of references not same", references1.size(),
            references2.size());
        for (int i = 0; i < references1.size(); i++)
        {
            ReferenceMetadata r1 = references1.get(i);
            ReferenceMetadata r2 = references2.get(i);
            r2.validate(cachedCM);
            assertEquals("Reference name not equal", r1.getName(), r2.getName());
            assertEquals("Reference interface not equal", r1.getInterface(),
                r2.getInterface());
            assertEquals("Reference cardinality not equal", r1.getCardinality(),
                r2.getCardinality());
            assertEquals("Reference policy not equal", r1.getPolicy(), r2.getPolicy());
            assertEquals("Reference policy option not equal", r1.getPolicyOption(),
                r2.getPolicyOption());
            assertEquals("Reference target not equal", r1.getTarget(), r2.getTarget());
            assertEquals("Reference target property name not equal",
                r1.getTargetPropertyName(), r2.getTargetPropertyName());
            assertEquals("Reference bind not equal", r1.getBind(), r2.getBind());
            assertEquals("Reference unbind not equal", r1.getUnbind(), r2.getUnbind());
            assertEquals("Reference updated not equal", r1.getUpdated(), r2.getUpdated());
            assertEquals("Reference field not equal", r1.getField(), r2.getField());
            assertEquals("Reference field option not equal", r1.getFieldOption(),
                r2.getFieldOption());
            assertEquals("Reference field collection type not equal",
                r1.getFieldCollectionType(), r2.getFieldCollectionType());
            assertEquals("Reference scope not equal", r1.getScope(), r2.getScope());
            assertEquals("Reference parameter index not equal", r1.getParameterIndex(),
                r2.getParameterIndex());
            assertEquals("Reference parameter collection type not equal",
                r1.getParameterCollectionType(), r2.getParameterCollectionType());
        }
    }

    private static void checkProperties(Map<String, Object> properties1,
        Map<String, Object> properties2)
    {
        assertEquals("Properties size not equal", properties1.size(), properties2.size());

        for (String key : properties1.keySet())
        {
            assertTrue("Properties not equal", properties2.containsKey(key));
            checkPropertyValues(properties1.get(key), properties2.get(key));
        }
    }

    private static void checkPropertyValues(Object value1, Object value2)
    {
        assertEquals("Property Values type not same", value1.getClass(),
            value2.getClass());
        if (value1.getClass().isArray())
        {
            if (value1 instanceof char[])
            {
                char[] v1 = (char[]) value1;
                char[] v2 = (char[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

            else if (value1 instanceof int[])
            {
                int[] v1 = (int[]) value1;
                int[] v2 = (int[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

            else if (value1 instanceof short[])
            {
                short[] v1 = (short[]) value1;
                short[] v2 = (short[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

            else if (value1 instanceof long[])
            {
                long[] v1 = (long[]) value1;
                long[] v2 = (long[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

            else if (value1 instanceof double[])
            {
                double[] v1 = (double[]) value1;
                double[] v2 = (double[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2, 0);
            }

            else if (value1 instanceof float[])
            {
                float[] v1 = (float[]) value1;
                float[] v2 = (float[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2, 0);
            }

            else if (value1 instanceof boolean[])
            {
                boolean[] v1 = (boolean[]) value1;
                boolean[] v2 = (boolean[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

            else if (value1 instanceof byte[])
            {
                byte[] v1 = (byte[]) value1;
                byte[] v2 = (byte[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

            else if (value1 instanceof String[])
            {
                String[] v1 = (String[]) value1;
                String[] v2 = (String[]) value2;
                assertEquals("Length of property values not equal", v1.length, v1.length);
                assertArrayEquals("Property Values not equal", v1, v2);
            }

        }
        else
        {
            assertEquals("Properties values not equal", value1, value2);
        }

    }
}
