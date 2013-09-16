package org.apache.felix.ipojo.runtime.core.components.nativ;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.services.BazService;

/**
 * Component using a native method.
 */
@Component
@Provides
public class NativeComponent implements BazService {

    static {
        System.loadLibrary("foo");
    }

    /**
     * Provided by the native library.
     * @return <code>foo: Test program of JNI.</code>
     */
    public native String nativeFoo();

    @Override
    public String hello(String name) {
        return nativeFoo();
    }
}
