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
package org.apache.felix.scr;
    
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.api.adapter.AdapterManager;

import javax.annotation.Nonnull;

/**
 * Just a simple DS Component
 */
@Component
@Service
public class SimpleDSComponent implements Runnable {
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private BundleContext bundleContext;

    @Nonnull
    public SimpleDSComponent() {
        
    }

    @Nonnull
    public SimpleDSComponent(String m) {
        
    }

    @Nonnull
    public void run() {
        logger.info("Running...");
    }
    
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }
    
    protected void deactivate() {
        this.bundleContext = null;
    }

}

