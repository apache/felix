package org.apache.felix.dm.lambda.callbacks;

import java.io.Serializable;

/**
 * Base interface for serializable lambdas. Some lambda must be serializable in order to allow to introspect their type and method signatures.
 */
public interface SerializableLambda extends Serializable {
}
