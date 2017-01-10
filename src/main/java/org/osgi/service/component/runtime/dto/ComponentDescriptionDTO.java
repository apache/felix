/*
 * Copyright (c) OSGi Alliance (2013, 2016). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.component.runtime.dto;

import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.BundleDTO;

/**
 * A representation of a declared component description.
 * 
 * @since 1.3
 * @NotThreadSafe
 * @author $Id$
 */
public class ComponentDescriptionDTO extends DTO {
	/**
	 * The name of the component.
	 * 
	 * <p>
	 * This is declared in the {@code name} attribute of the {@code component}
	 * element. This must be the default name if the component description does
	 * not declare a name.
	 */
	public String				name;

	/**
	 * The bundle declaring the component description.
	 */
	public BundleDTO			bundle;

	/**
	 * The component factory name.
	 * 
	 * <p>
	 * This is declared in the {@code factory} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description is not declared as a factory component.
	 */
	public String				factory;

	/**
	 * The service scope.
	 * 
	 * <p>
	 * This is declared in the {@code scope} attribute of the {@code service}
	 * element. This must be {@code null} if the component description does not
	 * declare any service interfaces.
	 */
	public String				scope;

	/**
	 * The fully qualified name of the implementation class.
	 * 
	 * <p>
	 * This is declared in the {@code class} attribute of the
	 * {@code implementation} element.
	 */
	public String				implementationClass;

	/**
	 * The initial enabled state.
	 * 
	 * <p>
	 * This is declared in the {@code enabled} attribute of the
	 * {@code component} element.
	 */
	public boolean				defaultEnabled;

	/**
	 * The immediate state.
	 * 
	 * <p>
	 * This is declared in the {@code immediate} attribute of the
	 * {@code component} element.
	 */
	public boolean				immediate;

	/**
	 * The fully qualified names of the service interfaces.
	 * 
	 * <p>
	 * These are declared in the {@code interface} attribute of the
	 * {@code provide} elements. The array must be empty if the component
	 * description does not declare any service interfaces.
	 */
	public String[]				serviceInterfaces;

	/**
	 * The component properties.
	 * <p>
	 * These are declared in the component description by the {@code property}
	 * and {@code properties} elements as well as the {@code target} attribute
	 * of the {@code reference} elements.
	 */
	public Map<String, Object>	properties;

	/**
	 * The referenced services.
	 * 
	 * <p>
	 * These are declared in the {@code reference} elements. The array must be
	 * empty if the component description does not declare references to any
	 * services.
	 */
	public ReferenceDTO[]			references;

	/**
	 * The name of the activate method.
	 * 
	 * <p>
	 * This is declared in the {@code activate} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description does not declare an activate method name.
	 */
	public String				activate;

	/**
	 * The name of the deactivate method.
	 * 
	 * <p>
	 * This is declared in the {@code deactivate} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description does not declare a deactivate method name.
	 */
	public String				deactivate;

	/**
	 * The name of the modified method.
	 * 
	 * <p>
	 * This is declared in the {@code modified} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description does not declare a modified method name.
	 */
	public String				modified;

	/**
	 * The configuration policy.
	 * 
	 * <p>
	 * This is declared in the {@code configuration-policy} attribute of the
	 * {@code component} element. This must be the default configuration policy
	 * if the component description does not declare a configuration policy.
	 */
	public String				configurationPolicy;

	/**
	 * The configuration pids.
	 * 
	 * <p>
	 * These are declared in the {@code configuration-pid} attribute of the
	 * {@code component} element. This must contain the default configuration
	 * pid if the component description does not declare a configuration pid.
	 */
	public String[]				configurationPid;

	/**
	 * The factory properties.
	 * <p>
	 * These are declared in the component description by the
	 * {@code factoryProperty} and {@code factoryProperties} elements. This must
	 * be {@code null} if the component description is not declared as a
	 * {@link #factory factory component}.
	 * 
	 * @since 1.4
	 */
	public Map<String,Object>	factoryProperties;

	/**
	 * The activation fields.
	 * <p>
	 * These are declared in the {@code activation-fields} attribute of the
	 * {@code component} element. The array must be empty if the component
	 * description does not declare any activation fields.
	 * 
	 * @since 1.4
	 */
	public String[]				activationFields;
}
