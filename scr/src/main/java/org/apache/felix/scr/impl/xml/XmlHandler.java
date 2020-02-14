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

import org.apache.felix.scr.impl.logger.BundleLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.PropertyMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Parser for the component XML
 */
public class XmlHandler extends DefaultHandler
{
    // the bundle containing the XML resource being parsed
    private final Bundle m_bundle;

    // logger for any messages
    private final BundleLogger m_logger;

    private final boolean m_globalObsoleteFactoryComponentFactory;

    private final boolean m_globalDelayedKeepInstances;

    // A reference to the current component
    private ComponentMetadata m_currentComponent;

    // The current service
    private ServiceMetadata m_currentService;

    // A list of component descriptors contained in the file
    private List<ComponentMetadata> m_components = new ArrayList<>();

    // PropertyMetaData whose value attribute is missing, hence has element data
    private PropertyMetadata m_pendingProperty;

    // PropertyMetaData whose value attribute is missing, hence has element data
    private PropertyMetadata m_pendingFactoryProperty;

    private StringBuilder propertyBuilder;

    /** Flag for detecting the first element. */
    protected boolean firstElement = true;

    /** Override namespace. */
    protected String overrideNamespace;

    /** Flag for elements inside a component element */
    protected boolean isComponent = false;

    // creates an instance with the bundle owning the component descriptor
    // file parsed by this instance
    public XmlHandler( Bundle bundle, BundleLogger logger, boolean globalObsoleteFactoryComponentFactory, boolean globalDelayedKeepInstances )
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


    @Override
    public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException
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
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_NAME ) != null )
                    {
                        m_currentComponent.setName( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_NAME ) );
                    }

                    // enabled attribute is optional
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "enabled" ) != null )
                    {
                        m_currentComponent.setEnabled( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "enabled" ).equals( "true" ) );
                    }

                    // immediate attribute is optional
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "immediate" ) != null )
                    {
                        m_currentComponent.setImmediate( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "immediate" ).equals( "true" ) );
                    }

                    // factory attribute is optional
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "factory" ) != null )
                    {
                        m_currentComponent.setFactoryIdentifier( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "factory" ) );
                    }

                    // configuration-policy is optional (since DS 1.1)
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "configuration-policy" ) != null )
                    {
                        m_currentComponent.setConfigurationPolicy( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "configuration-policy" ) );
                    }

                    // activate attribute is optional (since DS 1.1)
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "activate" ) != null )
                    {
                        m_currentComponent.setActivate( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "activate" ) );
                    }

                    // deactivate attribute is optional (since DS 1.1)
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "deactivate" ) != null )
                    {
                        m_currentComponent.setDeactivate( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "deactivate" ) );
                    }

                    // modified attribute is optional (since DS 1.1)
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "modified" ) != null )
                    {
                        m_currentComponent.setModified( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "modified" ) );
                    }

                    // configuration-pid attribute is optional (since DS 1.2)
                    String configurationPidString = attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "configuration-pid" );
                    if (configurationPidString != null)
                    {
                        String[] configurationPid = configurationPidString.split( " " );
                        m_currentComponent.setConfigurationPid( configurationPid );
                    }

                    m_currentComponent.setConfigurableServiceProperties("true".equals(attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_CONFIGURABLE_SERVICE_PROPERTIES)));
                    m_currentComponent.setPersistentFactoryComponent("true".equals(attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_PERSISTENT_FACTORY_COMPONENT)));
                    m_currentComponent.setDeleteCallsModify("true".equals(attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_DELETE_CALLS_MODIFY)));
                    if ( attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_OBSOLETE_FACTORY_COMPONENT_FACTORY) != null)
                    {
                        m_currentComponent.setObsoleteFactoryComponentFactory("true".equals(attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_OBSOLETE_FACTORY_COMPONENT_FACTORY)));
                    }
                    else if ( !namespaceCode.isDS13() )
                    {
                        m_currentComponent.setObsoleteFactoryComponentFactory(m_globalObsoleteFactoryComponentFactory);
                    }
                    m_currentComponent.setConfigureWithInterfaces("true".equals(attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_CONFIGURE_WITH_INTERFACES)));
                    m_currentComponent.setDelayedKeepInstances(m_globalDelayedKeepInstances || "true".equals(attributes.getValue(XmlConstants.NAMESPACE_URI_1_0_FELIX_EXTENSIONS, XmlConstants.ATTR_DELAYED_KEEP_INSTANCES)));

                    // activation-fields is optional (since DS 1.4)
                    String activationFields = attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_ACTIVATION_FIELDS );
                    if ( activationFields != null )
                    {
                        final String[] fields = activationFields.split(" ");
                        m_currentComponent.setActivationFields( fields );
                    }

                    // init is optional (since DS 1.4)
                    String init = attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_INIT );
                    if ( init != null )
                    {
                        m_currentComponent.setInit( init );
                    }

                    // Add this component to the list
                    m_components.add( m_currentComponent );
                }

                // not inside a component element, ignore current element
                else if ( !this.isComponent )
                {
                    m_logger.log( LogService.LOG_DEBUG,
                            "Not currently parsing a component; ignoring element {0} (bundle {1})", null,
                                    localName, m_bundle.getLocation() );
                }

                // 112.4.4 Implementation
                else if ( localName.equals( XmlConstants.EL_IMPL ) )
                {
                    // Set the implementation class name (mandatory)
                    m_currentComponent.setImplementationClassName( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "class" ) );
                }
                // 112.4.5 [...] Property Elements
                else if ( localName.equals( XmlConstants.EL_PROPERTY ) )
                {
                    PropertyMetadata prop = new PropertyMetadata();

                    // name attribute is mandatory
                    prop.setName( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_NAME ) );

                    // type attribute is optional
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_TYPE ) != null )
                    {
                        prop.setType( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_TYPE ) );
                    }

                    // 112.4.5: If the value attribute is specified, the body of the element is ignored.
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_VALUE ) != null )
                    {
                        prop.setValue( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_VALUE ) );
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
                    final Properties props = readPropertiesEntry( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "entry" ) );
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
                    prop.setName( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_NAME ) );

                    // type attribute is optional
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_TYPE ) != null )
                    {
                        prop.setType( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_TYPE ) );
                    }

                    // 112.4.5: If the value attribute is specified, the body of the element is ignored.
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_VALUE ) != null )
                    {
                        prop.setValue( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_VALUE ) );
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
                    final Properties props = readPropertiesEntry( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "entry" ) );
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
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "servicefactory" ) != null )
                    {
                        m_currentService.setServiceFactory( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "servicefactory" ).equals( "true" ) );
                    }

                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "scope" ) != null )
                    {
                        m_currentService.setScope( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "scope" ) );
                    }

                    m_currentComponent.setService( m_currentService );
                }
                else if ( localName.equals( XmlConstants.EL_PROVIDE ) )
                {
                    m_currentService.addProvide( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_INTERFACE ) );
                }

                // 112.4.7 Reference element
                else if ( localName.equals( XmlConstants.EL_REF ) )
                {
                    ReferenceMetadata ref = new ReferenceMetadata();

                    // name attribute is optional (since DS 1.1)
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_NAME ) != null )
                    {
                        ref.setName( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_NAME ) );
                    }

                    ref.setInterface( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, XmlConstants.ATTR_INTERFACE ) );

                    // Cardinality
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "cardinality" ) != null )
                    {
                        ref.setCardinality( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "cardinality" ) );
                    }

                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "policy" ) != null )
                    {
                        ref.setPolicy( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "policy" ) );
                    }

                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "policy-option" ) != null )
                    {
                        ref.setPolicyOption( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "policy-option" ) );
                    }

                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "scope" ) != null )
                    {
                        ref.setScope( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "scope" ) );
                    }

                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "target" ) != null)
                    {
                        ref.setTarget( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "target" ) );
                        PropertyMetadata prop = new PropertyMetadata();
                        prop.setName( (ref.getName() == null? ref.getInterface(): ref.getName()) + ".target");
                        prop.setValue( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "target" ) );
                        m_currentComponent.addProperty( prop );

                    }

                    // method reference
                    ref.setBind( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "bind" ) );
                    ref.setUpdated( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "updated" ) );
                    ref.setUnbind( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "unbind" ) );

                    // field reference
                    ref.setField( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "field" ) );
                    ref.setFieldOption( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "field-option" ) );
                    ref.setFieldCollectionType( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "field-collection-type" ) );

                    // DS 1.4 : references as parameter of the activator (method or constructor)
                    if ( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "parameter" ) != null)
                    {
                        ref.setParameter( attributes.getValue( XmlConstants.NAMESPACE_URI_EMPTY, "parameter" ) );

                    }

                    m_currentComponent.addDependency( ref );
                }

                // unexpected element (except the root element "components"
                // used by the Maven SCR Plugin, which is just silently ignored)
                else if ( !localName.equals( XmlConstants.EL_COMPONENTS ) )
                {
                    m_logger.log( LogService.LOG_DEBUG, "Ignoring unsupported element {0} (bundle {1})", null,
                            localName, m_bundle.getLocation() );
                }
            }
            catch ( Exception ex )
            {
                throw new SAXException( "Exception during parsing", ex );
            }
        }

        // unexpected namespace (except the root element "components"
        // used by the Maven SCR Plugin, which is just silently ignored)
        else if ( !localName.equals( XmlConstants.EL_COMPONENTS ) )
        {
            m_logger.log( LogService.LOG_DEBUG, "Ignoring unsupported element '{'{0}'}'{1} (bundle {2})", null,
                    uri, localName, m_bundle.getLocation()  );
        }
    }


    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
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
                if (propertyBuilder != null) {
                    m_pendingProperty.setValues(propertyBuilder.toString());
                    propertyBuilder = null;
                } else {
                    m_pendingProperty.setValues("");
                }
                m_currentComponent.addProperty( m_pendingProperty );
                m_pendingProperty = null;
            }
            else if ( localName.equals( XmlConstants.EL_FACTORY_PROPERTY ) && m_pendingFactoryProperty != null )
            {
                if (propertyBuilder != null) {
                    m_pendingFactoryProperty.setValues(propertyBuilder.toString());
                    propertyBuilder = null;
                } else {
                    m_pendingFactoryProperty.setValues("");
                }
                m_currentComponent.addFactoryProperty( m_pendingFactoryProperty );
                m_pendingFactoryProperty = null;
            }
        }
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        // 112.4.5 If the value attribute is not specified, the body must contain one or more values
        if ( m_pendingProperty != null || m_pendingFactoryProperty != null )
        {
            if (propertyBuilder == null) {
                propertyBuilder = new StringBuilder();
            }
            propertyBuilder.append(String.valueOf( ch, start, length));
        }
    }


    /**
     * Reads the name property file from the bundle owning this descriptor. All
     * properties read from the properties file are added to the current
     * component's property meta data list.
     *
     * @param entryName The name of the bundle entry containing the propertes
     *      to be added. This must not be <code>null</code>.
     *
     * @throws SAXException If the entry name is <code>null</code> or no
     *      entry with the given name exists in the bundle or an error occurrs
     *      reading the properties file.
     */
    private Properties readPropertiesEntry( String entryName ) throws SAXException
    {
        if ( entryName == null )
        {
            throw new SAXException( "Missing entry attribute of properties element", null );
        }

        URL entryURL = m_bundle.getEntry( entryName );
        if ( entryURL == null )
        {
            throw new SAXException( "Missing bundle entry " + entryName, null );
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
            throw new SAXException( "Failed to read properties entry " + entryName, ioe );
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
