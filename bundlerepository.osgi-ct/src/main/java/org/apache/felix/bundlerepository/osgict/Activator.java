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
package org.apache.felix.bundlerepository.osgict;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This Activator implements the required glue between an OSGi Repository implementation and the
 * OSGi CT. It is needed to prime the repository with the data needed by the CT and works as
 * follows:
 * <ul>
 * <li>The CT registers a String service with as property {@code repository-xml}. This service is
 * literally the repository XML needed by the test, and must be fed to the repository implementation.
 * <li>When that's done this glue code registers another service with as property
 * {@code repository-populated} to signal to the CT that the priming is done.
 * </ul>
 */
public class Activator implements BundleActivator
{
    private BundleContext bundleContext;
    private ServiceTracker<String, String> repoXMLTracker;
    private ServiceTracker<RepositoryAdmin, RepositoryAdmin> repoTracker;

    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;
        Filter f = context.createFilter("(&(objectClass=java.lang.String)(repository-xml=*))");
        repoXMLTracker = new ServiceTracker<String, String>(context, f, null) {
            @Override
            public String addingService(ServiceReference<String> reference)
            {
                try
                {
                    String xml = super.addingService(reference);
                    handleRepositoryXML(reference, xml);
                    return xml;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        repoXMLTracker.open();
    }

    public void stop(BundleContext context) throws Exception
    {
        repoXMLTracker.close();
        if (repoTracker != null)
            repoTracker.close();
    }

    private void handleRepositoryXML(ServiceReference<String> reference, String xml) throws Exception
    {
        File tempXMLFile = bundleContext.getDataFile("repo-" + reference.getProperty("repository-xml") + ".xml");
        writeXMLToFile(tempXMLFile, xml);

        repoTracker = new ServiceTracker<RepositoryAdmin, RepositoryAdmin>(bundleContext, RepositoryAdmin.class, null);
        repoTracker.open();
        RepositoryAdmin repo = repoTracker.waitForService(30000);
        repo.addRepository(tempXMLFile.toURI().toURL());
        tempXMLFile.delete();

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("repository-populated", reference.getProperty("repository-xml"));
        bundleContext.registerService(String.class, "", props);
    }

    private void writeXMLToFile(File tempXMLFile, String xml) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(tempXMLFile);
        try
        {
            fos.write(xml.getBytes());
        }
        finally
        {
            fos.close();
        }
    }
}
