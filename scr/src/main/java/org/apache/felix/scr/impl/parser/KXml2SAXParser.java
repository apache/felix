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
package org.apache.felix.scr.impl.parser;


import java.io.Reader;
import java.util.Stack;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The KXml2SAXParser extends the XmlParser from kxml. This is a very
 * simple parser that does not take into account the DTD
 *
 */
public class KXml2SAXParser extends KXmlParser
{

    /**
    * The constructor for a parser, it receives a java.io.Reader.
    *
    * @param   reader  The reader
    * @throws XmlPullParserException
    */
    public KXml2SAXParser( Reader reader ) throws XmlPullParserException
    {
        super();
        setInput( reader );
        setFeature( FEATURE_PROCESS_NAMESPACES, true );
    }


    /**
    * Parser from the reader provided in the constructor, and call
    * the startElement and endElement in a KxmlHandler
    *
    * @param   handler  The handler
    * @exception   Exception thrown by the superclass
    */
    public void parseXML( KXml2SAXHandler handler ) throws Exception
    {

        final Stack<XmlElement> openElements = new Stack<XmlElement>();
        XmlElement currentElement = null;
        final Attributes attributes = new Attributes();

        while ( next() != XmlPullParser.END_DOCUMENT )
        {
            handler.setLineNumber( getLineNumber() );
            handler.setColumnNumber( getColumnNumber() );

            if ( getEventType() == XmlPullParser.START_TAG )
            {
                currentElement = new XmlElement( getNamespace(), getName(), getLineNumber(), getColumnNumber() );
                openElements.push( currentElement );

                handler.startElement( getNamespace(), getName(), attributes );
            }
            else if ( getEventType() == XmlPullParser.END_TAG )
            {
                ensureMatchingCurrentElement(currentElement);
                openElements.pop();
                currentElement = openElements.isEmpty() ? null : ( XmlElement ) openElements.peek();

                handler.endElement( getNamespace(), getName() );
            }
            else if ( getEventType() == XmlPullParser.TEXT )
            {
                String text = getText();
                handler.characters( text );
            }
            else if ( getEventType() == XmlPullParser.PROCESSING_INSTRUCTION )
            {
                // TODO extract the target from the evt.getText()
                handler.processingInstruction( null, getText() );
            }
            else
            {
                // do nothing
            }
        }

        if ( !openElements.isEmpty() )
        {
            throw new ParseException( "Unclosed elements found: " + openElements, null );
        }
    }


    private void ensureMatchingCurrentElement( final XmlElement currentElement ) throws Exception
    {
        if ( currentElement == null )
        {
            throw new ParseException( "Unexpected closing element "
                + new XmlElement( getNamespace(), getName(), getLineNumber(), getColumnNumber() ), null );
        }

        if ( !currentElement.match( getNamespace(), getName() ) )
        {
            throw new ParseException( "Unexpected closing element "
                + new XmlElement( getNamespace(), getName(), getLineNumber(), getColumnNumber() )
                + ": Does not match opening element " + currentElement, null );
        }
    }

    private static class XmlElement
    {

        final String namespaceUri;
        final String name;
        final int line;
        final int col;


        XmlElement( final String namespaceUri, final String name, final int line, final int col )
        {
            this.namespaceUri = namespaceUri;
            this.name = name;
            this.line = line;
            this.col = col;
        }


        boolean match( final String namespaceUri, final String name )
        {
            return namespaceUri.equals( this.namespaceUri ) && name.equals( this.name );
        }

        public String toString()
        {
            return name + "@" + line + ":" + col;
        }
    }
    
    public class Attributes {
    	
    	public String getAttribute(String name) {
    		return getAttributeValue("", name);
    	}
    	
    	public String getAttribute(String uri, String name) {
    		return getAttributeValue(uri, name);
    	}

    }
}
