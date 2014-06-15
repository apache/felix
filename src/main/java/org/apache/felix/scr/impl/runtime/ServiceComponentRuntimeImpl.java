package org.apache.felix.scr.impl.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.ComponentManager;
import org.apache.felix.scr.impl.config.ReferenceManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.BoundReferenceDTO;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

public class ServiceComponentRuntimeImpl implements ServiceComponentRuntime {
	
	private static final String[] EMPTY = {};
	
	private final BundleContext context;
	private final ComponentRegistry componentRegistry;


	public ServiceComponentRuntimeImpl(BundleContext context,
			ComponentRegistry componentRegistry) {
		this.context = context;
		this.componentRegistry = componentRegistry;
	}

	public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(
			Bundle... bundles) {
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

	public ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle,
			String name) {
		ComponentHolder<?> holder = componentRegistry.getComponentHolder(bundle, name);
		return holderToDescription(holder);
	}

	public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(
			ComponentDescriptionDTO description) {
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

	public boolean isComponentEnabled(ComponentDescriptionDTO description) {
		ComponentHolder<?> holder = getHolderFromDescription( description);
		return holder.isEnabled();
	}

	public void enableComponent(ComponentDescriptionDTO description) {
		ComponentHolder<?> holder = getHolderFromDescription( description);
		holder.enableComponents(false); //synchronous
	}

	public void disableComponent(ComponentDescriptionDTO description) {
		ComponentHolder<?> holder = getHolderFromDescription( description);
		holder.disableComponents(false); //synchronous
	}
	
	private ComponentConfigurationDTO managerToConfiguration(
			ComponentManager<?> manager, ComponentDescriptionDTO description) {
		ComponentConfigurationDTO dto = new ComponentConfigurationDTO();
		dto.boundReferences = refManagersToDTO(manager.getReferenceManagers());
		dto.description = description;
		dto.id = manager.getId();
		dto.properties = new HashMap<String, Object>(manager.getProperties());//TODO deep copy?
		dto.state = manager.getState();
		return dto;
	}

	private BoundReferenceDTO[] refManagersToDTO(List<? extends ReferenceManager<?, ?>> referenceManagers) {
		BoundReferenceDTO[] dtos = new BoundReferenceDTO[referenceManagers.size()];
		for (ReferenceManager<?, ?> ref: referenceManagers)
		{
			BoundReferenceDTO dto = new BoundReferenceDTO();
			dto.name = ref.getName();
			dto.target = ref.getTarget();
			List<ServiceReference<?>> serviceRefs = ref.getServiceReferences();
			ServiceReferenceDTO[] srDTOs = new ServiceReferenceDTO[serviceRefs.size()];
			int i = 0;
			for (ServiceReference<?> serviceRef: serviceRefs)
			{
				srDTOs[i++] = serviceReferenceToDTO(serviceRef);
			}
			dto.serviceReferences = srDTOs;
		}
		return dtos;
	}

	private ServiceReferenceDTO serviceReferenceToDTO(
			ServiceReference<?> serviceRef) {
		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		dto.bundle = serviceRef.getBundle().getBundleId();
		dto.id = (Long) serviceRef.getProperty("service.id"); //TODO use proper constant
		//TODO service properties, using bundles
		return dto;
	}

	private ComponentHolder getHolderFromDescription(
			ComponentDescriptionDTO description) {
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
		dto.properties = new HashMap<String, Object>(m.getProperties());// TODO deep copy of arrays
		dto.references = refsToDTO(m.getDependencies());
		dto.scope = m.getServiceMetadata() == null? null: m.getServiceMetadata().getScope().name();
		dto.serviceInterfaces = m.getServiceMetadata() == null? EMPTY: m.getServiceMetadata().getProvides();
		return dto;
	}

	private ReferenceDTO[] refsToDTO(List<ReferenceMetadata> dependencies) {
		ReferenceDTO[] dtos = new ReferenceDTO[dependencies.size()];
		int i = 0;
		for (ReferenceMetadata r: dependencies)
		{
			ReferenceDTO dto = new ReferenceDTO();
			dto.bind = r.getBind();
			dto.cardinality = r.getCardinality();
			dto.interfaceName = r.getInterface();
			dto.name = r.getName();
			dto.policy = r.getPolicy();
			dto.policyOption = r.getPolicyOption();
			dto.scope = r.getScope().name();
			dto.target = r.getTarget();
			dto.unbind = r.getUnbind();
			dto.updated = r.getUpdated();
			dtos[i] = dto;
		}
		return dtos;
	}

	private BundleDTO bundleToDTO(BundleContext bundleContext) {
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

}
