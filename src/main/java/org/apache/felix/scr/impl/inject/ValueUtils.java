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
package org.apache.felix.scr.impl.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Utility methods for handling references and activation
 */
public class ValueUtils {

	/**
	 * The value type of the field, activation field or constructor parameter
	 */
	public enum ValueType
    {
        ignore,
        componentContext,
        bundleContext,
        config_map,
        config_annotation,
        ref_serviceReference,
        ref_serviceObjects,
        ref_serviceType,
        ref_map,
        ref_tuple
    }
    
    /** Empty array. */
	public static final ValueType[] EMPTY_VALUE_TYPES = new ValueType[0];

    /**
     * Get the value type for the parameter class.
     * @param typeClass The class of the parameter
     * @return The value type
     */
    public static ValueType getValueType( final Class<?> typeClass )
    {
        if ( typeClass == ClassUtils.COMPONENT_CONTEXT_CLASS )
        {
        	return ValueType.componentContext;
        }
        else if ( typeClass == ClassUtils.BUNDLE_CONTEXT_CLASS )
        {
        	return ValueType.bundleContext;
        }
        else if ( typeClass == ClassUtils.MAP_CLASS )
        {
        	return ValueType.config_map;
        }
        else
        {
        	return ValueType.config_annotation;
        }
    }

    /**
     * Get the value type of the reference
     * @param componentClass The component class declaring the reference
     * @param metadata The reference metadata
     * @param typeClass The type of the field/parameter
     * @param f The optional field. If null, this is a constructor reference
     * @param logger The logger
     * @return The value type for the field. If invalid, {@code ValueType#ignore}
     */
    public static ValueType getReferenceValueType( 
    		final Class<?> componentClass,
    		final ReferenceMetadata metadata,
    		final Class<?> typeClass, 
    		final Field field,
    		final SimpleLogger logger )
    {
        final Class<?> referenceType = ClassUtils.getClassFromComponentClassLoader(
                componentClass, metadata.getInterface(), logger);

        ValueType valueType = ValueType.ignore;
        
        // unary reference
        if ( !metadata.isMultiple() )
        {
            if ( typeClass.isAssignableFrom(referenceType) )
            {
                valueType = ValueType.ref_serviceType;
            }
            else if ( typeClass == ClassUtils.SERVICE_REFERENCE_CLASS )
            {
                valueType = ValueType.ref_serviceReference;
            }
            else if ( typeClass == ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS )
            {
                valueType = ValueType.ref_serviceObjects;
            }
            else if ( typeClass == ClassUtils.MAP_CLASS )
            {
                valueType = ValueType.ref_map;
            }
            else if ( typeClass == ClassUtils.MAP_ENTRY_CLASS )
            {
                valueType = ValueType.ref_tuple;
            }
            else
            {
            	if ( field != null )
            	{
	                logger.log( LogService.LOG_ERROR, "Field {0} in component {1} has unsupported type {2}", new Object[]
	                        {metadata.getField(), componentClass, typeClass.getName()}, null );
            	}
            	else
            	{
	                logger.log( LogService.LOG_ERROR, "Constructor argument {0} in component {1} has unsupported type {2}", new Object[]
	                        {metadata.getParameterIndex(), componentClass, typeClass.getName()}, null );            		
            	}
                valueType = ValueType.ignore;
            }

            // if the field is dynamic, it has to be volatile (field is ignored, case logged) (112.3.8.1)
            if ( field != null && !metadata.isStatic() && !Modifier.isVolatile(field.getModifiers()) ) {
                logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must be declared volatile to handle a dynamic reference", new Object[]
                        {metadata.getField(), componentClass}, null );
                valueType = ValueType.ignore;
            }

            // the field must not be final (field is ignored, case logged) (112.3.8.1)
            if ( field != null && Modifier.isFinal(field.getModifiers()) )
            {
                logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must not be declared as final", new Object[]
                        {metadata.getField(), componentClass}, null );
                valueType = ValueType.ignore;
            }
        }
        else
        {
            if ( ReferenceMetadata.FIELD_VALUE_TYPE_SERVICE.equals(metadata.getFieldCollectionType()) )
            {
                valueType = ValueType.ref_serviceType;
            }
            else if ( ReferenceMetadata.FIELD_VALUE_TYPE_REFERENCE.equals(metadata.getFieldCollectionType()) )
            {
                valueType = ValueType.ref_serviceReference;
            }
            else if ( ReferenceMetadata.FIELD_VALUE_TYPE_SERVICEOBJECTS.equals(metadata.getFieldCollectionType()) )
            {
                valueType = ValueType.ref_serviceObjects;
            }
            else if ( ReferenceMetadata.FIELD_VALUE_TYPE_PROPERTIES.equals(metadata.getFieldCollectionType()) )
            {
                valueType = ValueType.ref_map;
            }
            else if ( ReferenceMetadata.FIELD_VALUE_TYPE_TUPLE.equals(metadata.getFieldCollectionType()) )
            {
                valueType = ValueType.ref_tuple;
            }

            // multiple cardinality, field type must be collection or subtype
            if ( !ClassUtils.COLLECTION_CLASS.isAssignableFrom(typeClass) )
            {
            	if ( field != null )
            	{
	                logger.log( LogService.LOG_ERROR, "Field {0} in component {1} has unsupported type {2}", new Object[]
	                        {metadata.getField(), componentClass, typeClass.getName()}, null );
            	}
            	else
            	{
                    logger.log( LogService.LOG_ERROR, "Constructor argument {0} in component {1} has unsupported type {2}", new Object[]
                            {metadata.getParameterIndex(), componentClass, typeClass.getName()}, null );            		
            	}
                valueType = ValueType.ignore;
            }

            // additional checks for replace strategy:
            if ( metadata.isReplace() && field != null )
            {
                // if the field is dynamic wit has to be volatile (field is ignored, case logged) (112.3.8.1)
                if ( !metadata.isStatic() && !Modifier.isVolatile(field.getModifiers()) )
                {
                    logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must be declared volatile to handle a dynamic reference", new Object[]
                            {metadata.getField(), componentClass}, null );
                    valueType = ValueType.ignore;
                }

                // replace strategy: field must not be final (field is ignored, case logged) (112.3.8.1)
                //                   only collection and list allowed
                if ( typeClass != ClassUtils.LIST_CLASS && typeClass != ClassUtils.COLLECTION_CLASS )
                {
                    logger.log( LogService.LOG_ERROR, "Field {0} in component {1} has unsupported type {2}."+
                        " It must be one of java.util.Collection or java.util.List.",
                        new Object[] {metadata.getField(), componentClass, typeClass.getName()}, null );
                    valueType = ValueType.ignore;

                }
                if ( Modifier.isFinal(field.getModifiers()) )
                {
                    logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must not be declared as final", new Object[]
                            {metadata.getField(), componentClass}, null );
                    valueType = ValueType.ignore;
                }
            }
        }
        // static references only allowed for replace strategy
        if ( metadata.isStatic() && !metadata.isReplace() )
        {
            logger.log( LogService.LOG_ERROR, "Update strategy for field {0} in component {1} only allowed for non static field references.", new Object[]
                    {metadata.getField(), componentClass}, null );
            valueType = ValueType.ignore;
        }
        return valueType;
    }

    /**
     * Get the value for the parameter type
     * @param type The parameter type
     * @param componentContext The component context
     */
    @SuppressWarnings("unchecked")
	public static Object getValue( final ValueType type,
			final Class<?> targetType,
    		@SuppressWarnings("rawtypes") final ComponentContextImpl componentContext,
    		final RefPair<?, ?> refPair)
    {
    	final Object value;
    	switch ( type ) {
    		case ignore : value = null; break;
    		case componentContext : value = componentContext; break;
    		case bundleContext : value = componentContext.getBundleContext(); break;
    		case config_map : // note: getProperties() returns a ReadOnlyDictionary which is a Map
    			              value = componentContext.getProperties(); break;
    		case config_annotation : value = Annotations.toObject(targetType,
                    (Map<String, Object>) componentContext.getProperties(),
                    componentContext.getBundleContext().getBundle(), componentContext.getComponentMetadata().isConfigureWithInterfaces());
    		        break;
    		case ref_serviceType : value = refPair.getServiceObject(componentContext); break;
    		case ref_serviceReference : value = refPair.getRef(); break;
    		case ref_serviceObjects : value = componentContext.getComponentServiceObjectsHelper().getServiceObjects(refPair.getRef()); break;
    		case ref_map : value = new ReadOnlyDictionary( refPair.getRef() ); break;
    		case ref_tuple : final Object tupleKey = new ReadOnlyDictionary( refPair.getRef() );
                 final Object tupleValue = refPair.getServiceObject(componentContext);
                 value = new MapEntryImpl(tupleKey, tupleValue, refPair.getRef());
                 break;
            default: value = null;
    	}
    	return value;
    }

    /**
     * Comparable map entry using the service reference to compare.
     */
    @SuppressWarnings("rawtypes")
    private static final class MapEntryImpl implements Map.Entry, Comparable<Map.Entry<?, ?>>
    {

        private final Object key;
        private final Object value;
        private final ServiceReference<?> ref;

        public MapEntryImpl(final Object key,
                final Object value,
                final ServiceReference<?> ref)
        {
            this.key = key;
            this.value = value;
            this.ref = ref;
        }

        public Object getKey()
        {
            return this.key;
        }

        public Object getValue()
        {
            return this.value;
        }

        public Object setValue(final Object value)
        {
            throw new UnsupportedOperationException();
        }

        public int compareTo(final Map.Entry<?, ?> o)
        {
            if ( o == null )
            {
                return 1;
            }
            if ( o instanceof MapEntryImpl )
            {
                final MapEntryImpl other = (MapEntryImpl)o;
                return ref.compareTo(other.ref);

            }
            return new Integer(this.hashCode()).compareTo(o.hashCode());
        }
    }
}
