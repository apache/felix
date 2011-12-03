package test;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;

/**
 * Created by IntelliJ IDEA.
 * User: guillaume
 * Date: 10/08/11
 * Time: 21:56
 * To change this template use File | Settings | File Templates.
 */
@Component
public class AnnotatedComponent {

    @Property
    private String prop;
}
