package dm;

import java.util.Dictionary;

/**
 * A dependency. Can be added to a single component. Can be available, or not.
 */
public interface Dependency {
    public boolean isRequired();
    public boolean isAvailable();
    public boolean isAutoConfig();
    public String getAutoConfigName();
    public boolean isPropagated();
    public Dictionary getProperties();
}
