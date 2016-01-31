package org.apache.felix.dm.lambda.samples.future;

import java.util.List;

/**
 * Service that displays all links found from a given web page.
 */
public interface PageLinks {
    List<String> getLinks();
}
