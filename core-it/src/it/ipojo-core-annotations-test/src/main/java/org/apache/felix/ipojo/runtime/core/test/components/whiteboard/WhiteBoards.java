package org.apache.felix.ipojo.runtime.core.test.components.whiteboard;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.ServiceReference;

@Component
@Whiteboards(whiteboards={
		@Wbp(filter="(foo=true)", onArrival="onArrival", onDeparture="onDeparture"),
		@Wbp(filter="(foo=true)", onArrival="onArrival", onDeparture="onDeparture", onModification="onModification")
	})
public class WhiteBoards {

    public void onArrival(ServiceReference ref) {
        // nothing
    }

    public void onDeparture(ServiceReference ref) {
        // nothing
    }

    public void onModification(ServiceReference ref) {
        // nothing
    }

}
