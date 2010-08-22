package org.apache.felix.ipojo.test.scenarios.component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.felix.ipojo.test.scenarios.component.Marker.Type;

public class Annotation {
    
    @Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}
    )
    @SubMarker(subname="bar")
    @Invisible
    public void doSomething() {
        System.out.println("Foo ...");
    }
    
    @Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}
    )
    @SubMarker(subname="bar")
    @Invisible
    public Annotation() {
        
    }
    
    public void doSomethingWithParams(@Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}) String foo, 
            @Invisible String bar, 
            @bla @SubMarker(subname = "baz") String baz) {
        System.out.println("Foo ...");
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface bla {
        
    }

}
