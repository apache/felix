package org.apache.felix.dependencymanager.samples.customdep;

import org.apache.felix.dm.Dependency;

/**
 * A custom Dependency Manager Path Dependency that can track a path directory.
 * When a file is added or removed from the path dir, then the component is called
 * in the corresponding add/remove callback.
 */
public interface PathDependency extends Dependency {
    PathDependency setRequired(boolean required);
    PathDependency setCallbacks(String add, String change, String remove);
}
