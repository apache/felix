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
package org.apache.felix.deploymentadmin.spi;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.ResourceInfoImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
<<<<<<< HEAD
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.service.log.LogService;

/**
 * Command that drops resources.
 */
public class DropAllResourcesCommand extends Command {
<<<<<<< HEAD

    private final CommitResourceCommand m_commitCommand;

    /**
     * Creates an instance of this command. The commit command is used to make sure
     * the resource processors used to drop all resources will be committed at a later stage in the process.
     *
     * @param commitCommand The commit command that will be executed at a later stage in the process.
=======
    private final CommitResourceCommand m_commitCommand;

    /**
     * Creates an instance of this command. The commit command is used to make
     * sure the resource processors used to drop all resources will be committed
     * at a later stage in the process.
     * 
     * @param commitCommand The commit command that will be executed at a later
     *        stage in the process.
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
    public DropAllResourcesCommand(CommitResourceCommand commitCommand) {
        m_commitCommand = commitCommand;
    }

<<<<<<< HEAD
    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        // Allow proper rollback in case the drop fails...
        addRollback(new RollbackCommitAction(session));
        
=======
    protected void doExecute(DeploymentSessionImpl session) throws Exception {
        // Allow proper rollback in case the drop fails...
        addRollback(new RollbackCommitAction(session));

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();
        LogService log = session.getLog();

        // Collect all unique paths of all processed resources...
        Set resourceProcessors = new HashSet();

        ResourceInfoImpl[] orderedTargetResources = target.getOrderedResourceInfos();
        for (int i = orderedTargetResources.length - 1; i >= 0; i--) {
            ResourceInfoImpl resourceInfo = orderedTargetResources[i];
<<<<<<< HEAD
            
            String rpName = resourceInfo.getResourceProcessor();
            String path = resourceInfo.getPath();

            if (resourceProcessors.contains(rpName)) {
=======
            // FELIX-4491: only resources that need to be processed should be handled...
            if (!resourceInfo.isProcessedResource()) {
                session.getLog().log(LogService.LOG_INFO, "Ignoring non-processed resource: " + resourceInfo.getPath());
                continue;
            }

            String rpName = resourceInfo.getResourceProcessor();
            String path = resourceInfo.getPath();

            // Keep track of which resource processors we've seen already...
            if (!resourceProcessors.add(rpName)) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                // Already seen this RP; continue on the next one...
                continue;
            }

<<<<<<< HEAD
            // Keep track of which resource processors we've seen already...
            resourceProcessors.add(rpName);

            ServiceReference ref = target.getResourceProcessor(path);
            if (ref == null) {
                log.log(LogService.LOG_ERROR, "Failed to find resource processor for '" + rpName + "'!");
                throw new DeploymentException(DeploymentException.CODE_PROCESSOR_NOT_FOUND, "Failed to find resource processor '" + rpName + "'!");
            }
            
            ResourceProcessor resourceProcessor = (ResourceProcessor) context.getService(ref);
            if (resourceProcessor == null) {
                log.log(LogService.LOG_ERROR, "Failed to find resource processor for '" + rpName + "'!");
                throw new DeploymentException(DeploymentException.CODE_PROCESSOR_NOT_FOUND, "Failed to find resource processor '" + rpName + "'!");
            }
            
=======
            ServiceReference ref = target.getResourceProcessor(path);
            if (ref == null) {
                log.log(LogService.LOG_ERROR, "Failed to find resource processor for '" + rpName + "'!");
                throw new DeploymentException(CODE_PROCESSOR_NOT_FOUND, "Failed to find resource processor '" + rpName + "'!");
            }

            ResourceProcessor resourceProcessor = (ResourceProcessor) context.getService(ref);
            if (resourceProcessor == null) {
                log.log(LogService.LOG_ERROR, "Failed to find resource processor for '" + rpName + "'!");
                throw new DeploymentException(CODE_PROCESSOR_NOT_FOUND, "Failed to find resource processor '" + rpName + "'!");
            }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            try {
                if (m_commitCommand.addResourceProcessor(resourceProcessor)) {
                    resourceProcessor.begin(session);
                }
                resourceProcessor.dropAllResources();
            }
<<<<<<< HEAD
            catch (ResourceProcessorException e) {
                log.log(LogService.LOG_ERROR, "Failed to drop all resources for resource processor '" + rpName + "'!", e);
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Failed to drop all resources for resource processor '" + rpName + "'!", e);
            }
        }
    }
    
    private class RollbackCommitAction implements Runnable {
        private final DeploymentSessionImpl m_session; 
        
        public RollbackCommitAction(DeploymentSessionImpl session) {
            m_session = session;
        }
        
        public void run() {
=======
            catch (Exception e) {
                log.log(LogService.LOG_ERROR, "Failed to drop all resources for resource processor '" + rpName + "'!", e);
                throw new DeploymentException(CODE_OTHER_ERROR, "Failed to drop all resources for resource processor '" + rpName + "'!", e);
            }
        }
    }

    private class RollbackCommitAction extends AbstractAction {
        private final DeploymentSessionImpl m_session;

        public RollbackCommitAction(DeploymentSessionImpl session) {
            m_session = session;
        }

        protected void doRun() {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            m_commitCommand.rollback(m_session);
        }
    }
}
