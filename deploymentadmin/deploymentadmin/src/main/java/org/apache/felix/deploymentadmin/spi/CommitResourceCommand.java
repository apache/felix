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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.log.LogService;

/**
 * Command that commits all the resource processors that were added to the
 * command.
 */
public class CommitResourceCommand extends Command {

    private final List m_processors = new ArrayList();

    protected void doExecute(DeploymentSessionImpl session) throws Exception {
        for (ListIterator i = m_processors.listIterator(m_processors.size()); i.hasPrevious();) {
            ResourceProcessor processor = (ResourceProcessor) i.previous();
            try {
                processor.prepare();
            }
            catch (ResourceProcessorException e) {
                session.getLog().log(LogService.LOG_ERROR, "Preparing commit for resource processor failed", e);
                // Check what error code we got...
                if (e.getCode() == ResourceProcessorException.CODE_PREPARE) {
                    throw new DeploymentException(CODE_COMMIT_ERROR, "Preparing commit for resource processor failed!", e);
                }
                throw new DeploymentException(e.getCode(), "Preparing commit for resource processor failed!", e);
            }
            catch (Exception e) {
                session.getLog().log(LogService.LOG_ERROR, "Preparing commit for resource processor failed", e);
                throw new DeploymentException(CODE_OTHER_ERROR, "Preparing commit for resource processor failed", e);
            }
        }
        for (ListIterator i = m_processors.listIterator(m_processors.size()); i.hasPrevious();) {
            ResourceProcessor processor = (ResourceProcessor) i.previous();
            try {
                processor.commit();
            }
            catch (Exception e) {
                // We cannot throw an exception, see OSGi spec.
                session.getLog().log(LogService.LOG_ERROR, "Committing resource processor '" + processor + "' failed", e);
            }
        }
        m_processors.clear();
    }

    protected void rollback(DeploymentSessionImpl session) {
        for (ListIterator i = m_processors.listIterator(m_processors.size()); i.hasPrevious();) {
            ResourceProcessor processor = (ResourceProcessor) i.previous();
            try {
                processor.rollback();
            }
            catch (Exception e) {
                // We cannot throw an exception, see OSGi spec.
                session.getLog().log(LogService.LOG_ERROR, "Rollback of resource processor '" + processor + "' failed", e);
            }
            finally {
                i.remove();
            }
        }
    }

    /**
     * Add a resource processor, all resource processors that are added will be
     * committed when the command is executed.
     * 
     * @param processor The resource processor to add.
     * @return true if the resource processor was added, false if it was already
     *         added.
     */
    public boolean addResourceProcessor(ResourceProcessor processor) {
        for (Iterator i = m_processors.iterator(); i.hasNext();) {
            ResourceProcessor proc = (ResourceProcessor) i.next();
            if (proc == processor) {
                return false;
            }
        }
        m_processors.add(processor);
        return true;
    }
}
