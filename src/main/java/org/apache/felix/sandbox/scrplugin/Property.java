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
package org.apache.felix.sandbox.scrplugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.sandbox.scrplugin.tags.JavaTag;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Model: scr property
 */
public class Property extends AbstractDescriptorElement implements Comparable {

    private String name = "";

    private String label;

    private String description;

    private int cardinality;

    private String type = "String";

    private Object value;

    private boolean privateProperty;

    private Map options;

    Property(Log log, JavaTag tag) {
        super(log, tag);
    }

    boolean validate() {
        // might want to check name and type
        return true;
    }

    void generate(XMLWriter xw) {
        xw.printElementStart("property", true);
        xw.printAttribute("name", this.getName());
        if (!"String".equals(this.getType())) xw.printAttribute("type", this.getType());

        if (this.getValue() instanceof List) {
            xw.printElementStartClose(false);
            List values = (List) this.getValue();
            for (Iterator vi = values.iterator(); vi.hasNext();) {
                xw.println(vi.next());
            }
            xw.printElementEnd("property");
        } else if (this.getValue() != null) {
            xw.printAttribute("value", String.valueOf(this.getValue()));
            xw.printElementStartClose(true);
        }

    }

    void generateMetaTypeInfo(XMLWriter xw) {
        if (!this.isPrivateProperty()) {
            xw.printElementStart("AD", true);
            xw.printAttribute("id", this.getName());
            xw.printAttribute("type", this.getType());

            if (this.getValue() instanceof List) {
                List values = (List) this.getValue();
                StringBuffer buf = new StringBuffer();
                for (Iterator vi = values.iterator(); vi.hasNext();) {
                    if (buf.length() > 0) buf.append(",");
                    buf.append(vi.next());
                }
                xw.printAttribute("default", buf.toString());
            } else if (this.getValue() != null) {
                xw.printAttribute("default", String.valueOf(this.getValue()));
            }

            if (this.getLabel() != null) {
                xw.printAttribute("name", this.getLabel());
            } else {
                // use the name as a localizable key by default
                xw.printAttribute("name", "%" + this.getName() + ".name");
            }

            if (this.getDescription() != null) {
                xw.printAttribute("description", this.getDescription());
            } else {
                // use the name as a localizable key by default
                xw.printAttribute("description", "%" + this.getName() + ".description");
            }

            if (this.getCardinality() != 0) {
                xw.printAttribute("cardinality", String.valueOf(this.getCardinality()));
            }

            if (this.getOptions() != null) {
                xw.printElementStartClose(false);
                for (Iterator oi=this.getOptions().entrySet().iterator(); oi.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) oi.next();
                    xw.printElementStart("Option", true);
                    xw.printAttribute("value", String.valueOf(entry.getKey()));
                    xw.printAttribute("label", String.valueOf(entry.getValue()));
                    xw.printElementStartClose(true);
                }
                xw.printElementEnd("AD");
            } else {
                xw.printElementStartClose(true);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    protected void setName(String name) {
        this.name = (name != null) ? name : "";

        // default value for privateProperty field
        this.privateProperty = name.equals(Constants.SERVICE_PID)
            || name.equals(Constants.SERVICE_DESCRIPTION)
            || name.equals(Constants.SERVICE_ID)
            || name.equals(Constants.SERVICE_RANKING)
            || name.equals(Constants.SERVICE_VENDOR)
            || name.equals(ConfigurationAdmin.SERVICE_BUNDLELOCATION)
            || name.equals(ConfigurationAdmin.SERVICE_FACTORYPID);
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return this.description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    protected int getCardinality() {
        return this.cardinality;
    }

    protected void setCardinality(String cardinality) {
        if (cardinality == null) {
            // simple scalar by default
            this.cardinality = 0;
        } else if ("-".equals(cardinality)) {
            // unlimited vector
            this.cardinality = Integer.MIN_VALUE;
        } else if ("+".equals(cardinality)) {
            // unlimited array
            this.cardinality = Integer.MAX_VALUE;
        } else {
            try {
                this.cardinality = Integer.parseInt(cardinality);
            } catch (NumberFormatException nfe) {
                // default to scalar in case of conversion problem
                this.cardinality = 0;
            }
        }
    }

    protected void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    protected void setPrivateProperty(boolean privateProperty) {
        this.privateProperty = privateProperty;
    }

    protected boolean isPrivateProperty() {
        return this.privateProperty;
    }

    public String getType() {
        return this.type;
    }

    protected void setType(String type) {
        if (!StringUtils.isEmpty(type)) {
            this.type = type;
        }
    }

    public Object getValue() {
        return this.value;
    }

    protected void setValue(String value) {
        this.value = (value == null) ? "" : value;
    }

    protected void setValues(Map valueMap) {
        List values = new ArrayList();
        for (Iterator vi = valueMap.entrySet().iterator(); vi.hasNext();) {
            Map.Entry entry = (Map.Entry) vi.next();
            String key = (String) entry.getKey();
            if (key.startsWith("values")) {
                values.add(entry.getValue());
            }
        }

        if (!values.isEmpty()) {
            this.value = values;

            // assume array if set to scalar currently
            if (this.cardinality == 0) {
                this.cardinality = Integer.MAX_VALUE;
            }
        }
    }

    protected Map getOptions() {
        return this.options;
    }

    protected void setOptions(Map options) {
        this.options = options;
    }

    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }

        Property other = (Property) obj;
        return this.getName().compareTo(other.getName());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Property) {
            return this.getName().equals(((Property) obj).getName());
        }

        return false;
    }

    public int hashCode() {
        return this.getName().hashCode();
    }
}
