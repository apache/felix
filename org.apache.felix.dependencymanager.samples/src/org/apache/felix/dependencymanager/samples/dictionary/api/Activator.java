package org.apache.felix.dependencymanager.samples.dictionary.api;

import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {
        // Create the factory configuration for our DictionaryImpl service.
        dm.add(createFactoryConfigurationAdapterService(DictionaryConfiguration.class.getName(), "updated", true)
            .setInterface(DictionaryService.class.getName(), null)
            .setImplementation(DictionaryImpl.class)
            .add(createServiceDependency().setService(LogService.class))); // NullObject 
        
        // Create the Dictionary Aspect
        dm.add(createAspectService(DictionaryService.class, "(lang=en)", 10)
            .setImplementation(DictionaryAspect.class)
            .add(createConfigurationDependency().setPid(DictionaryAspectConfiguration.class.getName()))
            .add(createServiceDependency().setService(LogService.class))); // NullObject
        
        // Create the SpellChecker component
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandProcessor.COMMAND_SCOPE, "dictionary");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "spellcheck" });
        dm.add(createComponent()
            .setImplementation(SpellChecker.class)
            .setInterface(SpellChecker.class.getName(), props)
            .add(createServiceDependency().setService(DictionaryService.class).setRequired(true))
            .add(createServiceDependency().setService(LogService.class))); // NullObject
    }
}
