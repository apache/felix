package test;

import org.apache.felix.ipojo.annotations.*;
import test.ipojo.ExternalHandler;

import java.util.List;

@Component
@Instantiate
public class PlentyOfAnnotations {

    @Requires
    List list;
    private String m_prop;
    private Runnable m_runnable;
    private String m_stuff;

    @ExternalHandler
    private String stuff2;


    PlentyOfAnnotations(@Property String prop, @Requires Runnable runnable, @ExternalHandler String stuff) {

        m_prop = prop;
        m_runnable = runnable;
        m_stuff = stuff;

    }

    @Validate
    public void start() {
        //...
    }

    @ExternalHandler
    public void stuff() {
        // ...
    }

}
