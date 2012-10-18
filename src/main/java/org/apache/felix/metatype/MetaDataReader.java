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
package org.apache.felix.metatype;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.felix.metatype.internal.Activator;
import org.kxml2.io.KXmlParser;
import org.osgi.service.log.LogService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The <code>MetaDataReader</code> provides two methods to read meta type
 * documents according to the MetaType schema (105.8 XML Schema). The
 * {@link #parse(URL)} and {@link #parse(InputStream)} methods may be called
 * multiple times to parse such documents.
 * <p>
 * While reading the XML document java objects are created to hold the data.
 * These objects are created by factory methods. Users of this may extend this
 * class by overwriting the the factory methods to create specialized versions.
 * One notable use of this is the extension of the {@link AD} class to overwrite
 * the {@link AD#validate(String)} method. In this case, the {@link #createAD()}
 * method would be overwritten to return an instance of the extending class.
 * <p>
 * This class is not thread safe. Using instances of this class in multiple
 * threads concurrently is not supported and will fail.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MetaDataReader
{

    /**
     * The initial XML Namespace for Metatype 1.1 descriptors. This has
     * been replaced by the v1.1.0 namespace in the Compendium
     * Specification 4.2. We still have to support this namespace for
     * backwards compatibility.
     */
    private static final String NAMESPACE_1_0 = "http://www.osgi.org/xmlns/metatype/v1.0.0";

    /**
     * The XML Namespace for Metatype 1.1 descriptors.
     */
    private static final String NAMESPACE_1_1 = "http://www.osgi.org/xmlns/metatype/v1.1.0";

    /**
     * The XML Namespace for Metatype 1.2 descriptors.
     */
    private static final String NAMESPACE_1_2 = "http://www.osgi.org/xmlns/metatype/v1.2.0";

    /** The XML parser used to read the XML documents */
    private KXmlParser parser = new KXmlParser();

    /** Sets of attributes belonging to XML elements. */
    private final Set AD_ATTRIBUTES = new HashSet(Arrays.asList(new String[] { "name", "description", "id", "type", "cardinality", "min", "max", "default", "required" }));
    private final Set ATTRIBUTE_ATTRIBUTES = new HashSet(Arrays.asList(new String[] { "adref", "content" }));
    private final Set DESIGNATE_ATTRIBUTES = new HashSet(Arrays.asList(new String[] { "pid", "factoryPid", "bundle", "optional", "merge" }));
    private final Set DESIGNATEOBJECT_ATTRIBUTES = new HashSet(Arrays.asList(new String[] { "ocdref" }));
    private final Set METADATA_ATTRIBUTES = new HashSet(Arrays.asList(new String[] { "localization" }));
    private final Set OCD_ATTRIBUTES = new HashSet(Arrays.asList(new String[] { "name", "description", "id" }));


    /**
     * Parses the XML document provided by the <code>url</code>. The XML document
     * must be at the beginning of the stream contents.
     * <p>
     * This method is almost identical to
     * <code>return parse(url.openStream());</code> but also sets the string
     * representation of the URL as a location helper for error messages.
     *
     * @param url The <code>URL</code> providing access to the XML document.
     *
     * @return A {@link MetaData} providing access to the
     *      raw contents of the XML document.
     *
     * @throws IOException If an I/O error occurs accessing the stream.
     * @throws XmlPullParserException If an error occurs parsing the XML
     *      document.
     */
    public MetaData parse( URL url ) throws IOException, XmlPullParserException
    {
        InputStream ins = null;
        try
        {
            ins = url.openStream();
            this.parser.setProperty( "http://xmlpull.org/v1/doc/properties.html#location", url.toString() );
            MetaData md = this.parse( ins );
            if ( md != null )
            {
                md.setSource( url );
            }
            return md;
        }
        finally
        {
            if ( ins != null )
            {
                try
                {
                    ins.close();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }
        }
    }


    /**
     * Checks if this document has a meta type name space.
     *
     * @throws XmlPullParserException when there the meta type name space is not valid
     */
    private void checkMetatypeNamespace() throws XmlPullParserException
    {
        final String namespace = this.parser.getNamespace();
        if ( namespace != null && namespace.length() > 0 && !NAMESPACE_1_0.equals( namespace )
            && !NAMESPACE_1_1.equals( namespace ) && !NAMESPACE_1_2.equals( namespace ) )
        {
            throw new XmlPullParserException( "Unsupported Namespace " + namespace );
        }
    }


    /**
     * Parses the XML document in the given input stream.
     * <p>
     * This method starts reading at the current position of the input stream
     * and returns immediately after completely reading a single meta type
     * document. The stream is not closed by this method.
     *
     * @param ins The <code>InputStream</code> providing the XML document
     *
     * @return A {@link MetaData} providing access to the
     *      raw contents of the XML document.
     *
     * @throws IOException If an I/O error occurrs accessing the stream.
     * @throws XmlPullParserException If an error occurrs parsing the XML
     *      document.
     */
    public MetaData parse( InputStream ins ) throws IOException, XmlPullParserException
    {
        this.parser.setFeature( KXmlParser.FEATURE_PROCESS_NAMESPACES, true );

        // set the parser input, use null encoding to force detection with <?xml?>
        this.parser.setInput( ins, null );

        MetaData mti = null;

        int eventType = this.parser.getEventType();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "MetaData".equals( this.parser.getName() ) )
                {
                    checkMetatypeNamespace();
                    mti = this.readMetaData( );
                }
                else
                {
                    this.ignoreElement();
                }
            }
            eventType = this.parser.next();
        }

        return mti;
    }


    private void readOptionalAttributes(OptionalAttributes entity, Set attributes) {
        int count = this.parser.getAttributeCount();
        for (int i = 0; i < count; i++)
        {
            String name = this.parser.getAttributeName(i);
            if (!attributes.contains(name))
            {
                String value = this.parser.getAttributeValue(i);
                entity.addOptionalAttribute(name, value);
            }
        }
    }


    private MetaData readMetaData() throws IOException, XmlPullParserException
    {
        MetaData mti = this.createMetaData();
        mti.setLocalePrefix( this.getOptionalAttribute( "localization" ) );

        readOptionalAttributes(mti, METADATA_ATTRIBUTES);

        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "OCD".equals( this.parser.getName() ) )
                {
                    mti.addObjectClassDefinition( this.readOCD() );
                }
                else if ( "Designate".equals( this.parser.getName() ) )
                {
                    mti.addDesignate( this.readDesignate() );
                }
                else
                {
                    this.ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "MetaData".equals( this.parser.getName() ) )
                {
                    break;
                }

                throw this.unexpectedElement();
            }
            eventType = this.parser.next();
        }

        return mti;
    }


    private OCD readOCD() throws IOException, XmlPullParserException
    {
        OCD ocd = this.createOCD();
        ocd.setId( this.getRequiredAttribute( "id" ) );
        ocd.setName( this.getRequiredAttribute( "name" ) );
        ocd.setDescription( this.getOptionalAttribute( "description" ) );

        readOptionalAttributes(ocd, OCD_ATTRIBUTES);

        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "AD".equals( this.parser.getName() ) )
                {
                    ocd.addAttributeDefinition( this.readAD() );
                }
                else if ( "Icon".equals( this.parser.getName() ) )
                {
                    String res = this.getRequiredAttribute( "resource" );
                    String sizeString = this.getRequiredAttribute( "size" );
                    try
                    {
                        Integer size = Integer.decode( sizeString );
                        ocd.addIcon( size, res );
                    }
                    catch ( NumberFormatException nfe )
                    {
                        Activator.log( LogService.LOG_DEBUG, "readOCD: Icon size '" + sizeString
                            + "' is not a valid number" );
                    }
                }
                else
                {
                    this.ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "OCD".equals( this.parser.getName() ) )
                {
                    break;
                }
                else if ( !"Icon".equals( this.parser.getName() ) )
                {
                    throw this.unexpectedElement();
                }
            }
            eventType = this.parser.next();
        }

        return ocd;
    }


    private Designate readDesignate() throws IOException, XmlPullParserException
    {
        final String pid = this.getOptionalAttribute( "pid" );
        final String factoryPid = this.getOptionalAttribute( "factoryPid" );
        if ( pid == null && factoryPid == null )
        {
            missingAttribute( "pid or factoryPid" );
        }

        Designate designate = this.createDesignate();
        designate.setPid( pid );
        designate.setFactoryPid( factoryPid );
        designate.setBundleLocation( this.getOptionalAttribute( "bundle" ) );
        designate.setOptional( this.getOptionalAttribute( "optional", false ) );
        designate.setMerge( this.getOptionalAttribute( "merge", false ) );

        readOptionalAttributes(designate, DESIGNATE_ATTRIBUTES);

        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Object".equals( this.parser.getName() ) )
                {
                    designate.setObject( this.readObject() );
                }
                else
                {
                    this.ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "Designate".equals( this.parser.getName() ) )
                {
                    break;
                }

                throw this.unexpectedElement();
            }
            eventType = this.parser.next();
        }

        return designate;
    }


    private AD readAD() throws IOException, XmlPullParserException
    {
        AD ad = this.createAD();
        ad.setID( this.getRequiredAttribute( "id" ) );
        ad.setName( this.getOptionalAttribute( "name" ) );
        ad.setDescription( this.getOptionalAttribute( "description" ) );
        ad.setType( this.getRequiredAttribute( "type" ) );
        ad.setCardinality( this.getOptionalAttribute( "cardinality", 0 ) );
        ad.setMin( this.getOptionalAttribute( "min" ) );
        ad.setMax( this.getOptionalAttribute( "max" ) );
        ad.setDefaultValue( this.getOptionalAttribute( "default" ) );
        ad.setRequired( this.getOptionalAttribute( "required", true ) );

        readOptionalAttributes(ad, AD_ATTRIBUTES);

        Map options = new LinkedHashMap();
        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Option".equals( this.parser.getName() ) )
                {
                    String value = this.getRequiredAttribute( "value" );
                    String label = this.getRequiredAttribute( "label" );
                    options.put( value, label );
                }
                else
                {
                    this.ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "AD".equals( this.parser.getName() ) )
                {
                    break;
                }
                else if ( !"Option".equals( this.parser.getName() ) )
                {
                    throw this.unexpectedElement();
                }
            }
            eventType = this.parser.next();
        }

        ad.setOptions( options );

        return ad;
    }


    private DesignateObject readObject() throws IOException, XmlPullParserException
    {
        DesignateObject oh = this.createDesignateObject();
        oh.setOcdRef( this.getRequiredAttribute( "ocdref" ) );

        readOptionalAttributes(oh, DESIGNATEOBJECT_ATTRIBUTES);

        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Attribute".equals( this.parser.getName() ) )
                {
                    oh.addAttribute( this.readAttribute() );
                }
                else
                {
                    this.ignoreElement();
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "Object".equals( this.parser.getName() ) )
                {
                    break;
                }
                throw this.unexpectedElement();
            }
            eventType = this.parser.next();
        }

        return oh;
    }


    private Attribute readAttribute() throws IOException, XmlPullParserException
    {
        Attribute ah = this.createAttribute();
        ah.setAdRef( this.getRequiredAttribute( "adref" ) );
        ah.addContent( this.getOptionalAttribute( "content" ), true );

        readOptionalAttributes(ah, ATTRIBUTE_ATTRIBUTES);

        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( "Value".equals( this.parser.getName() ) )
                {
                    ah.addContent( this.parser.nextText(), false );
                    eventType = this.parser.getEventType();
                    continue;
                }
                this.ignoreElement();
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( "Attribute".equals( this.parser.getName() ) )
                {
                    break;
                }
                else if ( !"Value".equals( this.parser.getName() ) )
                {
                    throw this.unexpectedElement();
                }
            }
            eventType = this.parser.next();
        }

        return ah;
    }


    //---------- Attribute access helper --------------------------------------

    private String getRequiredAttribute( String attrName ) throws XmlPullParserException
    {
        String attrVal = this.parser.getAttributeValue( null, attrName );
        if ( attrVal != null )
        {
            return attrVal;
        }

        // fail if value is missing
        throw this.missingAttribute( attrName );
    }


    private String getOptionalAttribute( String attrName )
    {
        return this.getOptionalAttribute( attrName, ( String ) null );
    }


    private String getOptionalAttribute( String attrName, String defaultValue )
    {
        String attrVal = this.parser.getAttributeValue( null, attrName );
        return ( attrVal != null ) ? attrVal : defaultValue;
    }


    private boolean getOptionalAttribute( String attrName, boolean defaultValue )
    {
        String attrVal = this.parser.getAttributeValue( null, attrName );
        return ( attrVal != null ) ? "true".equalsIgnoreCase( attrVal ) : defaultValue;
    }


    private int getOptionalAttribute( String attrName, int defaultValue )
    {
        String attrVal = this.parser.getAttributeValue( null, attrName );
        if ( attrVal != null && attrVal.length() > 0 )
        {
            try
            {
                return Integer.decode( attrVal ).intValue();
            }
            catch ( NumberFormatException nfe )
            {
                Activator.log( LogService.LOG_DEBUG, "getOptionalAttribute: Value '" + attrVal + "' of attribute "
                    + attrName + " is not a valid number. Using default value " + defaultValue );
            }
        }

        // fallback to default
        return defaultValue;
    }


    //---------- Error Handling support ---------------------------------------

    private void ignoreElement() throws IOException, XmlPullParserException
    {
        String ignoredElement = this.parser.getName();

        int depth = 0; // enable nested ignored elements
        int eventType = this.parser.next();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( ignoredElement.equals( this.parser.getName() ) )
                {
                    depth++;
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( ignoredElement.equals( this.parser.getName() ) )
                {
                    if ( depth <= 0 )
                    {
                        return;
                    }

                    depth--;
                }
            }
            eventType = this.parser.next();
        }
    }


    private XmlPullParserException missingAttribute( String attrName )
    {
        String message = "Missing Attribute " + attrName + " in element " + this.parser.getName();
        return new XmlPullParserException( message, this.parser, null );
    }


    private XmlPullParserException unexpectedElement()
    {
        String message = "Illegal Element " + this.parser.getName();
        return new XmlPullParserException( message, this.parser, null );
    }


    //---------- Factory methods ----------------------------------------------

    /**
     * Creates a new {@link MetaData} object to hold the contents of the
     * <code>MetaData</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected MetaData createMetaData()
    {
        return new MetaData();
    }


    /**
     * Creates a new {@link OCD} object to hold the contents of the
     * <code>OCD</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected OCD createOCD()
    {
        return new OCD();
    }


    /**
     * Creates a new {@link AD} object to hold the contents of the
     * <code>AD</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected AD createAD()
    {
        return new AD();
    }


    /**
     * Creates a new {@link DesignateObject} object to hold the contents of the
     * <code>Object</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected DesignateObject createDesignateObject()
    {
        return new DesignateObject();
    }


    /**
     * Creates a new {@link Attribute} object to hold the contents of the
     * <code>Attribute</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected Attribute createAttribute()
    {
        return new Attribute();
    }


    /**
     * Creates a new {@link Designate} object to hold the contents of the
     * <code>Designate</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected Designate createDesignate()
    {
        return new Designate();
    }
}
