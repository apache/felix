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
 * <code>XMLHandler.java</code>...
 *
 */
public class XMLHandler {

    protected final XStream xstream;

    public XMLHandler() {
        this.xstream = new XStream();
        this.xstream.setMode(XStream.NO_REFERENCES);

        this.xstream.alias("components", Components.class);
        this.xstream.addImplicitCollection(Components.class, "components");

        this.xstream.alias("component", Component.class);
        this.xstream.useAttributeFor(Component.class, "name");
        this.xstream.useAttributeFor(Component.class, "enabled");
        this.xstream.useAttributeFor(Component.class, "immediate");

        this.xstream.alias("implementation", Implementation.class);
        this.xstream.useAttributeFor(Implementation.class, "classname");

        this.xstream.alias("property", Property.class);
        this.xstream.useAttributeFor(Property.class, "name");
        this.xstream.useAttributeFor(Property.class, "value");

        this.xstream.alias("service", Service.class);
        this.xstream.useAttributeFor(Service.class, "servicefactory");

        this.xstream.alias("provide", Interface.class);
        this.xstream.useAttributeFor(Interface.class, "interfacename");

        this.xstream.alias("reference", Reference.class);
        this.xstream.useAttributeFor(Reference.class, "name");
        this.xstream.useAttributeFor(Reference.class, "interfacename");
        this.xstream.useAttributeFor(Reference.class, "target");
        this.xstream.useAttributeFor(Reference.class, "cardinality");
        this.xstream.useAttributeFor(Reference.class, "policy");
        this.xstream.useAttributeFor(Reference.class, "bind");
        this.xstream.useAttributeFor(Reference.class, "unbind");
    }

    public Components read(File file) throws IOException, MojoExecutionException {
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
            return (Components)this.xstream.fromXML(new StringReader(buffer.toString()));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to read xml.", e);
        }
    }

    public void write(File file, Components components)
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
