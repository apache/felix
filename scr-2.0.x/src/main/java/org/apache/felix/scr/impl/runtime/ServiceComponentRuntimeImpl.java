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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.ComponentManager;
import org.apache.felix.scr.impl.manager.ReferenceManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

public class ServiceComponentRuntimeImpl implements ServiceComponentRuntime
{

	private static final String[] EMPTY = {};

	private final BundleContext context;
	private final ComponentRegistry componentRegistry;


	public ServiceComponentRuntimeImpl(BundleContext context,ComponentRegistry componentRegistry)
	{
		this.context = context;
		this.componentRegistry = componentRegistry;
	}

	/**
	 * @see org.osgi.service.component.runtime.ServiceComponentRuntime#getComponentDescriptionDTOs(org.osgi.framework.Bundle[])
	 */
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
		    ComponentDescriptionDTO dto = holderToDescription(holder);
		    if ( dto != null )
		    {
		        result.add(dto);
		    }
		}
		return result;
	}

	/**
	 * @see org.osgi.service.component.runtime.ServiceComponentRuntime#getComponentDescriptionDTO(org.osgi.framework.Bundle, java.lang.String)
	 */
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

	/**
	 * @see org.osgi.service.component.runtime.ServiceComponentRuntime#getComponentConfigurationDTOs(org.osgi.service.component.runtime.dto.ComponentDescriptionDTO)
	 */
	public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description)
	{
		if ( description == null)
		{
			return Collections.emptyList();
		}
		try
		{
    		ComponentHolder<?> holder = getHolderFromDescription( description);
    		// Get a fully filled out valid description DTO
    		description = holderToDescription(holder);
            if ( description == null)
            {
                return Collections.emptyList();
            }
    		List<? extends ComponentManager<?>> managers = holder.getComponents();
    		List<ComponentConfigurationDTO> result = new ArrayList<ComponentConfigurationDTO>(managers.size());
    		for (ComponentManager<?> manager: managers)
    		{
    			result.add(managerToConfiguration(manager, description));
    		}
    		return result;
		}
		catch ( IllegalStateException ise)
		{
		    return Collections.emptyList();
		}
	}

	/**
	 * @see org.osgi.service.component.runtime.ServiceComponentRuntime#isComponentEnabled(org.osgi.service.component.runtime.dto.ComponentDescriptionDTO)
	 */
	public boolean isComponentEnabled(ComponentDescriptionDTO description)
	{
        try
        {
    		ComponentHolder<?> holder = getHolderFromDescription( description);
	    	return holder.isEnabled();
        }
        catch ( IllegalStateException ise)
        {
            return false;
        }
	}

	/**
	 * @see org.osgi.service.component.runtime.ServiceComponentRuntime#enableComponent(org.osgi.service.component.runtime.dto.ComponentDescriptionDTO)
	 */
	public Promise<Void> enableComponent(ComponentDescriptionDTO description)
	{
        try
        {
            ComponentHolder<?> holder = getHolderFromDescription( description);
            return holder.enableComponents(true);
        }
        catch ( IllegalStateException ise)
        {
            return Promises.failed(ise);
        }
	}

	/**
	 * @see org.osgi.service.component.runtime.ServiceComponentRuntime#disableComponent(org.osgi.service.component.runtime.dto.ComponentDescriptionDTO)
	 */
	public Promise<Void> disableComponent(ComponentDescriptionDTO description)
	{
        try
        {
            ComponentHolder<?> holder = getHolderFromDescription( description);
            return holder.disableComponents(true); //synchronous
        }
        catch ( IllegalStateException ise)
        {
            return Promises.failed(ise);
        }
	}

	private ComponentConfigurationDTO managerToConfiguration(ComponentManager<?> manager, ComponentDescriptionDTO description)
	{
		ComponentConfigurationDTO dto = new ComponentConfigurationDTO();
        dto.satisfiedReferences = satisfiedRefManagersToDTO(manager.getReferenceManagers());
        dto.unsatisfiedReferences = unsatisfiedRefManagersToDTO(manager.getReferenceManagers());
		dto.description = description;
		dto.id = manager.getId();
		dto.properties = new HashMap<String, Object>(manager.getProperties());//TODO deep copy?
		dto.state = manager.getSpecState();
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
                    ServiceReferenceDTO srefDTO = serviceReferenceToDTO(serviceRef);
                    if (srefDTO != null)
                        srDTOs[j++] = srefDTO;
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
                    ServiceReferenceDTO srefDTO = serviceReferenceToDTO(serviceRef);
                    if (srefDTO != null)
                        srDTOs[j++] = srefDTO;
                }
                dto.targetServices = srDTOs;
                dtos.add(dto);
            }
        }
        return dtos.toArray( new UnsatisfiedReferenceDTO[dtos.size()] );
    }

	private ServiceReferenceDTO serviceReferenceToDTO( ServiceReference<?> serviceRef)
	{
	    if (serviceRef == null)
	        return null;

		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		Bundle bundle = serviceRef.getBundle();
		if (bundle != null)
		    dto.bundle = bundle.getBundleId();
		else
		    dto.bundle = -1; // No bundle ever has -1 as ID, so this indicates no bundle.

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

	/**
	 * Return the component holder
	 * @param description Component description DTO
	 * @return The component holder
	 * @throws IllegalStateException If the bundle is not active anymore
	 */
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
		// immediately return if bundle is not active anymore
		if ( dto.bundle == null )
		{
		    return null;
		}
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
		try
        {
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
        catch (IllegalStateException e)
        {
            return null;
        }
	}
}
