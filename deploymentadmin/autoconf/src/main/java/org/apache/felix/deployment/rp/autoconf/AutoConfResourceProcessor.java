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
package org.apache.felix.deployment.rp.autoconf;

import static org.osgi.service.deploymentadmin.spi.ResourceProcessorException.CODE_OTHER_ERROR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.DesignateObject;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

public class AutoConfResourceProcessor implements ResourceProcessor, EventHandler
{
    public static final String CONFIGURATION_ADMIN_FILTER_ATTRIBUTE = "filter";

    private static final String LOCATION_PREFIX = "osgi-dp:";
    /** FELIX-5169 - do not reference this constant from the Constants class in DA! */
    private static final String EVENTTOPIC_COMPLETE = "org/osgi/service/deployment/COMPLETE";

    // dependencies injected by Dependency Manager
    private volatile LogService m_log;
    private volatile MetaTypeService m_metaService;
    private volatile DependencyManager m_dm;
    // Locally managed
    private Component m_component;
    private PersistencyManager m_persistencyManager;

    private final Object m_lock; // protects the members below
    private final Map<String, List<AutoConfResource>> m_toBeInstalled;
    private final Map<String, List<AutoConfResource>> m_toBeDeleted;
    private final AtomicReference<DeploymentSession> m_sessionRef;
    private final List<ConfigurationAdminTask> m_configurationAdminTasks;
    private final List<PostCommitTask> m_postCommitTasks;

    public AutoConfResourceProcessor()
    {
        m_lock = new Object();
        m_sessionRef = new AtomicReference<DeploymentSession>();
        m_toBeInstalled = new HashMap<String, List<AutoConfResource>>();
        m_toBeDeleted = new HashMap<String, List<AutoConfResource>>();
        m_configurationAdminTasks = new ArrayList<ConfigurationAdminTask>();
        m_postCommitTasks = new ArrayList<PostCommitTask>();
    }

    /**
     * Called by Felix DM for the component created in {@link #commit()}.
     */
    public void addConfigurationAdmin(ServiceReference ref, ConfigurationAdmin ca)
    {
        m_log.log(LogService.LOG_DEBUG, "found configuration admin " + ref);

        List<ConfigurationAdminTask> configAdminTasks;
        synchronized (m_lock)
        {
            configAdminTasks = new ArrayList<ConfigurationAdminTask>(m_configurationAdminTasks);
        }

        for (ConfigurationAdminTask task : configAdminTasks)
        {
            try
            {
                Filter filter = task.getFilter();
                if ((filter == null) || (filter != null && filter.match(ref)))
                {
                    task.run(m_persistencyManager, ca);
                }
            }
            catch (Exception e)
            {
                m_log.log(LogService.LOG_ERROR, "Exception during configuration to " + ca + ". Trying to continue.", e);
            }
        }

        m_log.log(LogService.LOG_DEBUG, "found configuration admin " + ref + " done");
    }

    public void begin(DeploymentSession session)
    {
        m_log.log(LogService.LOG_DEBUG, "beginning session " + session);

        synchronized (m_lock)
        {
            DeploymentSession current = m_sessionRef.get();
            if (current != null)
            {
                throw new IllegalArgumentException("Trying to begin new deployment session while already in one.");
            }
            if (session == null)
            {
                throw new IllegalArgumentException("Trying to begin new deployment session with a null session.");
            }
            if (!m_toBeInstalled.isEmpty() || !m_toBeDeleted.isEmpty() || !m_configurationAdminTasks.isEmpty() || !m_postCommitTasks.isEmpty() || m_component != null)
            {
                throw new IllegalStateException("State not reset correctly at start of session.");
            }
            m_sessionRef.set(session);
        }
    }

    public void cancel()
    {
        m_log.log(LogService.LOG_DEBUG, "cancel");
        rollback();
    }

    public void commit()
    {
        m_log.log(LogService.LOG_DEBUG, "commit");

        Dictionary properties = new Properties();
        properties.put(EventConstants.EVENT_TOPIC, EVENTTOPIC_COMPLETE);
        m_component = m_dm.createComponent()
            .setInterface(EventHandler.class.getName(), properties)
            .setImplementation(this)
            .setCallbacks(null, null, null, null)
            .setAutoConfig(Component.class, false)
            .add(m_dm.createServiceDependency()
                    .setService(ConfigurationAdmin.class)
                    .setCallbacks("addConfigurationAdmin", null)
                    .setRequired(false)
        );
        m_dm.add(m_component);

        m_log.log(LogService.LOG_DEBUG, "commit done");
    }

    public void dropAllResources() throws ResourceProcessorException
    {
        m_log.log(LogService.LOG_DEBUG, "drop all resources");

        assertInDeploymentSession("Can not drop all resources without a Deployment Session");

        for (String name : m_persistencyManager.getResourceNames())
        {
            dropped(name);
        }

        m_log.log(LogService.LOG_DEBUG, "drop all resources done");
    }

    public void dropped(String name) throws ResourceProcessorException
    {
        m_log.log(LogService.LOG_DEBUG, "dropped " + name);

        assertInDeploymentSession("Can not drop resource without a Deployment Session");

        Map<String, List<AutoConfResource>> toBeDeleted;
        synchronized (m_lock)
        {
            toBeDeleted = new HashMap<String, List<AutoConfResource>>(m_toBeDeleted);
        }

        try
        {
            List<AutoConfResource> resources = m_persistencyManager.load(name);

            if (!toBeDeleted.containsKey(name))
            {
                toBeDeleted.put(name, new ArrayList());
            }
            toBeDeleted.get(name).addAll(resources);
        }
        catch (IOException ioe)
        {
            throw new ResourceProcessorException(CODE_OTHER_ERROR, "Unable to drop resource: " + name, ioe);
        }

        synchronized (m_lock)
        {
            m_toBeDeleted.putAll(toBeDeleted);
        }

        m_log.log(LogService.LOG_DEBUG, "dropped " + name + " done");
    }

    public void handleEvent(Event event)
    {
        // regardless of the outcome, we simply invoke postcommit
        postcommit();
    }

    public void postcommit()
    {
        m_log.log(LogService.LOG_DEBUG, "post commit");

        List<PostCommitTask> postCommitTasks;
        synchronized (m_lock)
        {
            postCommitTasks = new ArrayList<PostCommitTask>(m_postCommitTasks);
        }

        for (PostCommitTask task : postCommitTasks)
        {
            try
            {
                task.run(m_persistencyManager);
            }
            catch (Exception e)
            {
                m_log.log(LogService.LOG_ERROR, "Exception during post commit wrap-up. Trying to continue.", e);
            }
        }

        endSession();

        m_log.log(LogService.LOG_DEBUG, "post commit done");
    }

    public void prepare() throws ResourceProcessorException
    {
        m_log.log(LogService.LOG_DEBUG, "prepare");

        assertInDeploymentSession("Can not prepare resource without a Deployment Session");

        Map<String, List<AutoConfResource>> toBeDeleted;
        Map<String, List<AutoConfResource>> toBeInstalled;
        synchronized (m_lock)
        {
            toBeDeleted = new HashMap<String, List<AutoConfResource>>(m_toBeDeleted);
            toBeInstalled = new HashMap<String, List<AutoConfResource>>(m_toBeInstalled);
        }

        List<ConfigurationAdminTask> configAdminTasks = new ArrayList<ConfigurationAdminTask>();
        List<PostCommitTask> postCommitTasks = new ArrayList<PostCommitTask>();

        m_log.log(LogService.LOG_DEBUG, "prepare delete");
        // delete dropped resources
        for (Map.Entry<String, List<AutoConfResource>> entry : toBeDeleted.entrySet())
        {
            String name = entry.getKey();
            for (AutoConfResource resource : entry.getValue())
            {
                configAdminTasks.add(new DropResourceTask(resource));
            }
            postCommitTasks.add(new DeleteResourceTask(name));
        }

        m_log.log(LogService.LOG_DEBUG, "prepare install/update");
        // install new/updated resources
        for (Map.Entry<String, List<AutoConfResource>> entry : toBeInstalled.entrySet())
        {
            String name = entry.getKey();

            List<AutoConfResource> existingResources = null;
            try
            {
                existingResources = m_persistencyManager.load(name);
            }
            catch (IOException ioe)
            {
                throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Unable to read existing resources for resource " + name, ioe);
            }

            List<AutoConfResource> resources = entry.getValue();
            for (AutoConfResource resource : resources)
            {
                // When updating existing configurations, make sure that we delete the ones that have become obsolete... 
                if (existingResources != null)
                {
                    Iterator<AutoConfResource> iter = existingResources.iterator();
                    while (iter.hasNext())
                    {
                        AutoConfResource existing = iter.next();
                        if (existing.equalsTargetConfiguration(resource))
                        {
                            iter.remove();
                        }
                    }
                }

                configAdminTasks.add(new InstallOrUpdateResourceTask(resource));
            }
            // remove existing configurations that were not in the new version of the resource
            for (AutoConfResource existingResource : existingResources)
            {
                configAdminTasks.add(new DropResourceTask(existingResource));
            }

            postCommitTasks.add(new StoreResourceTask(name, resources));
        }

        synchronized (m_lock)
        {
            m_configurationAdminTasks.addAll(configAdminTasks);
            m_postCommitTasks.addAll(postCommitTasks);
        }

        m_log.log(LogService.LOG_DEBUG, "prepare done");
    }

    public void process(String name, InputStream stream) throws ResourceProcessorException
    {
        m_log.log(LogService.LOG_DEBUG, "processing " + name);

        // initial validation
        assertInDeploymentSession("Can not process resource without a Deployment Session");

        Map<String, List<AutoConfResource>> toBeInstalled;
        synchronized (m_lock)
        {
            toBeInstalled = new HashMap<String, List<AutoConfResource>>(m_toBeInstalled);
        }

        MetaData data = parseAutoConfResource(stream);
        // process resources
        Filter filter = getFilter(data);

        // add to session data
        if (!toBeInstalled.containsKey(name))
        {
            toBeInstalled.put(name, new ArrayList<AutoConfResource>());
        }

        List<Designate> designates = data.getDesignates();
        if (designates == null || designates.isEmpty())
        {
            // if there are no designates, there's nothing to process
            m_log.log(LogService.LOG_INFO, "No designates found in the resource, so there's nothing to process.");
            return;
        }

        Map<String, OCD> localOcds = data.getObjectClassDefinitions();
        if (localOcds == null)
        {
            localOcds = Collections.emptyMap();
        }

        for (Designate designate : designates)
        {
            // check object
            DesignateObject objectDef = designate.getObject();
            if (objectDef == null)
            {
                throw new ResourceProcessorException(CODE_OTHER_ERROR, "Designate Object child missing or invalid");
            }

            // check attributes
            if (objectDef.getAttributes() == null || objectDef.getAttributes().isEmpty())
            {
                throw new ResourceProcessorException(CODE_OTHER_ERROR, "Object Attributes child missing or invalid");
            }

            // check ocdRef
            String ocdRef = objectDef.getOcdRef();
            if (ocdRef == null || "".equals(ocdRef))
            {
                throw new ResourceProcessorException(CODE_OTHER_ERROR, "Object ocdRef attribute missing or invalid");
            }

            // determine OCD
            ObjectClassDefinition ocd = null;
            OCD localOcd = localOcds.get(ocdRef);
            // ask meta type service for matching OCD if no local OCD has been defined
            ocd = (localOcd != null) ? new ObjectClassDefinitionImpl(localOcd) : getMetaTypeOCD(data, designate);
            if (ocd == null)
            {
                throw new ResourceProcessorException(CODE_OTHER_ERROR, "No Object Class Definition found with id=" + ocdRef);
            }

            // determine configuration data based on the values and their type definition
            Dictionary dict = MetaTypeUtil.getProperties(designate, ocd);
            if (dict == null)
            {
                // designate does not match it's definition, but was marked optional, ignore it
                continue;
            }

            AutoConfResource resource = new AutoConfResource(name, designate.getPid(), designate.getFactoryPid(), designate.getBundleLocation(), designate.isMerge(), dict, filter);
            
            toBeInstalled.get(name).add(resource);
        }

        synchronized (m_lock)
        {
            m_toBeInstalled.putAll(toBeInstalled);
        }

        m_log.log(LogService.LOG_DEBUG, "processing " + name + " done");
    }

    public void rollback()
    {
        m_log.log(LogService.LOG_DEBUG, "rollback");

        Map<String, List<AutoConfResource>> toBeInstalled;
        synchronized (m_lock)
        {
            toBeInstalled = new HashMap<String, List<AutoConfResource>>(m_toBeInstalled);
        }

        for (Map.Entry<String, List<AutoConfResource>> entry : toBeInstalled.entrySet())
        {
            for (AutoConfResource resource : entry.getValue())
            {
                String name = resource.getName();
                try
                {
                    dropped(name);
                }
                catch (ResourceProcessorException e)
                {
                    m_log.log(LogService.LOG_ERROR, "Unable to roll back resource '" + name + "', reason: " + e.getMessage() + ", caused by: " + e.getCause().getMessage());
                }
                break;
            }
        }

        endSession();

        m_log.log(LogService.LOG_DEBUG, "rollback done");
    }

    /**
     * Called by Felix DM when starting this component.
     */
    public void start() throws IOException
    {
        File root = m_dm.getBundleContext().getDataFile("");
        if (root == null)
        {
            throw new IOException("No file system support");
        }
        m_persistencyManager = new PersistencyManager(root);
    }

    private void assertInDeploymentSession(String msg) throws ResourceProcessorException
    {
        synchronized (m_lock)
        {
            DeploymentSession current = m_sessionRef.get();
            if (current == null)
            {
                throw new ResourceProcessorException(CODE_OTHER_ERROR, msg);
            }
        }
    }

    private void endSession()
    {
        if (m_component != null)
        {
            m_dm.remove(m_component);
            m_component = null;
        }
        synchronized (m_lock)
        {
            m_toBeInstalled.clear();
            m_toBeDeleted.clear();
            m_postCommitTasks.clear();
            m_configurationAdminTasks.clear();
            m_sessionRef.set(null);
        }
    }

    private Bundle getBundle(String bundleLocation, boolean isFactory) throws ResourceProcessorException
    {
        Bundle bundle = null;
        if (!isFactory)
        {
            // singleton configuration, no foreign bundles allowed, use source deployment package to find specified bundle
            if (bundleLocation.startsWith(LOCATION_PREFIX))
            {
                DeploymentSession session = m_sessionRef.get();
                bundle = session.getSourceDeploymentPackage().getBundle(bundleLocation.substring(LOCATION_PREFIX.length()));
            }
        }
        else
        {
            // factory configuration, foreign bundles allowed, use bundle context to find the specified bundle
            Bundle[] bundles = m_dm.getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                String location = bundles[i].getLocation();
                if (bundleLocation.equals(location))
                {
                    bundle = bundles[i];
                    break;
                }
            }
        }
        return bundle;
    }

    private Filter getFilter(MetaData data) throws ResourceProcessorException
    {
        Map optionalAttributes = data.getOptionalAttributes();
        if (optionalAttributes != null)
        {
            try
            {
                return FrameworkUtil.createFilter((String) optionalAttributes.get(AutoConfResourceProcessor.CONFIGURATION_ADMIN_FILTER_ATTRIBUTE));
            }
            catch (InvalidSyntaxException e)
            {
                throw new ResourceProcessorException(CODE_OTHER_ERROR, "Unable to create filter!", e);
            }
        }
        return null;
    }

    /**
     * Determines the object class definition matching the specified designate.
     * 
     * @param data The meta data containing 'local' object class definitions.
     * @param designate The designate whose object class definition should be determined.
     * @return
     * @throws ResourceProcessorException
     */
    private ObjectClassDefinition getMetaTypeOCD(MetaData data, Designate designate) throws ResourceProcessorException
    {
        boolean isFactoryConfig = isFactoryConfig(designate);

        Bundle bundle = getBundle(designate.getBundleLocation(), isFactoryConfig);
        if (bundle == null)
        {
            return null;
        }

        MetaTypeInformation mti = m_metaService.getMetaTypeInformation(bundle);
        if (mti == null)
        {
            return null;
        }

        String pid = isFactoryConfig ? pid = designate.getFactoryPid() : designate.getPid();
        try
        {
            ObjectClassDefinition tempOcd = mti.getObjectClassDefinition(pid, null);
            // tempOcd will always have a value, if pid was not known IAE will be thrown
            String ocdRef = designate.getObject().getOcdRef();
            if (ocdRef.equals(tempOcd.getID()))
            {
                return tempOcd;
            }
        }
        catch (IllegalArgumentException iae)
        {
            // let null be returned
        }

        return null;
    }

    private boolean isFactoryConfig(Designate designate)
    {
        String factoryPid = designate.getFactoryPid();
        return (factoryPid != null && !"".equals(factoryPid));
    }

    private MetaData parseAutoConfResource(InputStream stream) throws ResourceProcessorException
    {
        MetaDataReader reader = new MetaDataReader();
        MetaData data = null;
        try
        {
            data = reader.parse(stream);
        }
        catch (IOException e)
        {
            throw new ResourceProcessorException(CODE_OTHER_ERROR, "Unable to process resource.", e);
        }
        if (data == null)
        {
            throw new ResourceProcessorException(CODE_OTHER_ERROR, "Supplied configuration is not conform the metatype xml specification.");
        }
        return data;
    }
}

interface ConfigurationAdminTask
{
    public Filter getFilter();

    public void run(PersistencyManager persistencyManager, ConfigurationAdmin configAdmin) throws Exception;
}

class DeleteResourceTask implements PostCommitTask
{
    private final String m_name;

    public DeleteResourceTask(String name)
    {
        m_name = name;
    }

    public void run(PersistencyManager manager) throws Exception
    {
        manager.delete(m_name);
    }
}

class DropResourceTask implements ConfigurationAdminTask
{
    private final AutoConfResource m_resource;

    public DropResourceTask(AutoConfResource resource)
    {
        m_resource = resource;
    }

    public Filter getFilter()
    {
        return m_resource.getFilter();
    }

    public void run(PersistencyManager persistencyManager, ConfigurationAdmin configAdmin) throws Exception
    {
        String pid;
        if (m_resource.isFactoryConfig())
        {
            pid = m_resource.getGeneratedPid();
        }
        else
        {
            pid = m_resource.getPid();
        }
        Configuration configuration = configAdmin.getConfiguration(pid, m_resource.getBundleLocation());
        configuration.delete();
    }
}

class InstallOrUpdateResourceTask implements ConfigurationAdminTask
{
    private final AutoConfResource m_resource;

    public InstallOrUpdateResourceTask(AutoConfResource resource)
    {
        m_resource = resource;
    }

    public Filter getFilter()
    {
        return m_resource.getFilter();
    }

    public void run(PersistencyManager persistencyManager, ConfigurationAdmin configAdmin) throws Exception
    {
        String name = m_resource.getName();
        Dictionary properties = m_resource.getProperties();
        String bundleLocation = m_resource.getBundleLocation();
        Configuration configuration = null;

        List existingResources = null;
        try
        {
            existingResources = persistencyManager.load(name);
        }
        catch (IOException ioe)
        {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Unable to read existing resources for resource " + name, ioe);
        }

        // update configuration
        if (m_resource.isFactoryConfig())
        {
            // check if this is an factory config instance update
            for (Iterator i = existingResources.iterator(); i.hasNext();)
            {
                AutoConfResource existingResource = (AutoConfResource) i.next();
                if (m_resource.equalsTargetConfiguration(existingResource))
                {
                    // existing instance found
                    configuration = configAdmin.getConfiguration(existingResource.getGeneratedPid(), bundleLocation);
                    existingResources.remove(existingResource);
                    break;
                }
            }
            if (configuration == null)
            {
                // no existing instance, create new
                configuration = configAdmin.createFactoryConfiguration(m_resource.getFactoryPid(), bundleLocation);
            }
            m_resource.setGeneratedPid(configuration.getPid());
        }
        else
        {
            for (Iterator i = existingResources.iterator(); i.hasNext();)
            {
                AutoConfResource existingResource = (AutoConfResource) i.next();
                if (m_resource.getPid().equals(existingResource.getPid()))
                {
                    // existing resource found
                    existingResources.remove(existingResource);
                    break;
                }
            }
            configuration = configAdmin.getConfiguration(m_resource.getPid(), bundleLocation);
            if (!bundleLocation.equals(configuration.getBundleLocation()))
            {
                // an existing configuration exists that is bound to a different location, which is not allowed
                throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                    "Existing configuration was bound to " + configuration.getBundleLocation() + " instead of " + bundleLocation);
            }
        }
        if (m_resource.isMerge())
        {
            Dictionary existingProperties = configuration.getProperties();
            if (existingProperties != null)
            {
                Enumeration keys = existingProperties.keys();
                while (keys.hasMoreElements())
                {
                    Object key = keys.nextElement();
                    properties.put(key, existingProperties.get(key));
                }
            }
        }
        configuration.update(properties);
    }
}

interface PostCommitTask
{
    public void run(PersistencyManager manager) throws Exception;
}

class StoreResourceTask implements PostCommitTask
{
    private final String m_name;
    private final List m_resources;

    public StoreResourceTask(String name, List resources)
    {
        m_name = name;
        m_resources = resources;
    }

    public void run(PersistencyManager manager) throws Exception
    {
        manager.store(m_name, m_resources);
    }
}