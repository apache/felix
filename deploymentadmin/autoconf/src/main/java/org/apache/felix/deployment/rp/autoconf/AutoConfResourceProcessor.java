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

<<<<<<< HEAD
=======
import static org.osgi.service.deploymentadmin.spi.ResourceProcessorException.CODE_OTHER_ERROR;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
<<<<<<< HEAD
import java.util.Arrays;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
<<<<<<< HEAD
import java.util.Hashtable;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
<<<<<<< HEAD
import java.util.Set;
import java.util.Vector;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.metatype.Attribute;
import org.apache.felix.metatype.Designate;
=======
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.DesignateObject;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.osgi.framework.Bundle;
<<<<<<< HEAD
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
=======
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
import org.osgi.service.metatype.AttributeDefinition;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

<<<<<<< HEAD
public class AutoConfResourceProcessor implements ResourceProcessor, EventHandler {
    private static final String LOCATION_PREFIX = "osgi-dp:";
    public static final String CONFIGURATION_ADMIN_FILTER_ATTRIBUTE = "filter";

	// dependencies injected by Dependency Manager
	private volatile LogService m_log;
	private volatile ConfigurationAdmin m_configAdmin;
	private volatile MetaTypeService m_metaService;
	private volatile BundleContext m_bc;
    private volatile DependencyManager m_dm;
    
	private Component m_component;
	
	private final Object LOCK = new Object(); // protects the members below

	private DeploymentSession m_session = null;
	private final Map m_toBeInstalled = new HashMap(); // Map<String, List<AutoConfResource>>
	private final Map m_toBeDeleted = new HashMap();
	
	private PersistencyManager m_persistencyManager;

	public void start() throws IOException {
		File root = m_bc.getDataFile("");
		if (root == null) {
			throw new IOException("No file system support");
		}
		m_persistencyManager = new PersistencyManager(root);
	}
	
    public void begin(DeploymentSession session) {
        m_log.log(LogService.LOG_DEBUG, "beginning session " + session);
        synchronized (LOCK) {
            if (m_session != null) {
                throw new IllegalArgumentException("Trying to begin new deployment session while already in one.");
            }
            if (session == null) {
                throw new IllegalArgumentException("Trying to begin new deployment session with a null session.");
            }
            if (m_toBeInstalled.size() > 0 || m_toBeDeleted.size() > 0 || m_configurationAdminTasks.size() > 0 || m_postCommitTasks.size() > 0 || m_component != null) {
                throw new IllegalStateException("State not reset correctly at start of session.");
            }
            m_session = session;
        }
    }
 
    public void process(String name, InputStream stream) throws ResourceProcessorException {
        m_log.log(LogService.LOG_DEBUG, "processing " + name);
        // initial validation
        synchronized (LOCK) {
            if (m_session == null) {
                throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
            }
        }
        MetaDataReader reader = new MetaDataReader();
        MetaData data = null;
        try {
            data = reader.parse(stream);
        }
        catch (IOException e) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Unable to process resource.", e);
        }
        if (data == null) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Supplied configuration is not conform the metatype xml specification.");
        }
        // process resources
        String filter = null;
        Map optionalAttributes = data.getOptionalAttributes();
        if (optionalAttributes != null) {
            filter = (String) optionalAttributes.get(AutoConfResourceProcessor.CONFIGURATION_ADMIN_FILTER_ATTRIBUTE);
        }
        // add to session data
        if (!m_toBeInstalled.containsKey(name)) {
            m_toBeInstalled.put(name, new ArrayList());
        }
        Map designates = data.getDesignates();
        if (designates == null) {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            // if there are no designates, there's nothing to process
            m_log.log(LogService.LOG_INFO, "No designates found in the resource, so there's nothing to process.");
            return;
        }
<<<<<<< HEAD
        Map localOcds = data.getObjectClassDefinitions();
        if (localOcds == null) {
            localOcds = Collections.EMPTY_MAP;
        }
        Iterator i = designates.keySet().iterator();
        while (i.hasNext()) {
            Designate designate = (Designate) designates.get(i.next());
            
            // check object
            if (designate.getObject() == null) {
                throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Designate Object child missing or invalid");
            }
            
            // check attributes
            if (designate.getObject().getAttributes() == null || designate.getObject().getAttributes().size() == 0) {
                throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Object Attributes child missing or invalid");
            }
            
            // check ocdRef
            String ocdRef = designate.getObject().getOcdRef();
            if (ocdRef == null || "".equals(ocdRef)) {
                throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Object ocdRef attribute missing or invalid");
=======

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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            // determine OCD
            ObjectClassDefinition ocd = null;
<<<<<<< HEAD
            OCD localOcd = (OCD) localOcds.get(ocdRef);
            // ask meta type service for matching OCD if no local OCD has been defined
            ocd = (localOcd != null) ? new ObjectClassDefinitionImpl(localOcd) : getMetaTypeOCD(data, designate);
            if (ocd == null) {
                throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "No Object Class Definition found with id=" + ocdRef);
            }
            // determine configuration data based on the values and their type definition
            Dictionary dict = getProperties(designate, ocd);
            if (dict == null) {
                // designate does not match it's definition, but was marked optional, ignore it
                continue;
            }
            List resources = (List) m_toBeInstalled.get(name);
            resources.add(new AutoConfResource(name, designate.getPid(), designate.getFactoryPid(), designate.getBundleLocation(), designate.isMerge(), dict, filter));
        }
        m_log.log(LogService.LOG_DEBUG, "processing " + name + " done");
    }

    public void dropped(String name) throws ResourceProcessorException {
        m_log.log(LogService.LOG_DEBUG, "dropped " + name);
        synchronized (LOCK) {
        	if (m_session == null) {
        		throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
        	}
        }
    	try {
    		List resources = m_persistencyManager.load(name);
    		if (!m_toBeDeleted.containsKey(name)) {
    			m_toBeDeleted.put(name, new ArrayList());
    		}
    		((List) m_toBeDeleted.get(name)).addAll(resources);
    	}
    	catch (IOException ioe) {
    		throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Unable to drop resource: " + name, ioe);
    	}
        m_log.log(LogService.LOG_DEBUG, "dropped " + name + " done");
    }

    public void dropAllResources() throws ResourceProcessorException {
        m_log.log(LogService.LOG_DEBUG, "drop all resources");
        synchronized (LOCK) {
        	if (m_session == null) {
        		throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not drop all resources without a Deployment Session");
        	}
        }

    	File basedir = m_bc.getDataFile("");
    	if (basedir != null && basedir.isDirectory()) {
    		String[] files = basedir.list();
    		for (int i = 0; i < files.length; i++) {
    			dropped(files[i]);
    		}
    	}
    	else {
    		throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Unable to drop resources, data area is not accessible");
    	}
        m_log.log(LogService.LOG_DEBUG, "drop all resources done");
    }
    
    private List m_configurationAdminTasks = new ArrayList();
    private List m_postCommitTasks = new ArrayList();

    public void prepare() throws ResourceProcessorException {
        m_log.log(LogService.LOG_DEBUG, "prepare");
        synchronized (LOCK) {
        	if (m_session == null) {
        		throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
        	}
        }
    	try {
            m_log.log(LogService.LOG_DEBUG, "prepare delete");
    		// delete dropped resources
    		for (Iterator i = m_toBeDeleted.keySet().iterator(); i.hasNext();) {
    			String name = (String) i.next();
    			List resources = (List) m_toBeDeleted.get(name);
    			for (Iterator j = resources.iterator(); j.hasNext();) {
    				AutoConfResource resource = (AutoConfResource) j.next();
    				m_configurationAdminTasks.add(new DropResourceTask(resource));
    			}
    			m_postCommitTasks.add(new DeleteResourceTask(name));
    		}

            m_log.log(LogService.LOG_DEBUG, "prepare install/update");
    		// install new/updated resources
    		for (Iterator j = m_toBeInstalled.keySet().iterator(); j.hasNext();) {
    			String name = (String) j.next();
    			List existingResources = null;
    			try {
    				existingResources = m_persistencyManager.load(name);
    			}
    			catch (IOException ioe) {
    				throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Unable to read existing resources for resource " + name, ioe);
    			}
    			List resources = (List) m_toBeInstalled.get(name);
    			for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
    				AutoConfResource resource = (AutoConfResource) iterator.next();
    				m_configurationAdminTasks.add(new InstallOrUpdateResourceTask(resource));
    			}
    			// remove existing configurations that were not in the new version of the resource
    			for (Iterator i = existingResources.iterator(); i.hasNext();) {
    				AutoConfResource existingResource = (AutoConfResource) i.next();
    				Configuration configuration = null;
    				if (existingResource.isFactoryConfig()) {
    					configuration = m_configAdmin.getConfiguration(existingResource.getGeneratedPid(), existingResource.getBundleLocation());
    				} else {
    					configuration = m_configAdmin.getConfiguration(existingResource.getPid(), existingResource.getBundleLocation());
    				}
    				configuration.delete();
    			}
    			m_postCommitTasks.add(new StoreResourceTask(name, resources));
    		}
    	}
    	catch (IOException ioe) {
    		m_toBeInstalled.clear();
    		throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Unable to prepare for commit for resource", ioe);
    	}
        m_log.log(LogService.LOG_DEBUG, "prepare done");
    }

    public synchronized void commit() {
        m_log.log(LogService.LOG_DEBUG, "commit");

        Dictionary properties = new Properties();
        properties.put(EventConstants.EVENT_TOPIC, org.apache.felix.deploymentadmin.Constants.EVENTTOPIC_COMPLETE);
        m_component = m_dm.createComponent()
            .setInterface(EventHandler.class.getName(), properties)
            .setImplementation(this)
            .setCallbacks(null, null, null, null)
            .setAutoConfig(Component.class, false)
            .add(m_dm.createServiceDependency()
                .setService(ConfigurationAdmin.class)
                .setCallbacks("addConfigurationAdmin", null)
                .setRequired(false));
        m_dm.add(m_component);

        m_log.log(LogService.LOG_DEBUG, "commit done");
    }
    
    public void addConfigurationAdmin(ServiceReference ref, ConfigurationAdmin ca) {
        m_log.log(LogService.LOG_DEBUG, "found configuration admin " + ref);
        Iterator iterator = m_configurationAdminTasks.iterator();
        while (iterator.hasNext()) {
            ConfigurationAdminTask task = (ConfigurationAdminTask) iterator.next();
            try {
                Filter filter = null;
                String filterString = task.getFilter();
                if (filterString != null) {
                    try {
                        filter = m_bc.createFilter(filterString);
                    }
                    catch (InvalidSyntaxException e) {
                        m_log.log(LogService.LOG_ERROR, "Could not parse filter, ignoring it: " + filterString, e);
                    }
                }
                if (filter == null || filter != null && filter.match(ref)) {
                    task.run(m_persistencyManager, ca);
                }
            }
            catch (Exception e) {
                m_log.log(LogService.LOG_ERROR, "Exception during configuration to " + ca + ". Trying to continue.", e);
            }
        }
        m_log.log(LogService.LOG_DEBUG, "found configuration admin " + ref + " done");
    }
    
    public void postcommit() {
        m_log.log(LogService.LOG_DEBUG, "post commit");
        Iterator iterator = m_postCommitTasks.iterator();
        while (iterator.hasNext()) {
            PostCommitTask task = (PostCommitTask) iterator.next();
            try {
                task.run(m_persistencyManager);
            }
            catch (Exception e) {
                m_log.log(LogService.LOG_ERROR, "Exception during post commit wrap-up. Trying to continue.", e);
            }
        }
        endSession();
        m_log.log(LogService.LOG_DEBUG, "post commit done");
    }

    private void endSession() {
        if (m_component != null) {
        	m_dm.remove(m_component);
            m_component = null;
        }
        m_toBeInstalled.clear();
        m_toBeDeleted.clear();
        m_postCommitTasks.clear();
        m_configurationAdminTasks.clear();
        m_session = null;
    }

    public void rollback() {
        m_log.log(LogService.LOG_DEBUG, "rollback");
    	Set keys = m_toBeInstalled.keySet();
    	for (Iterator i = keys.iterator(); i.hasNext();) {
    		List configs = (List) m_toBeInstalled.get(i.next());
    		for (Iterator j = configs.iterator(); j.hasNext();) {
    			AutoConfResource resource = (AutoConfResource) j.next();
    			String name = resource.getName();
    			try {
    				dropped(name);
    			}
    			catch (ResourceProcessorException e) {
    				m_log.log(LogService.LOG_ERROR, "Unable to roll back resource '" + name + "', reason: " + e.getMessage() + ", caused by: " + e.getCause().getMessage());
    			}
    			break;
    		}
    	}
    	endSession();
        m_log.log(LogService.LOG_DEBUG, "rollback done");
    }

    public void cancel() {
        m_log.log(LogService.LOG_DEBUG, "cancel");
    	rollback();
    }

    /**
     * Determines the actual configuration data based on the specified designate and object class definition
     * 
     * @param designate The designate object containing the values for the properties
     * @param ocd The object class definition
     * @return A dictionary containing data as described in the designate and ocd objects, or <code>null</code> if the designate does not match it's
     * definition and the designate was marked as optional.
     * @throws ResourceProcessorException If the designate does not match the ocd and the designate is not marked as optional.
     */
    private Dictionary getProperties(Designate designate, ObjectClassDefinition ocd) throws ResourceProcessorException {
    	Dictionary properties = new Hashtable();
    	AttributeDefinition[] attributeDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
    	List attributes = designate.getObject().getAttributes();

    	for (Iterator i = attributes.iterator(); i.hasNext();) {
    		Attribute attribute = (Attribute) i.next();

    		String adRef = attribute.getAdRef();
    		boolean found = false;
    		for(int j = 0; j < attributeDefs.length; j++) {
    			AttributeDefinition ad = attributeDefs[j];
    			if (adRef.equals(ad.getID())) {
    				// found attribute definition
    				Object value = getValue(attribute, ad);
    				if (value == null) {
    					if (designate.isOptional()) {
    						properties = null;
    						break;
    					}
    					else {
    						throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Could not match attribute to it's definition: adref=" + adRef);
    					}
    				}
    				properties.put(adRef, value);
    				found = true;
    				break;
    			}
    		}
    		if (!found) {
    			if (designate.isOptional()) {
    				properties = null;
    				break;
    			} else {
    				throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Could not find attribute definition: adref=" + adRef);
    			}
    		}
    	}

    	return properties;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * Determines the object class definition matching the specified designate.
     * 
     * @param data The meta data containing 'local' object class definitions.
     * @param designate The designate whose object class definition should be determined.
     * @return
     * @throws ResourceProcessorException
     */
<<<<<<< HEAD
    private ObjectClassDefinition getMetaTypeOCD(MetaData data, Designate designate) throws ResourceProcessorException {
    	ObjectClassDefinition ocd = null;
    	String ocdRef = designate.getObject().getOcdRef();
    	Bundle bundle = getBundle(designate.getBundleLocation(), isFactoryConfig(designate));
    	if (bundle != null) {
    		MetaTypeInformation mti = m_metaService.getMetaTypeInformation(bundle);
    		if (mti != null) {
    			String pid = isFactoryConfig(designate) ? pid = designate.getFactoryPid() : designate.getPid();
    			try {
    				ObjectClassDefinition tempOcd = mti.getObjectClassDefinition(pid, null);
    				// tempOcd will always have a value, if pid was not known IAE will be thrown
    				if (ocdRef.equals(tempOcd.getID())) {
    					ocd = tempOcd;
    				}
    			}
    			catch (IllegalArgumentException iae) {
    				// let null be returned
    			}
    		}
    	}
    	return ocd;
    }

    private boolean isFactoryConfig(Designate designate) {
    	String factoryPid = designate.getFactoryPid();
    	return (factoryPid != null && !"".equals(factoryPid));
    }

    private Bundle getBundle(String bundleLocation, boolean isFactory) throws ResourceProcessorException {
    	Bundle bundle = null;
    	if (!isFactory) {
    		// singleton configuration, no foreign bundles allowed, use source deployment package to find specified bundle
    		if (bundleLocation.startsWith(LOCATION_PREFIX)) {
    			bundle = m_session.getSourceDeploymentPackage().getBundle(bundleLocation.substring(LOCATION_PREFIX.length()));
    		}
    	}
    	else {
    		// factory configuration, foreign bundles allowed, use bundle context to find the specified bundle
    		Bundle[] bundles = m_bc.getBundles();                                                                               
    		for (int i = 0; i < bundles.length; i++) {
    			String location = bundles[i].getLocation();
    			if (bundleLocation.equals(location)) {
    				bundle = bundles[i];
    				break;
    			}
    		}
    	}
    	return bundle;
    }

    /**
     * Determines the value of an attribute based on an attribute definition
     * 
     * @param attribute The attribute containing value(s)
     * @param ad The attribute definition
     * @return An <code>Object</code> reflecting what was specified in the attribute and it's definition or <code>null</code> if
     * the value did not match it's definition.
     */
    private Object getValue(Attribute attribute, AttributeDefinition ad) {
    	if (attribute == null || ad == null || !attribute.getAdRef().equals(ad.getID())) {
    		// wrong attribute or definition
    		return null;
    	}
    	String[] content = attribute.getContent();

    	// verify correct type of the value(s)
    	int type = ad.getType();
    	Object[] typedContent = null;
    	try {
    		for (int i = 0; i < content.length; i++) {
    			String value = content[i];
    			switch (type) {
	    			case AttributeDefinition.BOOLEAN:
	    				typedContent = (typedContent == null) ? new Boolean[content.length] : typedContent;
	    				typedContent[i] = Boolean.valueOf(value);	
	    				break;
	    			case AttributeDefinition.BYTE:
	    				typedContent = (typedContent == null) ? new Byte[content.length] : typedContent;
	    				typedContent[i] = Byte.valueOf(value);
	    				break;
	    			case AttributeDefinition.CHARACTER:
	    				typedContent = (typedContent == null) ? new Character[content.length] : typedContent;
	    				char[] charArray = value.toCharArray();
	    				if (charArray.length == 1) {
	    					typedContent[i] = new Character(charArray[0]);
	    				}
	    				else {
	    					return null;
	    				}
	    				break;
	    			case AttributeDefinition.DOUBLE:
	    				typedContent = (typedContent == null) ? new Double[content.length] : typedContent;
	    				typedContent[i] = Double.valueOf(value);
	    				break;
	    			case AttributeDefinition.FLOAT:
	    				typedContent = (typedContent == null) ? new Float[content.length] : typedContent;
	    				typedContent[i] = Float.valueOf(value);
	    				break;
	    			case AttributeDefinition.INTEGER:
	    				typedContent = (typedContent == null) ? new Integer[content.length] : typedContent;
	    				typedContent[i] = Integer.valueOf(value);
	    				break;
	    			case AttributeDefinition.LONG:
	    				typedContent = (typedContent == null) ? new Long[content.length] : typedContent;
	    				typedContent[i] = Long.valueOf(value);
	    				break;
	    			case AttributeDefinition.SHORT:
	    				typedContent = (typedContent == null) ? new Short[content.length] : typedContent;
	    				typedContent[i] = Short.valueOf(value);
	    				break;
	    			case AttributeDefinition.STRING:
	    				typedContent = (typedContent == null) ? new String[content.length] : typedContent;
	    				typedContent[i] = value;
	    				break;
	    			default:
	    				// unsupported type
	    				return null;
    			}
    		}
    	}
    	catch (NumberFormatException nfe) {
    		return null;
    	}

    	// verify cardinality of value(s)
    	int cardinality = ad.getCardinality();
    	Object result = null;
    	if (cardinality == 0) {
    		if (typedContent.length == 1) {
    			result = typedContent[0];
    		}
    		else {
    			result = null;
    		}
    	}
    	else if (cardinality == Integer.MIN_VALUE) {
    		result = new Vector(Arrays.asList(typedContent));
    	}
    	else if (cardinality == Integer.MAX_VALUE) {
    		result = typedContent;
    	}
    	else if (cardinality < 0) {
            if (typedContent.length <= Math.abs(cardinality)) {
    			result = new Vector(Arrays.asList(typedContent));
    		}
    		else {
    			result = null;
    		}
    	}
    	else if (cardinality > 0) {
    		if (typedContent.length <= cardinality) {
    			result = typedContent;
    		}
    		else {
    			result = null;
    		}
    	}
    	return result;
    }

    public void handleEvent(Event event) {
    	// regardless of the outcome, we simply invoke postcommit
    	postcommit();
    }
}

interface ConfigurationAdminTask {
    public String getFilter();
    public void run(PersistencyManager persistencyManager, ConfigurationAdmin configAdmin) throws Exception;
}

interface PostCommitTask {
    public void run(PersistencyManager manager) throws Exception;
}

class DropResourceTask implements ConfigurationAdminTask {
    private final AutoConfResource m_resource;

    public DropResourceTask(AutoConfResource resource) {
        m_resource = resource;
    }
    
    public String getFilter() {
        return m_resource.getFilter();
    }

    public void run(PersistencyManager persistencyManager, ConfigurationAdmin configAdmin) throws Exception {
        String pid;
        if (m_resource.isFactoryConfig()) {
            pid = m_resource.getGeneratedPid();
        }
        else {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            pid = m_resource.getPid();
        }
        Configuration configuration = configAdmin.getConfiguration(pid, m_resource.getBundleLocation());
        configuration.delete();
    }
}

<<<<<<< HEAD
class InstallOrUpdateResourceTask implements ConfigurationAdminTask {
    private final AutoConfResource m_resource;

    public InstallOrUpdateResourceTask(AutoConfResource resource) {
        m_resource = resource;
    }

    public String getFilter() {
        return m_resource.getFilter();
    }

    public void run(PersistencyManager persistencyManager, ConfigurationAdmin configAdmin) throws Exception {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        String name = m_resource.getName();
        Dictionary properties = m_resource.getProperties();
        String bundleLocation = m_resource.getBundleLocation();
        Configuration configuration = null;

        List existingResources = null;
<<<<<<< HEAD
        try {
            existingResources = persistencyManager.load(name);
        }
        catch (IOException ioe) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Unable to read existing resources for resource " + name, ioe);
        }
        
        // update configuration
        if (m_resource.isFactoryConfig()) {
            // check if this is an factory config instance update
            for (Iterator i = existingResources.iterator(); i.hasNext();) {
                AutoConfResource existingResource = (AutoConfResource) i.next();
                if (m_resource.equalsTargetConfiguration(existingResource)) {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    // existing instance found
                    configuration = configAdmin.getConfiguration(existingResource.getGeneratedPid(), bundleLocation);
                    existingResources.remove(existingResource);
                    break;
                }
            }
<<<<<<< HEAD
            if (configuration == null) {
=======
            if (configuration == null)
            {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                // no existing instance, create new
                configuration = configAdmin.createFactoryConfiguration(m_resource.getFactoryPid(), bundleLocation);
            }
            m_resource.setGeneratedPid(configuration.getPid());
        }
<<<<<<< HEAD
        else {
            for (Iterator i = existingResources.iterator(); i.hasNext();) {
                AutoConfResource existingResource = (AutoConfResource) i.next();
                if (m_resource.getPid().equals(existingResource.getPid())) {
=======
        else
        {
            for (Iterator i = existingResources.iterator(); i.hasNext();)
            {
                AutoConfResource existingResource = (AutoConfResource) i.next();
                if (m_resource.getPid().equals(existingResource.getPid()))
                {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    // existing resource found
                    existingResources.remove(existingResource);
                    break;
                }
            }
            configuration = configAdmin.getConfiguration(m_resource.getPid(), bundleLocation);
<<<<<<< HEAD
            if (!bundleLocation.equals(configuration.getBundleLocation())) {
                // an existing configuration exists that is bound to a different location, which is not allowed
                throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Existing configuration was bound to " + configuration.getBundleLocation() + " instead of " + bundleLocation);
            }
        }
        if (m_resource.isMerge()) {
            Dictionary existingProperties = configuration.getProperties();
            if (existingProperties != null) {
                Enumeration keys = existingProperties.keys();
                while (keys.hasMoreElements()) {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    Object key = keys.nextElement();
                    properties.put(key, existingProperties.get(key));
                }
            }
        }
        configuration.update(properties);
    }
}

<<<<<<< HEAD
class DeleteResourceTask implements PostCommitTask {
    private final String m_name;

    public DeleteResourceTask(String name) {
        m_name = name;
    }

    public void run(PersistencyManager manager) throws Exception {
        manager.delete(m_name);
    }
}

class StoreResourceTask implements PostCommitTask {
    private final String m_name;
    private final List m_resources;

    public StoreResourceTask(String name, List resources) {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        m_name = name;
        m_resources = resources;
    }

<<<<<<< HEAD
    public void run(PersistencyManager manager) throws Exception {
=======
    public void run(PersistencyManager manager) throws Exception
    {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        manager.store(m_name, m_resources);
    }
}