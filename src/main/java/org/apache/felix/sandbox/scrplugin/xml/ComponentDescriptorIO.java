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
package org.apache.felix.sandbox.scrplugin.xml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;

import com.thoughtworks.xstream.XStream;

/**
 * <code>ComponentDescriptorIO</code>
 *
 * is a helper class to read and write component descriptor files.
 *
 */
public class ComponentDescriptorIO {

    protected final XStream xstream;

    public ComponentDescriptorIO() {
        this.xstream = new XStream();
        this.xstream.setMode(XStream.NO_REFERENCES);

        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.AbstractObject.class, "tag");

        this.xstream.alias("components", org.apache.felix.sandbox.scrplugin.om.Components.class);
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Components.class, "components");

        this.xstream.alias("component", org.apache.felix.sandbox.scrplugin.om.Component.class);
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Component.class, "references");
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Component.class, "properties");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Component.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Component.class, "enabled");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Component.class, "immediate");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "label");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "description");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "isAbstract");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "hasMetatype");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "serviceFactory");

        this.xstream.alias("implementation", org.apache.felix.sandbox.scrplugin.om.Implementation.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Implementation.class, "classname");

        this.xstream.alias("property", org.apache.felix.sandbox.scrplugin.om.Property.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Property.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Property.class, "value");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Property.class, "label");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Property.class, "description");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Property.class, "options");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Property.class, "privateProperty");

        this.xstream.alias("service", org.apache.felix.sandbox.scrplugin.om.Service.class);
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Service.class, "interfaces");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Service.class, "servicefactory");

        this.xstream.alias("provide", org.apache.felix.sandbox.scrplugin.om.Interface.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Interface.class, "interfacename");

        this.xstream.alias("reference", org.apache.felix.sandbox.scrplugin.om.Reference.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "interfacename");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "target");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "cardinality");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "policy");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "bind");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "unbind");
    }

    public org.apache.felix.sandbox.scrplugin.om.Components read(File file) throws IOException, MojoExecutionException {
        Writer buffer = new StringWriter();
        final TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            IOUtils.copy(new FileReader(file), buffer);
            String xmlDoc = buffer.toString();
            buffer = new StringWriter();
            int pos = xmlDoc.indexOf("?>");
            if ( pos > 0 ) {
                xmlDoc = xmlDoc.substring(pos+2);
            }
            xmlDoc = "<components>" + xmlDoc + "</components>";
            transformer = factory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/org/apache/felix/sandbox/scrplugin/xml/read.xsl")));
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.transform(new StreamSource(new StringReader(xmlDoc)), new StreamResult(buffer));
            return (org.apache.felix.sandbox.scrplugin.om.Components)this.xstream.fromXML(new StringReader(buffer.toString()));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to read xml.", e);
        }
    }

    public void write(File file, org.apache.felix.sandbox.scrplugin.om.Components components)
    throws IOException, MojoExecutionException {
        Writer buffer = new StringWriter();
        this.xstream.toXML(components, buffer);

        final TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = factory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/org/apache/felix/sandbox/scrplugin/xml/write.xsl")));
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            transformer.transform(new StreamSource(new StringReader(buffer.toString())), new StreamResult(new FileWriter(file)));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to write xml.", e);
        }
    }
}
