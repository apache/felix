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
package org.apache.felix.bundlerepository.impl;

import java.net.URL;
import java.util.Map;
import java.util.jar.Attributes;

import junit.framework.TestCase;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.manifest.Clause;
import org.osgi.framework.Constants;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DataModelHelperTest extends TestCase
{

    private DataModelHelper dmh = new DataModelHelperImpl();

    public void testResource() throws Exception
    {
        Attributes attr = new Attributes();
        attr.putValue("Manifest-Version", "1.0");
        attr.putValue("Bundle-Name", "Apache Felix Utils");
        attr.putValue("Bundle-Version", "0.1.0.SNAPSHOT");
        attr.putValue("Bundle-ManifestVersion", "2");
        attr.putValue("Bundle-License", "http://www.apache.org/licenses/LICENSE-2.0.txt");
        attr.putValue("Bundle-Description", "Utility classes for OSGi.");
        attr.putValue("Import-Package", "org.osgi.framework;version=\"[1.4,2)\"");
        attr.putValue("Bundle-SymbolicName", "org.apache.felix.utils");

        Resource resource = dmh.createResource(attr);

        String xml = dmh.writeResource(resource);
        System.out.println(xml);

        Resource resource2 = dmh.readResource(xml);
        String xml2 = dmh.writeResource(resource2);
        System.out.println(xml2);

        assertEquals(xml, xml2);
    }

    public void testRequirementFilter() throws Exception
    {
        RequirementImpl r = new RequirementImpl();
        r.setFilter("(&(package=foo.bar)(version>=0.0.0)(version<3.0.0))");
        assertEquals("(&(package=foo.bar)(!(version>=3.0.0)))", r.getFilter());

        r.setFilter("(&(package=javax.transaction)(partial=true)(mandatory:<*partial))");
        assertEquals("(&(package=javax.transaction)(partial=true)(mandatory:<*partial))", r.getFilter());
    }

    public void testCapabilities() throws Exception {
        Attributes attr = new Attributes();
        attr.putValue("Manifest-Version", "1.0");
        attr.putValue("Bundle-Name", "Apache Felix Utils");
        attr.putValue("Bundle-Version", "0.1.0.SNAPSHOT");
        attr.putValue("Bundle-ManifestVersion", "2");
        attr.putValue("Bundle-License", "http://www.apache.org/licenses/LICENSE-2.0.txt");
        attr.putValue("Bundle-Description", "Utility classes for OSGi.");
        attr.putValue("Import-Package", "org.osgi.framework;version=\"[1.4,2)\"");
        attr.putValue("Bundle-SymbolicName", "org.apache.felix.utils");
        attr.putValue("Provide-Capability", "osgi.extender;osgi.extender=\"osgi.component\";uses:=\"\n" +
                " org.osgi.service.component\";version:Version=\"1.3\",osgi.service;objectCl\n" +
                " ass:List<String>=\"org.osgi.service.component.runtime.ServiceComponentRu\n" +
                " ntime\";uses:=\"org.osgi.service.component.runtime\"");
        attr.putValue("Export-Package", "test.package;version=\"1.0.0\"");

        Resource resource = dmh.createResource(attr);

        assertEquals(4, resource.getCapabilities().length);

        Capability bundleCap = null;
        Capability osgiExtenderCap = null;
        Capability osgiServiceCap = null;
        Capability osgiPackageCap = null;

        for (Capability capability : resource.getCapabilities()) {
            if (capability.getName().equals("bundle")) {
                bundleCap = capability;
            } else if (capability.getName().equals("osgi.extender")) {
                osgiExtenderCap = capability;
            } else if (capability.getName().equals("service")) {
                osgiServiceCap = capability;
            } else if (capability.getName().equals("package")) {
                osgiPackageCap = capability;
            } else {
                osgiServiceCap = capability;
            }
        }

        assertNotNull(bundleCap);
        assertNotNull(osgiExtenderCap);
        assertNotNull(osgiServiceCap);
        assertNotNull(osgiPackageCap);

        assertEquals("osgi.extender", osgiExtenderCap.getName());
        assertEquals("osgi.component", osgiExtenderCap.getPropertiesAsMap().get("osgi.extender"));
        assertEquals("1.3.0", osgiExtenderCap.getPropertiesAsMap().get(Constants.VERSION_ATTRIBUTE).toString());

        assertEquals("service", osgiServiceCap.getName());

        assertEquals("package", osgiPackageCap.getName());
    }

    public void testGzipResource() throws Exception {
        URL urlArchive = getClass().getResource("/spec_repository.gz");
        assertNotNull("GZ archive was not found", urlArchive);
        Repository repository1 = dmh.repository(urlArchive);

        URL urlRepo = getClass().getResource("/spec_repository.xml");
        assertNotNull("Repository file was not found", urlRepo);
        Repository repository2 = dmh.repository(urlRepo);
        assertEquals(repository1.getName(), repository2.getName());
        assertEquals(repository1.getResources().length, repository2.getResources().length);
    }
}
