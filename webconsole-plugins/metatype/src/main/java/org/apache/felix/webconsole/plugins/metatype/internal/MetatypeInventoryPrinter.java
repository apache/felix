/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole.plugins.metatype.internal;

import java.io.PrintWriter;
import java.util.Hashtable;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * the {@code MetaTypeInve}
 */
class MetatypeInventoryPrinter implements InventoryPrinter
{

    private final BundleContext bundleContext;
    private final MetaTypeService metatype;

    private ServiceRegistration registration;

    @SuppressWarnings("serial")
    MetatypeInventoryPrinter(final BundleContext bundleContext, final MetaTypeService metatype)
    {
        this.bundleContext = bundleContext;
        this.metatype = metatype;

        this.registration = bundleContext.registerService(InventoryPrinter.SERVICE, this,
                new Hashtable<String, Object>()
        {
            {
                put(InventoryPrinter.NAME, "metatype");
                put(InventoryPrinter.TITLE, "Metatype Service");
                put(InventoryPrinter.FORMAT, new String[]
                        { Format.TEXT.toString(), Format.JSON.toString() });
            }
        });
    }

    void unregister()
    {
        ServiceRegistration registration = this.registration;
        this.registration = null;
        if (registration != null)
        {
            registration.unregister();
        }
    }

    public String getTitle()
    {
        return "Metatype Service";
    }

    public void print(PrintWriter printWriter, Format format, boolean isZip)
    {
        final Bundle[] bundles = this.bundleContext.getBundles();
        final Printer printer = (format == Format.JSON) ? new JsonPrinter(printWriter) : new TextPrinter(printWriter);

        printer.start();
        for (Bundle bundle : bundles)
        {
            printComponents(printer, bundle, metatype.getMetaTypeInformation(bundle));
        }
        printer.end();
    }

    private static final void printComponents(final Printer pw, final Bundle bundle, final MetaTypeInformation info)
    {
        if (info == null)
        {
            return;
        }

        final String[] pids = info.getPids();
        final String[] factoryPids = info.getFactoryPids();
        if ((pids == null || pids.length == 0) && (factoryPids == null || factoryPids.length == 0))
        {
            return;
        }

        pw.group(bundle.getSymbolicName() + " (" + bundle.getBundleId() + ")");

        // PIDs
        if (pids != null && pids.length > 0)
        {
            for (String pid : pids)
            {
                ocd(pw, info.getObjectClassDefinition(pid, null), pid);
            }
        }

        // Factory PIDs
        if (factoryPids != null && factoryPids.length > 0)
        {
            for (String factoryPid : factoryPids)
            {
                ocd(pw, info.getObjectClassDefinition(factoryPid, null), factoryPid);
            }
        }

        pw.endGroup();
    }

    private static final void ocd(final Printer pw, final ObjectClassDefinition ocd, String id)
    {
        pw.group(id);
        pw.keyValue("name", ocd.getName());
        pw.keyValue("description", ocd.getDescription());

        AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        if (ads != null)
        {
            pw.group("attributes");
            for (AttributeDefinition ad : ads)
            {
                ad(pw, ad);
            }
            pw.endGroup();
        }

        pw.endGroup();
    }

    private static final void ad(final Printer pw, final AttributeDefinition ad)
    {
        pw.group(ad.getID());
        pw.keyValue("name", ad.getName());
        pw.keyValue("description", ad.getDescription());
        pw.keyValue("type", type(ad.getType()));
        pw.keyValue("cardinality", cardinality(ad.getCardinality()));
        defaultValue(pw, ad.getDefaultValue());
        options(pw, ad.getOptionLabels(), ad.getOptionValues());
        pw.endGroup();
    }

    private static final void defaultValue(final Printer pw, final String[] defaultValue)
    {
        if (defaultValue != null)
        {
            switch (defaultValue.length)
            {
            case 0: // ignore
                break;

            case 1:
                pw.keyValue("default", defaultValue[0]);
                break;

            default:
                pw.list("default", defaultValue);
                break;
            }
        }
    }

    private static final void options(final Printer pw, final String[] optionLabels, final String[] optionValues)
    {
        if (optionLabels != null && optionLabels.length > 0)
        {
            pw.group("options");
            for (int i = 0; i < optionLabels.length; i++)
            {
                pw.keyValue(optionLabels[i], optionValues[i]);
            }
            pw.endGroup();
        }
    }

    @SuppressWarnings("deprecation")
    private static final String type(final int type)
    {
        switch (type)
        {
        case AttributeDefinition.BIGDECIMAL:
            return "BigDecimal";
        case AttributeDefinition.BIGINTEGER:
            return "BigInteger";
        case AttributeDefinition.BOOLEAN:
            return "Boolean";
        case AttributeDefinition.BYTE:
            return "Byte";
        case AttributeDefinition.CHARACTER:
            return "Character";
        case AttributeDefinition.DOUBLE:
            return "Double";
        case AttributeDefinition.FLOAT:
            return "Float";
        case AttributeDefinition.INTEGER:
            return "Integer";
        case AttributeDefinition.LONG:
            return "Long";
        case AttributeDefinition.SHORT:
            return "Short";
        case AttributeDefinition.STRING:
            return "String";
        case 12 /* PASSWORD */:
            return "Password";
        default:
            return String.valueOf(type);
        }
    }

    private static final String cardinality(final int cardinality)
    {
        if (cardinality == 0)
        {
            return "required";
        }
        else if (cardinality == Integer.MAX_VALUE || cardinality == Integer.MIN_VALUE)
        {
            return "unlimited";
        }
        else
        {
            return String.valueOf(Math.abs(cardinality));
        }
    }

    private static interface Printer
    {
        void start();

        void end();

        void group(String name);

        void endGroup();

        void list(String name, String[] values);

        void keyValue(String key, String value);

    }

    private static class TextPrinter implements Printer
    {

        private final PrintWriter pw;

        private String indent;

        TextPrinter(final PrintWriter pw)
        {
            this.pw = pw;

            this.indent = "";
        }

        public void start()
        {
        }

        public void end()
        {
        }

        public void group(String name)
        {
            this.pw.printf("%s%s:%n", indent, name);
            this.indent += "  ";
        }

        public void endGroup()
        {
            if (this.indent.length() > 2)
            {
                this.indent = this.indent.substring(0, this.indent.length() - 2);
            }
        }

        public void list(String name, String[] values)
        {
            this.pw.printf("%s%s: [", indent, name);
            boolean first = true;
            for (String val : values)
            {
                if (first) {
                    first = false;
                } else {
                    this.pw.print(", ");
                }
                this.pw.print(val);
            }
            this.pw.println("]");
        }

        public void keyValue(String key, String value)
        {
            this.pw.printf("%s%s: %s%n", indent, key, value);
        }
    }

    private static class JsonPrinter implements Printer
    {
        private boolean needsComma;

        private final PrintWriter pw;

        JsonPrinter(final PrintWriter pw)
        {
            this.pw = pw;
        }

        public void start()
        {
            this.pw.print('{');
        }

        public void end()
        {
            this.pw.print('}');
        }

        private void key(String name) {
            if (needsComma) {
                this.pw.print(',');
            }
            this.pw.print('"');
            this.pw.print(name);
            this.pw.print("\" : ");
        }

        public void group(String name)
        {
            this.key(name);
            this.pw.print('{');
            this.needsComma = false;
        }

        public void endGroup()
        {
            this.pw.print('}');
            this.needsComma = true;
        }

        public void list(String name, String[] values)
        {
            this.key(name);
            this.pw.print('[');
            boolean first = true;
            for (String val : values) {
                if (first) {
                    first = false;
                } else {
                    this.pw.print(',');
                }
                value(val);
            }
            this.pw.print(']');
            this.needsComma = true;
        }

        private void value(String value)
        {
            if (value == null) {
                this.pw.print("null");
            } else {
                this.pw.print('"');
                // escape the value
                final int len = value.length();
                for(int i=0;i<len;i++){
                    final char c = value.charAt(i);
                    switch(c){
                    case '"':
                        this.pw.print("\\\"");
                        break;
                    case '\\':
                        this.pw.print("\\\\");
                        break;
                    case '\b':
                        this.pw.print("\\b");
                        break;
                    case '\f':
                        this.pw.print("\\f");
                        break;
                    case '\n':
                        this.pw.print("\\n");
                        break;
                    case '\r':
                        this.pw.print("\\r");
                        break;
                    case '\t':
                        this.pw.print("\\t");
                        break;
                    case '/':
                        this.pw.print("\\/");
                        break;
                    default:
                        if ((c>='\u0000' && c<='\u001F') || (c>='\u007F' && c<='\u009F') || (c>='\u2000' && c<='\u20FF'))
                        {
                            final String hex=Integer.toHexString(c);
                            this.pw.print("\\u");
                            for(int k=0;k<4-hex.length();k++){
                                this.pw.print('0');
                            }
                            this.pw.print(hex.toUpperCase());
                        }
                        else{
                            this.pw.print(c);
                        }
                    }
                }
                this.pw.print('"');
            }
        }

        public void keyValue(String key, String value)
        {
            key(key);
            value(value);
            this.needsComma = true;
        }
    }
}
