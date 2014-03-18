package dm;

import org.osgi.framework.Bundle;

import dm.admin.ComponentDependencyDeclaration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface BundleDependency extends Dependency, ComponentDependencyDeclaration {
    /**
     * Sets the callbacks for this dependency. These callbacks can be used as hooks whenever a dependency is added or removed.
     * When you specify callbacks, the auto configuration feature is automatically turned off, because we're assuming you don't
     * need it in this case.
     * 
     * @param added the method to call when a bundle was added
     * @param removed the method to call when a bundle was removed
     * @return the bundle dependency
     */
    public BundleDependency setCallbacks(String added, String removed);

    /**
     * Sets the callbacks for this dependency. These callbacks can be used as hooks whenever a dependency is added, changed or
     * removed. When you specify callbacks, the auto configuration feature is automatically turned off, because we're assuming
     * you don't need it in this case.
     * 
     * @param added the method to call when a bundle was added
     * @param changed the method to call when a bundle was changed
     * @param removed the method to call when a bundle was removed
     * @return the bundle dependency
     */
    public BundleDependency setCallbacks(String added, String changed, String removed);

    /**
     * Sets the callbacks for this dependency. These callbacks can be used as hooks whenever a dependency is added or removed.
     * They are called on the instance you provide. When you specify callbacks, the auto configuration feature is automatically
     * turned off, because we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a bundle was added
     * @param removed the method to call when a bundle was removed
     * @return the bundle dependency
     */
    public BundleDependency setCallbacks(Object instance, String added, String removed);

    /**
     * Sets the callbacks for this dependency. These callbacks can be used as hooks whenever a dependency is added, changed or
     * removed. They are called on the instance you provide. When you specify callbacks, the auto configuration feature is
     * automatically turned off, because we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a bundle was added
     * @param changed the method to call when a bundle was changed
     * @param removed the method to call when a bundle was removed
     * @return the bundle dependency
     */
    public BundleDependency setCallbacks(Object instance, String added, String changed, String removed);

    /**
     * Enables auto configuration for this dependency. This means the component implementation (composition) will be
     * injected with this bundle dependency automatically.
     * 
     * @param autoConfig <code>true</code> to enable auto configuration
     * @return the bundle dependency
     */
    public BundleDependency setAutoConfig(boolean autoConfig);

    /**
     * Sets the dependency to be required.
     * 
     * @param required <code>true</code> if this bundle dependency is required
     * @return the bundle dependency
     */
    public BundleDependency setRequired(boolean required);

    /**
     * Sets the bundle to depend on directly.
     * 
     * @param bundle the bundle to depend on
     * @return the bundle dependency
     */
    public BundleDependency setBundle(Bundle bundle);

    /**
     * Sets the filter condition to depend on. Filters are matched against the full manifest of a bundle.
     * 
     * @param filter the filter condition
     * @return the bundle dependency
     * @throws IllegalArgumentException
     */
    public BundleDependency setFilter(String filter) throws IllegalArgumentException;

    /**
     * Sets the bundle state mask to depend on. The OSGi BundleTracker explains this mask in more detail, but
     * it is basically a mask with flags for each potential state a bundle can be in.
     * 
     * @param mask the mask to use
     * @return the bundle dependency
     */
    public BundleDependency setStateMask(int mask);

    /**
     * Sets property propagation. If set to <code>true</code> any bundle manifest properties will be added
     * to the service properties of the component that has this dependency (if it registers as a service).
     * 
     * @param propagate <code>true</code> to propagate the bundle manifest properties
     * @return the bundle dependency
     */
    public BundleDependency setPropagate(boolean propagate);
    
    /**
     * Sets an Object instance and a callback method used to propagate some properties to the provided service properties.
     * The method will be invoked on the specified object instance and must have one of the following signatures:<p>
     * <ul><li>Dictionary callback(ServiceReference, Object service) 
     * <li>Dictionary callback(ServiceReference)
     * </ul>
     * @param instance the Object instance which is used to retrieve propagated service properties 
     * @param method the method to invoke for retrieving the properties to be propagated to the service properties.
     * @return this service dependency.
     */
    public BundleDependency setPropagate(Object instance, String method);
}
