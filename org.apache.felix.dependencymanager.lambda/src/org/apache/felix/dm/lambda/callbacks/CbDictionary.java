package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

/**
 * Represents a callback(Dictionary) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbDictionary {
    /**
     * Handles the given argument.
     * @param conf the properties
     */
    void accept(Dictionary<String, Object> conf);

    default CbDictionary andThen(CbDictionary after) {
        Objects.requireNonNull(after);
        return (Dictionary<String, Object> conf) -> {
            accept(conf);
            after.accept(conf);
        };
    }
}
