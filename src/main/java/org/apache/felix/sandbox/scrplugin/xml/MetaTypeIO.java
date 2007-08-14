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
import org.apache.felix.sandbox.scrplugin.om.metatype.Definitions;
import org.apache.maven.plugin.MojoExecutionException;

import com.thoughtworks.xstream.XStream;

/**
 * <code>MetaType</code>
 *
 * is a helper class to read and write meta type service files.
 *
 */
public class MetaTypeIO {

    protected final XStream xstream;

    public MetaTypeIO() {
        this.xstream = new XStream();
        this.xstream.setMode(XStream.NO_REFERENCES);

        this.xstream.alias("OCD", org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "id");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "description");

        this.xstream.alias("AD", org.apache.felix.sandbox.scrplugin.om.metatype.AttributeDefinition.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.AttributeDefinition.class, "id");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.AttributeDefinition.class, "type");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "description");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "cardinality");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.OCD.class, "defaultValue");

        this.xstream.alias("Designate", org.apache.felix.sandbox.scrplugin.om.metatype.Designate.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.Designate.class, "pid");

        this.xstream.alias("Object", org.apache.felix.sandbox.scrplugin.om.metatype.MTObject.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.metatype.MTObject.class, "ocdref");
    }

    public Definitions read(File file) throws IOException, MojoExecutionException {
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
            return (Definitions)this.xstream.fromXML(new StringReader(buffer.toString()));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to read xml.", e);
        }
    }

    public void write(File file, Definitions defs)
    throws IOException, MojoExecutionException {
        Writer buffer = new StringWriter();
        this.xstream.toXML(defs, buffer);

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
