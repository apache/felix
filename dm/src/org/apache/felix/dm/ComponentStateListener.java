package org.apache.felix.dm;


public interface ComponentStateListener {
    public void changed(Component c, ComponentState state);
}
