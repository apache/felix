package org.apache.felix.ipojo.test.scenarios.component;

import foo.RGB;
import foo.ipojo.IPOJOFoo;
import org.apache.felix.ipojo.annotations.Component;

@Component
@IPOJOFoo(bar="bar", rgb = RGB.RED, colors = {RGB.BLUE, RGB.RED})
public class CustomAnnotationWithEnum {

}
