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
package org.apache.felix.scr.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;

/**
 * The <code>ComponentMetaDataCacheIntegrationTest</code> tests if restart of
 * scr bundle causes component metadata to be reused from a cache which is
 * stored during stopping the scr bundle.
 * 
 */
@RunWith(PaxExam.class)
public class ComponentMetaDataCacheIntegrationTest extends ComponentTestBase
{
    static
    {
        // use different components
        descriptorFile = "/integration_test_component_metadata_cache.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".metadata.cache";
        CACHE_META_DATA = true;

        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void testCacheExists() throws Exception
    {
        Collection<ComponentDescriptionDTO> actualComponents = getComponentDescriptions(
            bundle);

        ServiceReference<ServiceComponentRuntime> ref = scrTracker.getServiceReference();
        Bundle scrBundle = ref.getBundle();
        File f = scrBundle.getDataFile("componentMetadataStore");
        assertFalse("Cache " + f.getAbsolutePath() + " should not exist", f.exists());
        scrBundle.stop();
        scrBundle.start();
        delay();
        assertTrue("Cache " + f.getAbsolutePath() + " does not exist", f.exists());

        Collection<ComponentDescriptionDTO> cachedComponents = getComponentDescriptions(
            bundle);
        assertComponentsEqual((List<ComponentDescriptionDTO>) actualComponents,
            (List<ComponentDescriptionDTO>) cachedComponents);
    }

    @Test
    public void testUpdateBundleDismissesCache() throws Exception
    {

        // Suppose actualBundle = BundleA, updatedBundle = BundleB

        Collection<ComponentDescriptionDTO> bundleAComponents = getComponentDescriptions(
            bundle);

        ServiceReference<ServiceComponentRuntime> ref = scrTracker.getServiceReference();
        Bundle scrBundle = ref.getBundle();
        File f = scrBundle.getDataFile("componentMetadataStore");
        assertFalse("Cache " + f.getAbsolutePath() + " should not exist", f.exists());
        scrBundle.stop();
        scrBundle.start();
        delay();
        assertTrue("Cache " + f.getAbsolutePath() + " does not exist", f.exists());

        String descFile = "/integration_test_component_metadata_cache2.xml";

        bundle.update(createBundleInputStream(descFile, COMPONENT_PACKAGE,
            "simplecomponent", "0.0.12"));

        Collection<ComponentDescriptionDTO> bundleBComponents = getComponentDescriptions(
            bundle);

        // testing the change in number of components when the bundle was updated - this verifies that the cache wasn't used 
        assertEquals("Expected number of components not found in updated bundle", 2,
            ((List<ComponentDescriptionDTO>) bundleBComponents).size());

        scrBundle.stop();
        scrBundle.start();

        Collection<ComponentDescriptionDTO> cachedBundleBComponents = getComponentDescriptions(
            bundle);

        assertComponentsEqual((List<ComponentDescriptionDTO>) bundleBComponents,
            (List<ComponentDescriptionDTO>) cachedBundleBComponents);

        scrBundle.stop();

        // updating bundle back to original
        bundle.update(createBundleInputStream(descriptorFile, COMPONENT_PACKAGE,
            "simplecomponent", "0.0.11"));

        scrBundle.start();

        Collection<ComponentDescriptionDTO> bundleCComponents = getComponentDescriptions(
            bundle);

        assertComponentsEqual((List<ComponentDescriptionDTO>) bundleCComponents,
            (List<ComponentDescriptionDTO>) bundleAComponents);
    }

    private void assertComponentsEqual(List<ComponentDescriptionDTO> actualComponents,
        List<ComponentDescriptionDTO> cachedComponents)
        throws InvocationTargetException, InterruptedException
    {
        assertEquals("Number of components not equal", actualComponents.size(),
            cachedComponents.size());
        for (int i = 0; i < actualComponents.size(); i++)
        {
            ComponentDescriptionDTO actualComponent = actualComponents.get(i);
            ComponentDescriptionDTO cachedComponent = cachedComponents.get(i);

            assertEquals("Component Name not equal", actualComponent.name,
                cachedComponent.name);
            assertEquals("Expecting components to be enabled",
                actualComponent.defaultEnabled, cachedComponent.defaultEnabled);

            ComponentConfigurationDTO actualCC = findComponentConfigurationByName(
                actualComponent.name, 0);
            ComponentConfigurationDTO cachedCC = findComponentConfigurationByName(
                cachedComponent.name, 0);
            assertEquals("Expecting component states to be equal", actualCC.state,
                cachedCC.state);

            checkSatisfiedReferences(actualCC.satisfiedReferences,
                cachedCC.satisfiedReferences);

            checkProperties(actualCC.properties, cachedCC.properties);

            if (actualCC.service != null && cachedCC.service != null)
            {
                assertEquals("Service Reference not same", actualCC.service.toString(),
                    cachedCC.service.toString());
            }
        }
    }

    private static void checkSatisfiedReferences(
        SatisfiedReferenceDTO[] satisfiedReferences1,
        SatisfiedReferenceDTO[] satisfiedReferences2)
    {
        assertEquals("Size of satisfied references not equal",
            satisfiedReferences1.length, satisfiedReferences2.length);
        for (int i = 0; i < satisfiedReferences1.length; i++)
        {
            assertEquals("Satisfied referneces name not equal",
                satisfiedReferences1[i].toString(), satisfiedReferences2[i].toString());
        }
    }

    private static void checkProperties(Map<String, Object> actualProperties,
        Map<String, Object> cachedProperties)
    {
        assertEquals("Properties size not equal", actualProperties.size(),
            cachedProperties.size());
        for (String property : actualProperties.keySet())
        {
            assertTrue("Properties not equal", cachedProperties.containsKey(property));
            checkPropertyValues(actualProperties.get(property),
                cachedProperties.get(property));
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
                for (int i = 0; i < v1.length; i++)
                {
                    assertEquals("Property Values not equal", v1[i], v2[i]);
                }
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
