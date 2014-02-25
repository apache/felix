package dm;

import dm.context.ComponentState;

public interface ComponentStateListener {
	public void changed(ComponentState state);
}
