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
package org.apache.felix.dm.lambda.samples.future;

import static java.lang.System.out;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * This examples show how to use the new "Future" dependency available from the dependencymanager-lambda library.
 * The PageLink component provides the list of available hrefs found from the Felix web site.
 * The page is downloaded asynchronously using a CompletableFuture, and the component of the PageLinkImpl class
 * will wait for the completion of the future before start.
 * 
 * The interesting thing to look at is located in the PageLinkImpl.init() method.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyManagerActivator {
    /**
     * Initialize our components using new DM-lambda activator base.
     */
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    	out.println("type \"log warn\" to see the logs emitted by this test.");
    	
    	// System.setProperty("http.proxyHost","your.http.proxy.host");
    	// System.setProperty("http.proxyPort", "your.http.proxy.port");
    	    	
        // Create the PageLinks service, which asynchronously downloads the content of the Felix web page.
    	// The PageLink service will be started once the page has been downloaded (using a CompletableFuture).
        component(comp -> comp
            .factory(() -> new PageLinksImpl("http://felix.apache.org/"))
            .provides(PageLinks.class)
            .withSvc(LogService.class, log -> log.required().add(PageLinksImpl::bind)));
        
        // Just wait for the PageLinks service and display all links found from the Felix web site.
        component(comp -> comp.impl(this).withSvc(PageLinks.class, page -> page.add(this::setPageLinks))); 
    }
        
    /**
     * display all the hrefs (links) found from the Felix web site.
     */
    void setPageLinks(PageLinks page) {
        out.println("Felix site links: " + page.getLinks());
    }
}
