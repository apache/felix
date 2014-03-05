package dm.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;

import dm.Component;
import dm.DependencyManager;
import dm.admin.ComponentAdmin;
import dm.admin.ComponentDeclaration;

public class ComponentAdminImpl implements ComponentAdmin {
    /**
     * Sorter used to sort components.
     */
    private static final DependencyManagerSorter SORTER = new DependencyManagerSorter();

    private static class DependencyManagerSorter implements Comparator<DependencyManager> {
        public int compare(DependencyManager dm1, DependencyManager dm2) {
            long id1 = dm1.getBundleContext().getBundle().getBundleId();
            long id2 = dm2.getBundleContext().getBundle().getBundleId();
            return id1 > id2 ? 1 : -1;
        }
    }

    @Override
	public List<ComponentDeclaration> getComponents() {
    	return getComponents(null, -1);
	}

	@Override
	public ComponentDeclaration getComponent(long componentId) {
    	List<ComponentDeclaration> result = getComponents(null, componentId);
    	return result.size() > 0 ? result.get(0) : null;
	}

	@Override
	public List<ComponentDeclaration> getComponents(Bundle bundle) {
    	return getComponents(bundle);
	}
	
	private List<ComponentDeclaration> getComponents(Bundle bundle, long componentId) {
		List<ComponentDeclaration> result = new ArrayList();
        List<DependencyManager> managers = DependencyManager.getDependencyManagers();
        Collections.sort(managers, SORTER);
        Iterator<DependencyManager> iterator = managers.iterator();
        
        while (iterator.hasNext()) {
            DependencyManager manager = iterator.next();
            List<Component> complist = manager.getComponents();
            Iterator<Component> componentIterator = complist.iterator();
            
            while (componentIterator.hasNext()) {
                Component component = componentIterator.next();
                ComponentDeclaration sc = (ComponentDeclaration) component;
                if (bundle != null && ! sc.getBundleContext().getBundle().equals(bundle)) {
                	continue;
                }
                if (componentId != -1 && sc.getId() != componentId) {
                	continue;
                }
                result.add(sc);
            }
        }
        
        return result;
	}
}
