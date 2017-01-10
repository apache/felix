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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Utility methods for handling field and constructor injection.
 */
public class FieldUtils {

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
	public static final ValueType[] EMPTY_TYPES = new ValueType[0];

	/**
	 * Return type for {@link FieldUtils#searchField(Class, String, SimpleLogger)}
	 */
    public static final class FieldSearchResult 
	{
		public final Field field;
		public final boolean usable;
		
		public FieldSearchResult(final Field f, final boolean usable)
		{
			this.field = f;
			this.usable = usable;
		}
	}
	
    /**
     * Searches the field named {@code fieldName} in the given
     * {@code targetClass}. If the target class has no acceptable field
     * the class hierarchy is traversed until a field is found or the root
     * of the class hierarchy is reached without finding a field.
     * <p>
     * If an unexpected error occurs while searching, {@code null} is
     * returned. In all other cases a {@code FieldSearchResult} is
     * returned. If no field is found, {@code FieldSearchResult#field}
     * is set to {@code null}. If the field is found, but not usable
     * (e.g. due to visibility restrictions), {@code FieldSearchResult#usable}
     * is set to {@code false}.
     *
     * @param targetClass The class of the component
     * @param fieldName The name of the field
     * @param logger A logger to log errors / problems
     * @return A field search result or {@code null} if an unexpected
     *         error occurred.
     */
    public static FieldSearchResult searchField( final Class<?> componentClass,
    		final String fieldName,
    		final SimpleLogger logger )
    {
        final ClassLoader targetClasslLoader = componentClass.getClassLoader();
        final String targetPackage = ClassUtils.getPackageName( componentClass );
        Class<?> theClass = componentClass;
        boolean acceptPrivate = true;
        boolean acceptPackage = true;
        while (true)
        {

            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG,
                    "Locating field " + fieldName + " in class " + theClass.getName(), null );
            }

            try 
            {
	            final FieldSearchResult result = getField( componentClass, theClass, fieldName, acceptPrivate, acceptPackage, logger );
	            if ( result != null )
	            {
	                return result;
	            }
            }
            catch ( final InvocationTargetException ex )
            {
                logger.log( LogService.LOG_WARNING, "{0} cannot be found in component class {1}", new Object[]
                        {fieldName, componentClass.getName()}, ex.getTargetException() );
                return null;
            }

            // if we get here, we have no field, so check the super class
            theClass = theClass.getSuperclass();
            if ( theClass == null )
            {
                break;
            }

            // super class field check ignores private fields and accepts
            // package fields only if in the same package and package
            // fields are (still) allowed
            acceptPackage &= targetClasslLoader == theClass.getClassLoader()
                && targetPackage.equals( ClassUtils.getPackageName( theClass ) );

            // private fields will not be accepted any more in super classes
            acceptPrivate = false;
        }

        // nothing found 
        logger.log( LogService.LOG_WARNING, "{0} cannot be found in component class {1}", new Object[]
                        {fieldName, componentClass.getName()}, null );
        return new FieldSearchResult(null, false);
    }

    /**
     * Finds the field named {@code fieldName} field in the given
     * {@code targetClass}. If the target class has no acceptable field
     * the class hierarchy is traversed until a field is found or the root
     * of the class hierarchy is reached without finding a field.
     *
     * @param componentClass The class of the component (for logging)
     * @param targetClass The class in which to look for the method
     * @param fieldName The name of the field
     * @param acceptPrivate {@code true} if private fields should be
     *      considered.
     * @param acceptPackage {@code true} if package private fields should
     *      be considered.
     * @param logger For error logging
     * @return If the field is found a {@code FieldSearchResult} is returned.
     *         If the field is not found, {@code null} is returned.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested field.
     */
    private static FieldSearchResult getField( final Class<?> componentClass,
    		final Class<?> targetClass,
    		final String fieldName,
            final boolean acceptPrivate,
            final boolean acceptPackage,
            final SimpleLogger logger )
    throws InvocationTargetException
    {
        try
        {
            // find the declared field in this class
            final Field field = targetClass.getDeclaredField( fieldName );

            // accept public and protected fields only and ensure accessibility
            return accept( componentClass, field, acceptPrivate, acceptPackage, logger );
        }
        catch ( NoSuchFieldException nsfe )
        {
            // thrown if no field is declared with the given name and
            // parameters
            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG, "Declared Field {0}.{1} not found", new Object[]
                    { targetClass.getName(), fieldName }, null );
            }
        }
        catch ( Throwable throwable )
        {
            // unexpected problem accessing the field, don't let everything
            // blow up in this situation, just throw a declared exception
            throw new InvocationTargetException( throwable, "Unexpected problem trying to get field " + fieldName );
        }

        // caught and ignored exception, assume no field and continue search
        return null;
    }

    /**
     * This method checks whether the found field is acceptable (= usable)
     * for the component instance.
     * It returns a {@code FieldSearchResult} with the usable flag set to
     * {@code true} if the field is not static and
     * <ul>
     * <li>Is public or protected</li>
     * <li>Is private and {@code acceptPrivate} is {@code true}</li>
     * <li>Is package private and {@code acceptPackage} is {@code true}</li>
     * </ul>
     * <p>
     * If the field is usable, this method makes the field accessible.
     * <p>
     * If the field is not usable, a {@code FieldSearchResult} with the usable 
     * flag set to {@code false} is returned and an error is logged with
     * the provided logger.
     *
     * @param componentClass The class of the component,.
     * @param field The field to check
     * @param acceptPrivate Whether a private field is acceptable
     * @param acceptPackage Whether a package private field is acceptable
     * @param logger The logger for error logging
     * @return A field search result, this is never {@code null}
     */
    private static FieldSearchResult accept( final Class<?> componentClass,
    		final Field field,
            final boolean acceptPrivate,
            final boolean acceptPackage,
            final SimpleLogger logger)
    {
        // check modifiers now
        final int mod = field.getModifiers();

        // static fields
        if ( Modifier.isStatic( mod ) )
        {
            logger.log( LogService.LOG_ERROR, "Field {0} must not be static", new Object[]
                    { toString(componentClass, field) }, null );
            return new FieldSearchResult(field, false);
        }

        // accept public and protected fields
        if ( Modifier.isPublic( mod ) || Modifier.isProtected( mod ) )
        {
            setAccessible( field );
            return new FieldSearchResult(field, true);
        }

        // accept private if accepted
        if ( Modifier.isPrivate( mod ) )
        {
            if ( acceptPrivate )
            {
                setAccessible( field );
                return new FieldSearchResult(field, true);
            }

        } else {

        	// accept default (package)
        	if ( acceptPackage )
        	{
        		setAccessible( field );
        		return new FieldSearchResult(field, true);
        	}
        }

        // else don't accept
        // the method would fit the requirements but is not acceptable
        logger.log( LogService.LOG_ERROR,
                "findField: Suitable but non-accessible field {0}", new Object[]
                    { toString(componentClass, field) }, null );
        return new FieldSearchResult(field, false);
    }

    /**
     * Return a string representation of the field
     * @param componentClass The component class
     * @param field The field
     * @return A string representation of the field
     */
    public static String toString(final Class<?> componentClass,
    		final Field field)
    {
    	if ( componentClass.getName().equals(field.getDeclaringClass().getName()) ) 
    	{
    		return field.getName() + " in component class " + componentClass.getName();
    	}
    	return field.getName() + " in class " + field.getDeclaringClass().getName() + ", subclass of component class " + componentClass.getName();
    }
    
    /**
     * Make the field accessible
     * @param field The field
     */
    private static void setAccessible(final Field field)
    {
        AccessController.doPrivileged( new PrivilegedAction<Object>()
        {
            public Object run()
            {
                field.setAccessible( true );
                return null;
            }
        } );
    }

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
