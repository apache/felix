package dm.impl;

import dm.context.Event;

/* in real life, this event might contain a service reference and service instance
 * or something similar
 */
public class EventImpl implements Event, Comparable {
	private final int m_id;

	public EventImpl() {
		this(1);
	}
	/** By constructing events with different IDs, we can simulate different unique instances. */
	public EventImpl(int id) {
		m_id = id;
	}

	@Override
	public int hashCode() {
		return m_id;
	}

	@Override
	public boolean equals(Object obj) {
		// an instanceof check here is not "strong" enough with subclasses overriding the
		// equals: we need to be sure that a.equals(b) == b.equals(a) at all times
		if (obj != null && obj.getClass().equals(EventImpl.class)) {
			return ((EventImpl) obj).m_id == m_id;
		}
		return false;
	}
	
    @Override
    public int compareTo(Object o) {
        EventImpl a = this, b = (EventImpl) o;
        if (a.m_id < b.m_id) {
            return -1;
        } else if (a.m_id == b.m_id){
            return 0;
        } else {
            return 1;
        }
    }
}
