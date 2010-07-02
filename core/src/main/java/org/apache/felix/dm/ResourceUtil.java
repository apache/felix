package org.apache.felix.dm;

import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;

public class ResourceUtil {
    public static Dictionary createProperties(URL url) {
        Properties props = new Properties();
        props.setProperty(ResourceHandler.PROTOCOL, url.getProtocol());
        props.setProperty(ResourceHandler.HOST, url.getHost());
        props.setProperty(ResourceHandler.PORT, Integer.toString(url.getPort()));
        props.setProperty(ResourceHandler.PATH, url.getPath());
        return props;
    }
}
