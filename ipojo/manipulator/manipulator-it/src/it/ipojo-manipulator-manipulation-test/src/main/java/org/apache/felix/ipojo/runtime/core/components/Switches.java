/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
