package org.apache.felix.dm;


public interface ConfigurationDependency extends Dependency, ComponentDependencyDeclaration {
	ConfigurationDependency setCallback(String callback);
	ConfigurationDependency setPid(String pid);
	ConfigurationDependency setPropagate(boolean propagate);
	ConfigurationDependency setHeading(String heading);
	ConfigurationDependency setDescription(String description);
	ConfigurationDependency setLocalization(String path);
	ConfigurationDependency add(PropertyMetaData properties);
}
