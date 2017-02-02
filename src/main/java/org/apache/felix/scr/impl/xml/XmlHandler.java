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
package org.apache.felix.scr.impl.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.PropertyMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.apache.felix.scr.impl.parser.KXml2SAXHandler;
import org.apache.felix.scr.impl.parser.KXml2SAXParser.Attributes;
import org.apache.felix.scr.impl.parser.ParseException;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * XML Parser for the component XML
 */
public class XmlHandler implements KXml2SAXHandler
{
    // the bundle containing the XML resource being parsed
    private final Bundle m_bundle;

    // logger for any messages
    private final Logger m_logger;

    private final boolean m_globalObsoleteFactoryComponentFactory;

    private final boolean m_globalDelayedKeepInstances;

    // A reference to the current component
    private ComponentMetadata m_currentComponent;

    // The current service
    private ServiceMetadata m_currentService;

    // A list of component descriptors contained in the file
    private List<ComponentMetadata> m_components = new ArrayList<ComponentMetadata>();

    // PropertyMetaData whose value attribute is missing, hence has element data
    private PropertyMetadata m_pendingProperty;

    // PropertyMetaData whose value attribute is missing, hence has element data
    private PropertyMetadata m_pendingFactoryProperty;

    /** Flag for detecting the first element. */
    protected boolean firstElement = true;

    /** Override namespace. */
    protected String overrideNamespace;

    /** Flag for elements inside a component element */
    protected boolean isComponent = false;

    // creates an instance with the bundle owning the component descriptor
    // file parsed by this instance
    public XmlHandler( Bundle bundle, Logger logger, boolean globalObsoleteFactoryComponentFactory, boolean globalDelayedKeepInstances )
    {
        m_bundle = bundle;
        m_logger = logger;
        m_globalObsoleteFactoryComponentFactory = globalObsoleteFactoryComponentFactory;
        m_globalDelayedKeepInstances = globalDelayedKeepInstances;
    }


    /**
     * Called to retrieve the service descriptors
     *
     * @return   A list of service descriptors
     */
    public List<ComponentMetadata> getComponentMetadataList()
    {
        return m_components;
    }


    /**
     * Method called when a tag opens
     *
     * @param   uri
     * @param   localName
     * @param   attributes
     * @exception   ParseException
     **/
    @Override
    public void startElement( String uri, String localName, Attributes attributes ) throws ParseException
    {
        // according to the spec, the elements should have the namespace,
        // except when the root element is the "component" element
        // So we check this for the first element, we receive.
        if ( firstElement )
        {
            firstElement = false;
            if ( localName.equals( XmlConstants.EL_COMPONENT ) && XmlConstants.NAMESPACE_URI_EMPTY.equals( uri ) )
            {
                overrideNamespace = XmlConstants.NAMESPACE_URI;
            }
        }

        if ( overrideNamespace != null && XmlConstants.NAMESPACE_URI_EMPTY.equals( uri ) )
        {
            uri = overrideNamespace;
        }

        // FELIX-695: however the spec also states that the inner elements
        // of a component are unqualified, so they don't have
        // the namespace - we allow both: with or without namespace!
        if ( this.isComponent && XmlConstants.NAMESPACE_URI_EMPTY.equals(uri) )
        {
            uri = XmlConstants.NAMESPACE_URI;
        }

        // get the namespace code for the namespace uri
        DSVersion namespaceCode = XmlConstants.NAMESPACE_CODE_MAP.get( uri );
        // from now on uri points to the namespace
        if ( namespaceCode != null )
        {
            try
            {

                // 112.4.3 Component Element
                if ( localName.equals( XmlConstants.EL_COMPONENT ) )
                {
                    this.isComponent = true;

                    // Create a new ComponentMetadata
                    m_currentComponent = new ComponentMetadata( namespaceCode );

                    // name attribute is optional (since DS 1.1)
                    if ( attributes.getAttribute( XmlConstants.ATTR_NAME ) != null )
                    {
                        m_currentComponent.setName( attributes.getAttribute( XmlConstants.ATTR_NAME ) );
                    }

                    // enabled attribute is optional
                    if ( attributes.getAttribute( "enabled" ) != null )
                    {
                        m_currentComponent.setEnabled( attributes.getAttribute( "enabled" ).equals( "true" ) );
                    }

                    // immediate attribute is optional
                    if ( attributes.getAttribute( "immediate" ) != null )
                    {
                        m_currentComponent.setImmediate( attributes.getAttribute( "immediate" ).equals( "true" ) );
                    }

                    // factory attribute is optional
                    if ( attributes.getAttribute( "factory" ) != null )
                    {
                        m_currentComponent.setFactoryIdentifier( attributes.getAttribute( "factory" ) );
                    }

                    // configuration-policy is optional (since DS 1.1)
                    if ( attributes.getAttribute( "configuration-policy" ) != null )
                    {
                        m_currentComponent.setConfigurationPolicy( attributes.getAttribute( "configuration-policy" ) );
                    }

                    // activate attribute is optional (since DS 1.1)
                    if ( attributes.getAttribute( "activate" ) != null )
                    {
                        m_currentComponent.setActivate( attributes.getAttribute( "activate" ) );
                    }

                    // deactivate attribute is optional (since DS 1.1)
                    if ( attributes.getAttribute( "deactivate" ) != null )
                    {
                        m_currentComponent.setDeactivate( attributes.getAttribute( "deactivate" ) );
                    }

                    // modified attribute is optional (since DS 1.1)
                    if ( attributes.getAttribute( "modified" ) != null )
                    {
                        m_currentComponent.setModified( attributes.getAttribute( "modified" ) );
                    }

                    // configuration-pid attribute is optional (since DS 1.2)
                    String configurationPidString = attributes.getAttribute( "configuration-pid" );
                    if (configurationPidString != null)
                    {
                        String[] configurationPid = configurationPidString.split( " " );
                        m_currentComponent.setConfigurationPid( configurationPid );
                    }

                    m_currentComponent.setConfigurableServiceProperties("true".equals(attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_CONFIGURABLE_SERVICE_PROPERTIES)));
                    m_currentComponent.setPersistentFactoryComponent("true".equals(attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_PERSISTENT_FACTORY_COMPONENT)));
                    m_currentComponent.setDeleteCallsModify("true".equals(attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_DELETE_CALLS_MODIFY)));
                    if ( attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_OBSOLETE_FACTORY_COMPONENT_FACTORY) != null)
                    {
                        m_currentComponent.setObsoleteFactoryComponentFactory("true".equals(attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_OBSOLETE_FACTORY_COMPONENT_FACTORY)));
                    }
                    else if ( !namespaceCode.isDS13() )
                    {
                        m_currentComponent.setObsoleteFactoryComponentFactory(m_globalObsoleteFactoryComponentFactory);
                    }
                    m_currentComponent.setConfigureWithInterfaces("true".equals(attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_CONFIGURE_WITH_INTERFACES)));
                    m_currentComponent.setDelayedKeepInstances(m_globalDelayedKeepInstances || "true".equals(attributes.getAttribute(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_DELAYED_KEEP_INSTANCES)));

                    // activation-fields is optional (since DS 1.4)
                    String activationFields = attributes.getAttribute( "activation-fields" );
                    if ( activationFields != null )
                    {
                        final String[] fields = activationFields.split(" ");
                        m_currentComponent.setActivationFields( fields );
                    }

                    // Add this component to the list
                    m_components.add( m_currentComponent );
                }

                // not inside a component element, ignore current element
                else if ( !this.isComponent )
                {
                    m_logger.log( LogService.LOG_DEBUG,
                            "Not currently parsing a component; ignoring element {0} (bundle {1})", new Object[]
                                    { localName, m_bundle.getLocation() }, null, null, null );
                }

                // 112.4.4 Implementation
                else if ( localName.equals( XmlConstants.EL_IMPL ) )
                {
                    // Set the implementation class name (mandatory)
                    m_currentComponent.setImplementationClassName( attributes.getAttribute( "class" ) );
                }
                // 112.4.5 [...] Property Elements
                else if ( localName.equals( XmlConstants.EL_PROPERTY ) )
                {
                    PropertyMetadata prop = new PropertyMetadata();

                    // name attribute is mandatory
                    prop.setName( attributes.getAttribute( XmlConstants.ATTR_NAME ) );

                    // type attribute is optional
                    if ( attributes.getAttribute( XmlConstants.ATTR_TYPE ) != null )
                    {
                        prop.setType( attributes.getAttribute( XmlConstants.ATTR_TYPE ) );
                    }

                    // 112.4.5: If the value attribute is specified, the body of the element is ignored.
                    if ( attributes.getAttribute( XmlConstants.ATTR_VALUE ) != null )
                    {
                        prop.setValue( attributes.getAttribute( XmlConstants.ATTR_VALUE ) );
                        m_currentComponent.addProperty( prop );
                    }
                    else
                    {
                        // hold the metadata pending
                        m_pendingProperty = prop;
                    }
                }
                // 112.4.5 Properties [...] Elements
                else if ( localName.equals( XmlConstants.EL_PROPERTIES ) )
                {
                    final Properties props = readPropertiesEntry( attributes.getAttribute( "entry" ) );
                    // create PropertyMetadata for the properties from the file
                    for ( Map.Entry<Object, Object> pEntry: props.entrySet() )
                    {
                        PropertyMetadata prop = new PropertyMetadata();
                        prop.setName( String.valueOf( pEntry.getKey() ) );
                        prop.setValue( String.valueOf( pEntry.getValue() ) );
                        m_currentComponent.addProperty( prop );
                    }

                }
                // 112.4.9 [...] Factory Property Element
                else if ( localName.equals( XmlConstants.EL_FACTORY_PROPERTY ) )
                {
                    PropertyMetadata prop = new PropertyMetadata();

                    // name attribute is mandatory
                    prop.setName( attributes.getAttribute( XmlConstants.ATTR_NAME ) );

                    // type attribute is optional
                    if ( attributes.getAttribute( XmlConstants.ATTR_TYPE ) != null )
                    {
                        prop.setType( attributes.getAttribute( XmlConstants.ATTR_TYPE ) );
                    }

                    // 112.4.5: If the value attribute is specified, the body of the element is ignored.
                    if ( attributes.getAttribute( XmlConstants.ATTR_VALUE ) != null )
                    {
                        prop.setValue( attributes.getAttribute( XmlConstants.ATTR_VALUE ) );
                        m_currentComponent.addFactoryProperty( prop );
                    }
                    else
                    {
                        // hold the metadata pending
                        m_pendingFactoryProperty = prop;
                    }
                }
                // 112.4.9 [...] Factory Properties Element
                else if ( localName.equals( XmlConstants.EL_FACTORY_PROPERTIES ) )
                {
                    final Properties props = readPropertiesEntry( attributes.getAttribute( "entry" ) );
                    // create PropertyMetadata for the properties from the file
                    for ( Map.Entry<Object, Object> pEntry: props.entrySet() )
                    {
                        PropertyMetadata prop = new PropertyMetadata();
                        prop.setName( String.valueOf( pEntry.getKey() ) );
                        prop.setValue( String.valueOf( pEntry.getValue() ) );
                        m_currentComponent.addFactoryProperty( prop );
                    }
                }
                // 112.4.6 Service Element
                else if ( localName.equals( XmlConstants.EL_SERVICE ) )
                {

                    m_currentService = new ServiceMetadata();

                    // servicefactory attribute is optional
                    if ( attributes.getAttribute( "servicefactory" ) != null )
                    {
                        m_currentService.setServiceFactory( attributes.getAttribute( "servicefactory" ).equals( "true" ) );
                    }

                    if ( attributes.getAttribute( "scope" ) != null )
                    {
                        m_currentService.setScope( attributes.getAttribute( "scope" ) );
                    }

                    m_currentComponent.setService( m_currentService );
                }
                else if ( localName.equals( XmlConstants.EL_PROVIDE ) )
                {
                    m_currentService.addProvide( attributes.getAttribute( XmlConstants.ATTR_INTERFACE ) );
                }

                // 112.4.7 Reference element
                else if ( localName.equals( XmlConstants.EL_REF ) )
                {
                    ReferenceMetadata ref = new ReferenceMetadata();

                    // name attribute is optional (since DS 1.1)
                    if ( attributes.getAttribute( XmlConstants.ATTR_NAME ) != null )
                    {
                        ref.setName( attributes.getAttribute( XmlConstants.ATTR_NAME ) );
                    }

                    ref.setInterface( attributes.getAttribute( XmlConstants.ATTR_INTERFACE ) );

                    // Cardinality
                    if ( attributes.getAttribute( "cardinality" ) != null )
                    {
                        ref.setCardinality( attributes.getAttribute( "cardinality" ) );
                    }

                    if ( attributes.getAttribute( "policy" ) != null )
                    {
                        ref.setPolicy( attributes.getAttribute( "policy" ) );
                    }

                    if ( attributes.getAttribute( "policy-option" ) != null )
                    {
                        ref.setPolicyOption( attributes.getAttribute( "policy-option" ) );
                    }

                    if ( attributes.getAttribute( "scope" ) != null )
                    {
                        ref.setScope( attributes.getAttribute( "scope" ) );
                    }

                    if ( attributes.getAttribute( "target" ) != null)
                    {
                        ref.setTarget( attributes.getAttribute( "target" ) );
                        PropertyMetadata prop = new PropertyMetadata();
                        prop.setName( (ref.getName() == null? ref.getInterface(): ref.getName()) + ".target");
                        prop.setValue( attributes.getAttribute( "target" ) );
                        m_currentComponent.addProperty( prop );

                    }

                    // method reference
                    ref.setBind( attributes.getAttribute( "bind" ) );
                    ref.setUpdated( attributes.getAttribute( "updated" ) );
                    ref.setUnbind( attributes.getAttribute( "unbind" ) );

                    // field reference
                    ref.setField( attributes.getAttribute( "field" ) );
                    ref.setFieldOption( attributes.getAttribute( "field-option" ) );
                    ref.setFieldCollectionType( attributes.getAttribute( "field-collection-type" ) );

                    // DS 1.4 : references as parameter of the activator (method or constructor)
                    if ( attributes.getAttribute( "parameter" ) != null)
                    {
                        ref.setParameter( attributes.getAttribute( "parameter" ) );

                    }

                    m_currentComponent.addDependency( ref );
                }

                // unexpected element (except the root element "components"
                // used by the Maven SCR Plugin, which is just silently ignored)
                else if ( !localName.equals( XmlConstants.EL_COMPONENTS ) )
                {
                    m_logger.log( LogService.LOG_DEBUG, "Ignoring unsupported element {0} (bundle {1})", new Object[]
                            { localName, m_bundle.getLocation() }, null, null, null );
                }
            }
            catch ( Exception ex )
            {
                throw new ParseException( "Exception during parsing", ex );
            }
        }

        // unexpected namespace (except the root element "components"
        // used by the Maven SCR Plugin, which is just silently ignored)
        else if ( !localName.equals( XmlConstants.EL_COMPONENTS ) )
        {
            m_logger.log( LogService.LOG_DEBUG, "Ignoring unsupported element '{'{0}'}'{1} (bundle {2})", new Object[]
                    { uri, localName, m_bundle.getLocation() }, null, null, null );
        }
    }


    /**
     * Method called when a tag closes
     *
     * @param   uri
     * @param   localName
     */
    @Override
    public void endElement( String uri, String localName )
    {
        if ( overrideNamespace != null && XmlConstants.NAMESPACE_URI_EMPTY.equals( uri ) )
        {
            uri = overrideNamespace;
        }

        if ( this.isComponent && XmlConstants.NAMESPACE_URI_EMPTY.equals(uri) )
        {
            uri = XmlConstants.NAMESPACE_URI;
        }

        if ( XmlConstants.NAMESPACE_URI.equals( uri ) )
        {
            if ( localName.equals( XmlConstants.EL_COMPONENT ) )
            {
                this.isComponent = false;
            }
            else if ( localName.equals( XmlConstants.EL_PROPERTY ) && m_pendingProperty != null )
            {
                // 112.4.5 body expected to contain property value
                // if so, the m_pendingProperty field would be null
                // currently, we just ignore this situation
                m_pendingProperty = null;
            }
            else if ( localName.equals( XmlConstants.EL_FACTORY_PROPERTY ) && m_pendingFactoryProperty != null )
            {
                // 112.4.5 body expected to contain property value
                // if so, the m_pendingFactoryProperty field would be null
                // currently, we just ignore this situation
                m_pendingFactoryProperty = null;
            }
        }
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#characters(java.lang.String)
     */
    @Override
    public void characters( String text )
    {
        // 112.4.5 If the value attribute is not specified, the body must contain one or more values
        if ( m_pendingProperty != null )
        {
            m_pendingProperty.setValues( text );
            m_currentComponent.addProperty( m_pendingProperty );
            m_pendingProperty = null;
        }
        if ( m_pendingFactoryProperty != null )
        {
            m_pendingFactoryProperty.setValues( text );
            m_currentComponent.addFactoryProperty( m_pendingFactoryProperty );
            m_pendingFactoryProperty = null;
        }
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    @Override
    public void processingInstruction( String target, String data )
    {
        // Not used
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#setLineNumber(int)
     */
    @Override
    public void setLineNumber( int lineNumber )
    {
        // Not used
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#setColumnNumber(int)
     */
    @Override
    public void setColumnNumber( int columnNumber )
    {
        // Not used
    }


    /**
     * Reads the name property file from the bundle owning this descriptor. All
     * properties read from the properties file are added to the current
     * component's property meta data list.
     *
     * @param entryName The name of the bundle entry containing the propertes
     *      to be added. This must not be <code>null</code>.
     *
     * @throws ParseException If the entry name is <code>null</code> or no
     *      entry with the given name exists in the bundle or an error occurrs
     *      reading the properties file.
     */
    private Properties readPropertiesEntry( String entryName ) throws ParseException
    {
        if ( entryName == null )
        {
            throw new ParseException( "Missing entry attribute of properties element", null );
        }

        URL entryURL = m_bundle.getEntry( entryName );
        if ( entryURL == null )
        {
            throw new ParseException( "Missing bundle entry " + entryName, null );
        }

        Properties props = new Properties();
        InputStream entryStream = null;
        try
        {
            entryStream = entryURL.openStream();
            props.load( entryStream );
        }
        catch ( IOException ioe )
        {
            throw new ParseException( "Failed to read properties entry " + entryName, ioe );
        }
        finally
        {
            if ( entryStream != null )
            {
                try
                {
                    entryStream.close();
                }
                catch ( IOException ignore )
                {
                    // don't care
                }
            }
        }

        return props;
    }
}
