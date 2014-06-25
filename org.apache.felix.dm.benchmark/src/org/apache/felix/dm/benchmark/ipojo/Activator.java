package org.apache.felix.dm.benchmark.ipojo;

import org.apache.felix.dm.benchmark.scenario.Helper;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {   
    volatile ComponentInstance m_componentInstance;
    
    @Override
    public void start(BundleContext context) throws Exception {
        Helper.debug(() -> Activator.class.getName() + ".start()");
        
        if (context.getBundle(0).getState() == Bundle.STARTING) {
            // if the fwk is starting, it means the iPojo factory may not have been started yet.
            // Anyway, we can return because the scenario controller will restart us later.
            Helper.debug(() -> Activator.class.getName() + ": fwk state=" + context.getBundle(0).getState());
            return;
        }

        // Wait for the ScenarioController, before starting the actual stress test.
        m_componentInstance = new PrimitiveComponentType()
            .setBundleContext(context)
            .setClassName(IpojoScenario.class.getName())
            .addDependency(new Dependency().setField("m_controller"))
            .setValidateMethod("start")
            .setInvalidateMethod("stop")
            .createInstance();  
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        Helper.debug(() -> Activator.class.getName() + ".stop()");
        m_componentInstance.dispose();
    }
}
