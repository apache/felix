package org.apache.felix.ipojo.log.handler.example;


import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.foo.FooService;
import org.apache.felix.ipojo.log.handler.Log;

@Component(immediate = true)
@Log(level = Log.Level.INFO) // We configure the handler.
@Instantiate(name = "my.simple.consumer")
public class SimpleComponent {

    @Requires
    FooService fs;

    @Validate
    public void starting() {
        System.out.println("Starting...");
        fs.foo();
    }

    @Invalidate
    public void stopping() {
        System.out.println("Stopping...");
    }
}
