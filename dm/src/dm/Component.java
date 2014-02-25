package dm;


/**
 * A component. Has dependencies. 
 */
public interface Component {
	public Component setImplementation(Object implementation);
	public Component add(Dependency d);
	public Component remove(Dependency d);
	public Component add(ComponentStateListener l);
	public Component remove(ComponentStateListener l);
}
