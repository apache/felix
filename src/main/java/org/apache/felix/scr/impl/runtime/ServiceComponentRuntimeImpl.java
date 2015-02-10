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
package org.apache.felix.scr.impl.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.ComponentManager;
import org.apache.felix.scr.impl.config.ReferenceManager;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.SingleComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.promise.Promise;

public class ServiceComponentRuntimeImpl implements ServiceComponentRuntime, ScrService
{

	private static final String[] EMPTY = {};

	private final BundleContext context;
	private final ComponentRegistry componentRegistry;


	public ServiceComponentRuntimeImpl(BundleContext context,ComponentRegistry componentRegistry)
	{
		this.context = context;
		this.componentRegistry = componentRegistry;
	}

	public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle... bundles)
	{
		List<ComponentHolder<?>> holders;
		if (bundles == null || bundles.length == 0)
		{
			holders = componentRegistry.getComponentHolders();
		}
		else
		{
			holders = componentRegistry.getComponentHolders(bundles);
		}

		List<ComponentDescriptionDTO> result = new ArrayList<ComponentDescriptionDTO>(holders.size());
		for (ComponentHolder<?> holder: holders)
		{
			result.add(holderToDescription(holder));
		}
		return result;
	}

	public ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle, String name)
	{
	    ComponentHolder<?> holder = componentRegistry.getComponentHolder(bundle, name);
		if ( holder != null )
		{
			return holderToDescription(holder);
		}
		else
		{
			return null;
		}
	}

	public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description)
	{
		if ( description == null)
		{
			return Collections.emptyList();
		}
		ComponentHolder<?> holder = getHolderFromDescription( description);
		//Get a fully filled out valid description DTO
		description = holderToDescription(holder);
		List<? extends ComponentManager<?>> managers = holder.getComponents();
		List<ComponentConfigurationDTO> result = new ArrayList<ComponentConfigurationDTO>(managers.size());
		for (ComponentManager<?> manager: managers)
		{
			result.add(managerToConfiguration(manager, description));
		}
		return result;
	}

	public boolean isComponentEnabled(ComponentDescriptionDTO description)
	{
		ComponentHolder<?> holder = getHolderFromDescription( description);
		return holder.isEnabled();
	}

	public Promise<Void> enableComponent(ComponentDescriptionDTO description)
	{
		ComponentHolder<?> holder = getHolderFromDescription( description);
		return holder.enableComponents(true);
	}

	public Promise<Void> disableComponent(ComponentDescriptionDTO description)
	{
		ComponentHolder<?> holder = getHolderFromDescription( description);
		return holder.disableComponents(true); //synchronous
	}

	private ComponentConfigurationDTO managerToConfiguration(ComponentManager<?> manager, ComponentDescriptionDTO description)
	{
		ComponentConfigurationDTO dto = new ComponentConfigurationDTO();
        dto.satisfiedReferences = satisfiedRefManagersToDTO(manager.getReferenceManagers());
        dto.unsatisfiedReferences = unsatisfiedRefManagersToDTO(manager.getReferenceManagers());
		dto.description = description;
		dto.id = manager.getId();
		dto.properties = new HashMap<String, Object>(manager.getProperties());//TODO deep copy?
		dto.state = manager.getState();
		return dto;
	}

    private SatisfiedReferenceDTO[] satisfiedRefManagersToDTO(List<? extends ReferenceManager<?, ?>> referenceManagers)
    {
        List<SatisfiedReferenceDTO> dtos = new ArrayList<SatisfiedReferenceDTO>();
        for (ReferenceManager<?, ?> ref: referenceManagers)
        {
            if (ref.isSatisfied())
            {
                SatisfiedReferenceDTO dto = new SatisfiedReferenceDTO();
                dto.name = ref.getName();
                dto.target = ref.getTarget();
                List<ServiceReference<?>> serviceRefs = ref.getServiceReferences();
                ServiceReferenceDTO[] srDTOs = new ServiceReferenceDTO[serviceRefs.size()];
                int j = 0;
                for (ServiceReference<?> serviceRef : serviceRefs)
                {
                    srDTOs[j++] = serviceReferenceToDTO(serviceRef);
                }
                dto.boundServices = srDTOs;
                dtos.add(dto);
            }
        }
        return dtos.toArray( new SatisfiedReferenceDTO[dtos.size()] );
    }

    private UnsatisfiedReferenceDTO[] unsatisfiedRefManagersToDTO(List<? extends ReferenceManager<?, ?>> referenceManagers)
    {
        List<UnsatisfiedReferenceDTO> dtos = new ArrayList<UnsatisfiedReferenceDTO>();
        for (ReferenceManager<?, ?> ref: referenceManagers)
        {
            if (!ref.isSatisfied())
            {
                UnsatisfiedReferenceDTO dto = new UnsatisfiedReferenceDTO();
                dto.name = ref.getName();
                dto.target = ref.getTarget();
                List<ServiceReference<?>> serviceRefs = ref.getServiceReferences();
                ServiceReferenceDTO[] srDTOs = new ServiceReferenceDTO[serviceRefs.size()];
                int j = 0;
                for (ServiceReference<?> serviceRef : serviceRefs)
                {
                    srDTOs[j++] = serviceReferenceToDTO(serviceRef);
                }
                dto.targetServices = srDTOs;
                dtos.add(dto);
            }
        }
        return dtos.toArray( new UnsatisfiedReferenceDTO[dtos.size()] );
    }

	private ServiceReferenceDTO serviceReferenceToDTO( ServiceReference<?> serviceRef)
	{
		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		dto.bundle = serviceRef.getBundle().getBundleId();
		dto.id = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
		dto.properties = deepCopy( serviceRef );
		Bundle[] usingBundles = serviceRef.getUsingBundles();
		if (usingBundles != null)
        {
            long[] usingBundleIds = new long[usingBundles.length];
            for (int i = 0; i < usingBundles.length; i++)
            {
                usingBundleIds[i] = usingBundles[i].getBundleId();
            }
            dto.usingBundles = usingBundleIds;
        }
        return dto;
	}

	private ComponentHolder<?> getHolderFromDescription(ComponentDescriptionDTO description)
	{
		if (description.bundle == null)
		{
			throw new IllegalArgumentException("No bundle supplied in ComponentDescriptionDTO named " + description.name);
		}
		long bundleId = description.bundle.id;
		Bundle b = context.getBundle(bundleId);
		String name = description.name;
		return componentRegistry.getComponentHolder(b, name);
	}

	private ComponentDescriptionDTO holderToDescription( ComponentHolder<?> holder )
	{
		ComponentDescriptionDTO dto = new ComponentDescriptionDTO();
		ComponentMetadata m = holder.getComponentMetadata();
		dto.activate = m.getActivate();
		dto.bundle = bundleToDTO(holder.getActivator().getBundleContext());
		dto.configurationPid = m.getConfigurationPid().toArray(new String[m.getConfigurationPid().size()]);
		dto.configurationPolicy = m.getConfigurationPolicy();
		dto.deactivate = m.getDeactivate();
		dto.defaultEnabled = m.isEnabled();
		dto.factory = m.getFactoryIdentifier();
		dto.immediate = m.isImmediate();
		dto.implementationClass = m.getImplementationClassName();
		dto.modified = m.getModified();
		dto.name = m.getName();
		dto.properties = deepCopy(m.getProperties());
		dto.references = refsToDTO(m.getDependencies());
		dto.scope = m.getServiceMetadata() == null? null: m.getServiceMetadata().getScope().name();
		dto.serviceInterfaces = m.getServiceMetadata() == null? EMPTY: m.getServiceMetadata().getProvides();
		return dto;
	}

    private Map<String, Object> deepCopy(Map<String, Object> source)
    {
        HashMap<String, Object> result = new HashMap<String, Object>(source.size());
        for (Map.Entry<String, Object> entry: source.entrySet())
        {
            result.put(entry.getKey(), convert(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> deepCopy(ServiceReference<?> source)
    {
        String[] keys = source.getPropertyKeys();
        HashMap<String, Object> result = new HashMap<String, Object>(keys.length);
        for (int i = 0; i< keys.length; i++)
        {
            result.put(keys[i], convert(source.getProperty(keys[i])));
        }
        return result;
    }

    Object convert(Object source)
	{
	    if (source.getClass().isArray())
	    {
	        Class<?> type = source.getClass().getComponentType();
	        if (checkType(type))
	        {
	            return source;
	        }
	        return String.valueOf(source);
	        /* array copy code in case it turns out to be needed
	        int length = Array.getLength(source);
            Object copy = Array.newInstance(type, length);
	        for (int i = 0; i<length; i++)
	        {
	            Array.set(copy, i, Array.get(source, i));
	        }
	        return copy;
	        */
	    }
	    if (checkType(source.getClass()))
	    {
	        return source;
	    }
	    return String.valueOf(source);
	}

    boolean checkType(Class<?> type)
    {
        if (type == String.class) return true;
        if (type == Boolean.class) return true;
        if (Number.class.isAssignableFrom(type)) return true;
        if (DTO.class.isAssignableFrom(type)) return true;
        return false;
    }

	private ReferenceDTO[] refsToDTO(List<ReferenceMetadata> dependencies)
	{
		ReferenceDTO[] dtos = new ReferenceDTO[dependencies.size()];
		int i = 0;
		for (ReferenceMetadata r: dependencies)
		{
			ReferenceDTO dto = new ReferenceDTO();
			dto.bind = r.getBind();
			dto.cardinality = r.getCardinality();
			dto.field = r.getField();
			dto.fieldOption = r.getFieldOption();
			dto.interfaceName = r.getInterface();
			dto.name = r.getName();
			dto.policy = r.getPolicy();
			dto.policyOption = r.getPolicyOption();
			dto.scope = r.getScope().name();
			dto.target = r.getTarget();
			dto.unbind = r.getUnbind();
			dto.updated = r.getUpdated();
			dtos[i++] = dto;
		}
		return dtos;
	}

	private BundleDTO bundleToDTO(BundleContext bundleContext)
	{
		if (bundleContext == null)
		{
			return null;
		}
		Bundle bundle = bundleContext.getBundle();
		if (bundle == null)
		{
			return null;
		}
		BundleDTO b = new BundleDTO();
		b.id = bundle.getBundleId();
		b.lastModified = bundle.getLastModified();
		b.state = bundle.getState();
		b.symbolicName = bundle.getSymbolicName();
		b.version = bundle.getVersion().toString();
		return b;
	}

	// ScrService

    /**
     * @see org.apache.felix.scr.ScrService#getComponents()
     */
    public Component[] getComponents()
    {
        final List<ComponentHolder<?>> holders = componentRegistry.getComponentHolders();
        ArrayList<Component> list = new ArrayList<Component>();
        for ( ComponentHolder<?> holder: holders )
        {
            if ( holder != null )
            {
                final List<? extends ComponentManager<?>> components = holder.getComponents();
                for ( ComponentManager<?> component: components )
                {
                    list.add( new ComponentWrapper((AbstractComponentManager<?>)component) );
                }
            }
        }

        // nothing to return
        if ( list.isEmpty() )
        {
            return null;
        }

        return list.toArray( new Component[list.size()] );
    }


    /**
     * @see org.apache.felix.scr.ScrService#getComponents(org.osgi.framework.Bundle)
     */
    public Component[] getComponents( Bundle bundle )
    {
        final List<ComponentHolder<?>> holders = componentRegistry.getComponentHolders();
        ArrayList<Component> list = new ArrayList<Component>();
        for ( ComponentHolder<?> holder: holders )
        {
            if ( holder != null )
            {
                BundleComponentActivator activator = holder.getActivator();
                if ( activator != null && activator.getBundleContext().getBundle() == bundle )
                {
                    final List<? extends ComponentManager<?>> components = holder.getComponents();
                    for ( ComponentManager<?> component: components )
                    {
                        list.add( new ComponentWrapper((AbstractComponentManager<?>)component) );
                    }
                }
            }
        }

        // nothing to return
        if ( list.isEmpty() )
        {
            return null;
        }

        return list.toArray( new Component[list.size()] );
    }


    /**
     * @see org.apache.felix.scr.ScrService#getComponent(long)
     */
    public Component getComponent( long componentId )
    {
        final AbstractComponentManager<?> c = componentRegistry.getComponentManagerById(componentId);
        if ( c != null )
        {
            return new ComponentWrapper(c);
        }
        return null;
    }

    /**
     * @see org.apache.felix.scr.ScrService#getComponents(java.lang.String)
     */
    public Component[] getComponents( final String componentName )
    {
        final List<Component> list = new ArrayList<Component>();
        final List<ComponentHolder<?>> holders = componentRegistry.getComponentHolders();
        for ( ComponentHolder<?> holder: holders )
        {
            if ( holder.getComponentMetadata().getName().equals(componentName) )
            {
                final List<? extends ComponentManager<?>> components = holder.getComponents();
                for ( ComponentManager<?> component: components )
                {
                    list.add( new ComponentWrapper((AbstractComponentManager<?>)component) );
                }
            }
        }

        return ( list.isEmpty() ) ? null : list.toArray( new Component[list.size()] );
    }


    private static final class ComponentWrapper implements Component
    {
        private final AbstractComponentManager<?> mgr;

        public ComponentWrapper(final AbstractComponentManager<?> mgr)
        {
            this.mgr = mgr;
        }

        public long getId()
        {
            return mgr.getId();
        }

        public String getName()
        {
            return mgr.getComponentMetadata().getName();
        }

        public int getState()
        {
            final int s = mgr.getState();
            switch ( s )
            {
                case ComponentManager.STATE_DISPOSED : return Component.STATE_DISPOSED;
                case ComponentManager.STATE_DISABLED : return Component.STATE_DISABLED;
                case ComponentManager.STATE_UNSATISFIED_REFERENCE : return Component.STATE_UNSATISFIED;
                case ComponentManager.STATE_ACTIVE : return Component.STATE_ACTIVE;
                default: // satisfied
                    return Component.STATE_ENABLED;
            }
        }

        public Bundle getBundle()
        {
            return mgr.getBundle();
        }

        public String getFactory()
        {
            return mgr.getComponentMetadata().getFactoryIdentifier();
        }

        public boolean isServiceFactory()
        {
            return mgr.getComponentMetadata().isFactory();
        }

        public String getClassName()
        {
            return mgr.getComponentMetadata().getImplementationClassName();
        }

        public boolean isDefaultEnabled()
        {
            return mgr.getComponentMetadata().isEnabled();
        }

        public boolean isImmediate()
        {
            return mgr.getComponentMetadata().isImmediate();
        }

        public String[] getServices()
        {
            if ( mgr.getComponentMetadata().getServiceMetadata() != null )
            {
                return mgr.getComponentMetadata().getServiceMetadata().getProvides();
            }

            return null;
        }

        public Dictionary getProperties()
        {
            return new Hashtable<String, Object>(mgr.getComponentMetadata().getProperties());
        }

        public Reference[] getReferences()
        {
            final List<? extends ReferenceManager<?, ?>> refs = mgr.getReferenceManagers();
            if ( refs != null && refs.size() > 0 )
            {
                final List<Reference> list = new ArrayList<Reference>();
                for(final ReferenceManager<?, ?> rm : refs)
                {
                    list.add(new ReferenceWrapper(rm));
                }
                return list.toArray(
                        new Reference[list.size()] );
            }

            return null;
        }

        public ComponentInstance getComponentInstance()
        {
            if ( mgr instanceof SingleComponentManager<?> )
            {
                return ((SingleComponentManager<?>)mgr).getComponentInstance();
            }

            return null;
        }

        public String getActivate()
        {
            return mgr.getComponentMetadata().getActivate();
        }

        public boolean isActivateDeclared()
        {
            return mgr.getComponentMetadata().isActivateDeclared();
        }

        public String getDeactivate()
        {
            return mgr.getComponentMetadata().getDeactivate();
        }

        public boolean isDeactivateDeclared()
        {
            return mgr.getComponentMetadata().isDeactivateDeclared();
        }

        public String getModified()
        {
            return mgr.getComponentMetadata().getModified();
        }

        public String getConfigurationPolicy()
        {
            return mgr.getComponentMetadata().getConfigurationPolicy();
        }

        public String getConfigurationPid()
        {
            final List<String> pids = mgr.getComponentMetadata().getConfigurationPid();
            if ( pids != null && pids.size() > 0 )
            {
                return pids.get(0);
            }
            return null;
        }

        public boolean isConfigurationPidDeclared()
        {
            return mgr.getComponentMetadata().isConfigurationPidDeclared();
        }

        public void enable()
        {
            mgr.enable(false);
        }

        public void disable()
        {
            mgr.disable(false);
        }
    }

    private static final class ReferenceWrapper implements Reference
    {
        private final ReferenceManager<?, ?> mgr;

        public ReferenceWrapper(final ReferenceManager<?, ?> mgr)
        {
            this.mgr = mgr;
        }

        public String getName()
        {
            return mgr.getReferenceMetadata().getName();
        }

        public String getServiceName()
        {
            return mgr.getReferenceMetadata().getInterface();
        }

        public ServiceReference[] getServiceReferences()
        {
            final List<ServiceReference<?>> refs = this.mgr.getServiceReferences();
            return refs.toArray(new ServiceReference<?>[refs.size()]);
        }

        public ServiceReference<?>[] getBoundServiceReferences()
        {
            final List<ServiceReference<?>> refs = this.mgr.getServiceReferences();
            return refs.toArray(new ServiceReference<?>[refs.size()]);
        }

        public boolean isSatisfied()
        {
            return mgr.isSatisfied();
        }

        public boolean isOptional()
        {
            return this.mgr.getReferenceMetadata().isOptional();
        }

        public boolean isMultiple()
        {
            return this.mgr.getReferenceMetadata().isMultiple();
        }

        public boolean isStatic()
        {
            return this.mgr.getReferenceMetadata().isStatic();
        }

        public boolean isReluctant()
        {
            return this.mgr.getReferenceMetadata().isReluctant();
        }

        public String getTarget()
        {
            return this.mgr.getTarget();
        }

        public String getBindMethodName()
        {
            return this.mgr.getReferenceMetadata().getBind();
        }

        public String getUnbindMethodName()
        {
            return this.mgr.getReferenceMetadata().getUnbind();
        }

        public String getUpdatedMethodName()
        {
            return this.mgr.getReferenceMetadata().getUpdated();
        }

    }
}
