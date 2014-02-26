package dm;


/**
 * A dependency. Can be added to a single component. Can be available, or not.
 */
public interface Dependency {
	public Dependency setCallbacks(String add, String remove);
	public Dependency setCallbacks(String add, String change, String remove);
	public Dependency setCallbacks(Object instance, String add, String remove);
	public Dependency setCallbacks(Object instance, String add, String change, String remove);
	public Dependency setRequired(boolean required);
	public Dependency setAutoConfig(boolean autoConfig);
    public Dependency setAutoConfig(String instanceName);
}
