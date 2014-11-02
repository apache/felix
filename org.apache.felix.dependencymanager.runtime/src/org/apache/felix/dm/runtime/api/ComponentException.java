package org.apache.felix.dm.runtime.api;

/**
 * Exception thrown when a Component can't be instantiated using a {@link ComponentFactory#newInstance(java.util.Dictionary)} 
 * service.
 *
 */
@SuppressWarnings("serial")
public class ComponentException extends RuntimeException {
    public ComponentException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
