package dm;

import java.net.URL;
import java.util.Dictionary;

/** 
 * Service interface for anybody wanting to be notified of changes to resources. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ResourceHandler {
    /** Name of the property that's used to describe the filter condition for a resource. */
    public static final String FILTER = "filter";
    /** Exact URL that this handler is looking for. Can be used instead of a filter to be very explicit about the resource you're looking for. */
    public static final String URL = "url";
    /** The host part of the URL. */
    public static final String HOST = "host";
    /** The path part of the URL. */
    public static final String PATH = "path";
    /** The protocol part of the URL. */
    public static final String PROTOCOL = "protocol";
    /** The port part of the URL. */
    public static final String PORT = "port";

    /**
     * @deprecated Please use {@link #added(URL, Dictionary)} instead. When both are specified,
     *     the new method takes precedence and the deprecated one is not invoked.
     */
    public void added(URL resource);
    
    /**
     * Invoked whenever a new resource is added.
     */
    public void added(URL resource, Dictionary resourceProperties);
    
    /**
     * @deprecated Please use {@link #changed(URL, Dictionary)} instead. When both are specified,
     *     the new method takes precedence and the deprecated one is not invoked.
     */
    public void changed(URL resource);
    
    /**
     * Invoked whenever an existing resource changes.
     */
    public void changed(URL resource, Dictionary resourceProperties);
    
    /**
     * @deprecated Please use {@link #removed(URL, Dictionary)} instead. When both are specified,
     *     the new method takes precedence and the deprecated one is not invoked.
     */
    public void removed(URL resource);
    
    /**
     * Invoked whenever an existing resource is removed.
     */
    public void removed(URL resource, Dictionary resourceProperties);
}

