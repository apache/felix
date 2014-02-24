/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.extender.internal.declaration.service;

import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link org.osgi.framework.ServiceFactory} for {@link org.apache.felix.ipojo.extender.DeclarationBuilderService}.
 * It avoids to explicitly gives a {@link org.osgi.framework.BundleContext} for each service method.
 * The {@link org.osgi.framework.BundleContext} of the client is used.
 */
public class DeclarationServiceFactory implements ServiceFactory<DeclarationBuilderService> {

    private final BundleContext bundleContext;
    private ServiceRegistration<?> registration;

    public DeclarationServiceFactory(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void start() {
        if (registration == null) {
            registration = bundleContext.registerService(DeclarationBuilderService.class.getName(), this, null);
        }
    }

    public void stop() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    public DeclarationBuilderService getService(final Bundle bundle, final ServiceRegistration<DeclarationBuilderService> registration) {
        return new DefaultDeclarationBuilderService(bundle.getBundleContext());
    }

    public void ungetService(final Bundle bundle, final ServiceRegistration<DeclarationBuilderService> registration, final DeclarationBuilderService service) {
        // Nothing to do, built declarations will be kept
        // It's the client responsibility to dispose its declarations
    }


}
