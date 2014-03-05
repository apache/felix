package dm.admin;

import java.util.List;

import org.osgi.framework.Bundle;

/**
 * Service for administering Dependency Manager components.
 * The main purpose of this interface is to provide access to the components declared by all 
 * dependency manager instances at runtime.
 */
public interface ComponentAdmin {
	/**
	 * Returns components declared by all dependency manager instances. 
	 * @return the list of components declared by all dependency managers instances.
	 */
	List<ComponentDeclaration> getComponents();

	/**
	 * Returns the component matching a given id. 
	 * @return the component matching a given id or null.
	 */
	ComponentDeclaration getComponent(long componentId);

	/**
	 * Returns the components declared by dependency managers being part of a given bundle.
	 * @return the components declared by dependency managers being part of a given bundle.
	 */
	List<ComponentDeclaration> getComponents(Bundle bundle);
}
