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
package org.apache.felix.scr.impl.compat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;

public class ScrServiceImpl implements ScrService
{

	private static final String[] EMPTY = {};

	private final BundleContext context;
	private final ServiceComponentRuntime runtime;


	public ScrServiceImpl(BundleContext context, final ServiceComponentRuntime runtime)
	{
        // we always use the system bundle to avoid problems if subsystems etc.
        // are used and the SCR implemented extends those "invisible" bundles
		this.context = context.getBundle(0).getBundleContext();
		this.runtime = runtime;
	}


	// ScrService

    /**
     * @see org.apache.felix.scr.ScrService#getComponents()
     */
    public Component[] getComponents()
    {
        return this.getComponents((Bundle)null);
    }


    /**
     * @see org.apache.felix.scr.ScrService#getComponents(org.osgi.framework.Bundle)
     */
    public Component[] getComponents( final Bundle bundle )
    {
        List<Component> result = new ArrayList<Component>();

        final Collection<ComponentDescriptionDTO> descriptions = (bundle == null ? this.runtime.getComponentDescriptionDTOs() : this.runtime.getComponentDescriptionDTOs(bundle));
        for(final ComponentDescriptionDTO descDTO : descriptions )
        {
            final Collection<ComponentConfigurationDTO> configs = this.runtime.getComponentConfigurationDTOs(descDTO);
            ComponentConfigurationDTO configDTO = null;
            if ( !configs.isEmpty() )
            {
                configDTO = configs.iterator().next();
            }
            result.add(new ComponentWrapper(this.context, this.runtime, descDTO, configDTO));
        }

        return result.isEmpty() ? null : result.toArray( new Component[result.size()] );
    }


    /**
     * @see org.apache.felix.scr.ScrService#getComponent(long)
     */
    public Component getComponent( long componentId )
    {
        final Collection<ComponentDescriptionDTO> descriptions = this.runtime.getComponentDescriptionDTOs();
        for(final ComponentDescriptionDTO descDTO : descriptions )
        {
            final Collection<ComponentConfigurationDTO> configs = this.runtime.getComponentConfigurationDTOs(descDTO);
            for(final ComponentConfigurationDTO configDTO : configs)
            {
                if ( configDTO.id == componentId )
                {
                    return new ComponentWrapper(this.context, this.runtime, descDTO, configDTO);
                }
            }
        }

        return null;
    }

    /**
     * @see org.apache.felix.scr.ScrService#getComponents(java.lang.String)
     */
    public Component[] getComponents( final String componentName )
    {
        List<Component> result = new ArrayList<Component>();

        final Collection<ComponentDescriptionDTO> descriptions = this.runtime.getComponentDescriptionDTOs();
        for(final ComponentDescriptionDTO descDTO : descriptions )
        {
            if ( descDTO.name.equals(componentName) ) {
                final Collection<ComponentConfigurationDTO> configs = this.runtime.getComponentConfigurationDTOs(descDTO);
                ComponentConfigurationDTO configDTO = null;
                if ( !configs.isEmpty() )
                {
                    configDTO = configs.iterator().next();
                }
                result.add(new ComponentWrapper(this.context, this.runtime, descDTO, configDTO));
            }
        }

        return result.isEmpty() ? null : result.toArray( new Component[result.size()] );
    }


    private static final class ComponentWrapper implements Component
    {
        private final ComponentDescriptionDTO description;

        private final ComponentConfigurationDTO configuration;

        private final BundleContext bundleContext;

        private final ServiceComponentRuntime runtime;

        public ComponentWrapper(final BundleContext bundleContext,
                final ServiceComponentRuntime runtime,
                final ComponentDescriptionDTO description,
                final ComponentConfigurationDTO configuration)
        {
            this.bundleContext = bundleContext;
            this.description = description;
            this.configuration = configuration;
            this.runtime = runtime;
        }

        public long getId()
        {
            return configuration != null ? configuration.id : -1;
        }

        public String getName()
        {
            return description.name;
        }

        public int getState()
        {
            if ( configuration == null )
            {
                return Component.STATE_UNSATISFIED; // TODO Check!
            }
            final int s = configuration.state;
            switch ( s )
            {
                case ComponentConfigurationDTO.ACTIVE : return Component.STATE_ACTIVE;
                case ComponentConfigurationDTO.SATISFIED : return Component.STATE_ENABLED;
                case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION : return Component.STATE_UNSATISFIED;
                case ComponentConfigurationDTO.UNSATISFIED_REFERENCE : return Component.STATE_UNSATISFIED;
                default: // satisfied
                    return Component.STATE_ENABLED;
            }
        }

        public Bundle getBundle()
        {
            return this.bundleContext.getBundle(this.description.bundle.id);
        }

        public String getFactory()
        {
            return this.description.factory;
        }

        public boolean isServiceFactory()
        {
            return !"singleton".equals(this.description.scope);
        }

        public String getClassName()
        {
            return this.description.implementationClass;
        }

        public boolean isDefaultEnabled()
        {
            return this.description.defaultEnabled;
        }

        public boolean isImmediate()
        {
            return this.description.immediate;
        }

        public String[] getServices()
        {
            return this.description.serviceInterfaces.length == 0 ? null : this.description.serviceInterfaces;
        }

        public Dictionary getProperties()
        {
            return new Hashtable<String, Object>(this.description.properties);
        }

        public Reference[] getReferences()
        {
            if ( this.configuration == null )
            {
                return null;
            }

            final List<Reference> result = new ArrayList<Reference>();
            for(final ReferenceDTO dto : this.description.references)
            {
                SatisfiedReferenceDTO sRef = null;
                for(final SatisfiedReferenceDTO r : this.configuration.satisfiedReferences)
                {
                    if ( r.name.equals(dto.name) )
                    {
                        sRef = r;
                        break;
                    }
                }
                result.add(new ReferenceWrapper(this.bundleContext, dto, sRef));
            }

            if ( result.isEmpty() )
            {
                return null;
            }

            return result.toArray(new Reference[result.size()]);
        }

        public ComponentInstance getComponentInstance()
        {
            // returning null as we should have never returned this in the first place
            return null;
        }

        public String getActivate()
        {
            return this.description.activate;
        }

        public boolean isActivateDeclared()
        {
            return this.description.activate != null;
        }

        public String getDeactivate()
        {
            return this.description.deactivate;
        }

        public boolean isDeactivateDeclared()
        {
            return this.description.deactivate != null;
        }

        public String getModified()
        {
            return this.description.modified;
        }

        public String getConfigurationPolicy()
        {
            return this.description.configurationPolicy;
        }

        public String getConfigurationPid()
        {
            final String[] pids = this.description.configurationPid;
            return pids[0];
        }

        public boolean isConfigurationPidDeclared()
        {
            return true;
        }

        public void enable()
        {
            // noop as the old model was broken
        }

        public void disable()
        {
            // noop as the old model was broken
        }
    }

    private static final class ReferenceWrapper implements Reference
    {
        // constant for option single reference - 0..1
        private static final String CARDINALITY_0_1 = "0..1";

        // constant for option multiple reference - 0..n
        private static final String CARDINALITY_0_N = "0..n";

        // constant for required multiple reference - 1..n
        private static final String CARDINALITY_1_N = "1..n";

        // constant for static policy
        private static final String POLICY_STATIC = "static";

        // constant for reluctant policy option
        private static final String POLICY_OPTION_RELUCTANT = "reluctant";

        private final ReferenceDTO dto;

        private final SatisfiedReferenceDTO satisfiedDTO;

        private final BundleContext bundleContext;

        public ReferenceWrapper(
                final BundleContext bundleContext,
                final ReferenceDTO dto,
                final SatisfiedReferenceDTO satisfied)
        {
            this.bundleContext = bundleContext;
            this.dto = dto;
            this.satisfiedDTO = satisfied;
        }

        public String getName()
        {
            return dto.name;
        }

        public String getServiceName()
        {
            return dto.interfaceName;
        }

        public ServiceReference[] getServiceReferences()
        {
            if ( this.satisfiedDTO == null )
            {
                return null;
            }
            final List<ServiceReference<?>> refs = new ArrayList<ServiceReference<?>>();
            for(ServiceReferenceDTO dto : this.satisfiedDTO.boundServices)
            {
                try
                {
                    final ServiceReference<?>[] serviceRefs = this.bundleContext.getServiceReferences((String)null,
                            "(" + Constants.SERVICE_ID + "=" + String.valueOf(dto.id) + ")");
                    if ( serviceRefs != null && serviceRefs.length > 0 )
                    {
                        refs.add(serviceRefs[0]);
                    }
                }
                catch ( final InvalidSyntaxException ise)
                {
                    // ignore
                }
            }
            return refs.toArray(new ServiceReference<?>[refs.size()]);
        }

        public ServiceReference<?>[] getBoundServiceReferences()
        {
            return this.getServiceReferences();
        }

        public boolean isSatisfied()
        {
            return this.satisfiedDTO != null;
        }

        public boolean isOptional()
        {
            return CARDINALITY_0_1.equals(dto.cardinality) || CARDINALITY_0_N.equals(dto.cardinality);
        }

        public boolean isMultiple()
        {
            return CARDINALITY_1_N.equals(dto.cardinality) || CARDINALITY_0_N.equals(dto.cardinality);
        }

        public boolean isStatic()
        {
            return POLICY_STATIC.equals(dto.policy);
        }

        public boolean isReluctant()
        {
            return POLICY_OPTION_RELUCTANT.equals(dto.policyOption);
        }

        public String getTarget()
        {
            return this.dto.target;
        }

        public String getBindMethodName()
        {
            return this.dto.bind;
        }

        public String getUnbindMethodName()
        {
            return this.dto.unbind;
        }

        public String getUpdatedMethodName()
        {
            return this.dto.unbind;
        }
    }
}
