package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.Color;

import java.util.Properties;

/**
 * A component checking switch construct with integer and enumeration.
 */
public class Switches implements CheckService {

    private static enum Stuff {
        FOO,
        BAR
    }

    private String switchOnInteger(int i) {
        switch (i) {
            case 0:
                return "0";
            case 1:
                return "1";
            case 2:
                return "2";
            default:
                return "3";
        }
    }

    private String switchOnEnum(Color color) {
        switch (color) {
            case RED:
                return "RED";
            case GREEN:
                return "GREEN";
            case BLUE:
                return "BLUE";
            default:
                return "COLOR";
        }
    }

    private String switchOnStuff(Stuff stuff) {
        switch (stuff) {
            case FOO : return "foo";
            case BAR : return "bar";
            default: return "";
        }
    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public Properties getProps() {
        Properties properties = new Properties();
        properties.put("switchOnInteger1", switchOnInteger(1));
        properties.put("switchOnInteger4", switchOnInteger(4));

        properties.put("switchOnEnumRed", switchOnEnum(Color.RED));

        properties.put("switchOnStuffFoo", switchOnStuff(Stuff.FOO));

        return properties;
    }
}
