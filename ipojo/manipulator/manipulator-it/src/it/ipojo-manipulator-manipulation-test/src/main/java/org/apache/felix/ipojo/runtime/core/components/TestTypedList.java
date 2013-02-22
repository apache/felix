package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.util.List;
import java.util.Properties;

public class TestTypedList implements CheckService {

    private List<FooService> list;

    public boolean check() {
        return !list.isEmpty();
    }

    public Properties getProps() {
        Properties props = new Properties();
        if (list != null) {
            props.put("list", list);

            int i = 0;
            for (FooService fs : list) {
                props.put(i, fs.foo());
                i++;
            }
        }

        return props;
    }

}
