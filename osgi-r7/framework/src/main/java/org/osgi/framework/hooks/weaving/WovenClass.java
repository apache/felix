/*
 * Copyright (c) OSGi Alliance (2010, 2013). All Rights Reserved.
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

package org.osgi.framework.hooks.weaving;

import java.security.ProtectionDomain;
import java.util.List;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A class being woven.
 * 
 * This object represents a class being woven and is passed to each
 * {@link WeavingHook} for possible modification. It allows access to the most
 * recently transformed class file bytes and to any additional packages that
 * should be added to the bundle as dynamic imports.
 * 
 * <p>
 * Upon entering one of the terminal states, this object becomes effectively
 * immutable.
 * 
 * @NotThreadSafe
 * @author $Id: b86db7713c738ae7147fe86f754302e2e324676b $
 */
@ProviderType
public interface WovenClass {
	/**
	 * The woven class is being transformed.
	 * 
	 * <p>
	 * The woven class is in this state while {@link WeavingHook weaving hooks}
	 * are being called. The woven class is mutable so the {@link #getBytes()
	 * class bytes} may be {@link #setBytes(byte[]) modified} and
	 * {@link #getDynamicImports() dynamic imports} may be added. If a weaving
	 * hook throws an exception the state transitions to
	 * {@link #TRANSFORMING_FAILED}. Otherwise, after the last weaving hook has
	 * been successfully called, the state transitions to {@link #TRANSFORMED}.
	 * 
	 * @since 1.1
	 */
	int	TRANSFORMING		= 0x00000001;

	/**
	 * The woven class has been transformed.
	 * 
	 * <p>
	 * The woven class is in this state after {@link WeavingHook weaving hooks}
	 * have been called and before the class is defined. The woven class cannot
	 * be further transformed. The woven class is in this state while defining
	 * the class. If a failure occurs while defining the class, the state
	 * transitions to {@link #DEFINE_FAILED}. Otherwise, after the class has
	 * been defined, the state transitions to {@link #DEFINED}.
	 * 
	 * @since 1.1
	 */
	int	TRANSFORMED			= 0x00000002;

	/**
	 * The woven class has been defined.
	 * <p>
	 * The woven class is in this state after the class is defined. The woven
	 * class cannot be further transformed. This is a terminal state. Upon
	 * entering this state, this object is effectively immutable, the
	 * {@link #getBundleWiring() bundle wiring} has been updated with the
	 * {@link #getDynamicImports() dynamic import requirements} and the class
	 * has been {@link #getDefinedClass() defined}.
	 * 
	 * @since 1.1
	 */
	int	DEFINED				= 0x00000004;

	/**
	 * The woven class failed to transform.
	 * <p>
	 * The woven class is in this state if a {@link WeavingHook weaving hook}
	 * threw an exception. The woven class cannot be further transformed or
	 * defined. This is a terminal state. Upon entering this state, this object
	 * is effectively immutable.
	 * 
	 * @since 1.1
	 */
	int	TRANSFORMING_FAILED	= 0x00000008;

	/**
	 * The woven class failed to define.
	 * <p>
	 * The woven class is in this state when a failure occurs while defining the
	 * class. The woven class cannot be further transformed or defined. This is
	 * a terminal state. Upon entering this state, this object is effectively
	 * immutable.
	 * 
	 * @since 1.1
	 */
	int	DEFINE_FAILED		= 0x00000010;

	/**
	 * Returns the class file bytes to be used to define the
	 * {@link WovenClass#getClassName() named} class.
	 * 
	 * <p>
	 * While in the {@link #TRANSFORMING} state, this method returns a reference
	 * to the class files byte array contained in this object. After leaving the
	 * {@link #TRANSFORMING} state, this woven class can no longer be
	 * transformed and a copy of the class file byte array is returned.
	 * 
	 * @return The bytes to be used to define the
	 *         {@link WovenClass#getClassName() named} class.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[bundle,WEAVE]} and the Java runtime
	 *         environment supports permissions.
	 */
	public byte[] getBytes();

	/**
	 * Set the class file bytes to be used to define the
	 * {@link WovenClass#getClassName() named} class. This method must not be
	 * called outside invocations of the {@link WeavingHook#weave(WovenClass)
	 * weave} method by the framework.
	 * 
	 * <p>
	 * While in the {@link #TRANSFORMING} state, this method replaces the
	 * reference to the array contained in this object with the specified array.
	 * After leaving the {@link #TRANSFORMING} state, this woven class can no
	 * longer be transformed and this method will throw an
	 * {@link IllegalStateException}.
	 * 
	 * @param newBytes The new classfile that will be used to define the
	 *        {@link WovenClass#getClassName() named} class. The specified array
	 *        is retained by this object and the caller must not modify the
	 *        specified array.
	 * @throws NullPointerException If newBytes is {@code null}.
	 * @throws IllegalStateException If state is {@link #TRANSFORMED},
	 *         {@link #DEFINED}, {@link #TRANSFORMING_FAILED} or
	 *         {@link #DEFINE_FAILED}.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[bundle,WEAVE]} and the Java runtime
	 *         environment supports permissions.
	 */
	public void setBytes(byte[] newBytes);

	/**
	 * Returns the list of dynamic import package descriptions to add to the
	 * {@link #getBundleWiring() bundle wiring} for this woven class. Changes
	 * made to the returned list will be visible to later {@link WeavingHook
	 * weaving hooks} called with this object. The returned list must not be
	 * modified outside invocations of the {@link WeavingHook#weave(WovenClass)
	 * weave} method by the framework.
	 * 
	 * <p>
	 * After leaving the {@link #TRANSFORMING} state, this woven class can no
	 * longer be transformed and the returned list will be unmodifiable.
	 * 
	 * <p>
	 * If the Java runtime environment supports permissions, any modification to
	 * the returned list requires {@code AdminPermission[bundle,WEAVE]}.
	 * Additionally, any add or set modification requires
	 * {@code PackagePermission[package,IMPORT]}.
	 * 
	 * @return A list containing zero or more dynamic import package
	 *         descriptions to add to the bundle wiring for this woven class.
	 *         This list must throw {@code IllegalArgumentException} if a
	 *         malformed dynamic import package description is added.
	 * @see "Core Specification, Dynamic Import Package, for the syntax of a dynamic import package description."
	 */
	public List<String> getDynamicImports();

	/**
	 * Returns whether weaving is complete in this woven class. Weaving is
	 * complete after the class is defined.
	 * 
	 * @return {@code true} if {@link #getState() state} is {@link #DEFINED},
	 *         {@link #TRANSFORMING_FAILED} or {@link #DEFINE_FAILED};
	 *         {@code false} otherwise.
	 */
	public boolean isWeavingComplete();

	/**
	 * Returns the fully qualified name of the class being woven.
	 * 
	 * @return The fully qualified name of the class being woven.
	 */
	public String getClassName();

	/**
	 * Returns the protection domain to which the woven class will be assigned
	 * when it is defined.
	 * 
	 * @return The protection domain to which the woven class will be assigned
	 *         when it is defined, or {@code null} if no protection domain will
	 *         be assigned.
	 */
	public ProtectionDomain getProtectionDomain();

	/**
	 * Returns the class defined by this woven class. During weaving, this
	 * method will return {@code null}. Once weaving is
	 * {@link #isWeavingComplete() complete}, this method will return the class
	 * object if this woven class was used to define the class.
	 * 
	 * @return The class associated with this woven class, or {@code null} if
	 *         weaving is not complete, the class definition failed or this
	 *         woven class was not used to define the class.
	 */
	public Class<?> getDefinedClass();

	/**
	 * Returns the bundle wiring whose class loader will define the woven class.
	 * 
	 * @return The bundle wiring whose class loader will define the woven class.
	 */
	public BundleWiring getBundleWiring();

	/**
	 * Returns the current state of this woven class.
	 * <p>
	 * A woven class can be in only one state at any time.
	 * 
	 * @return Either {@link #TRANSFORMING}, {@link #TRANSFORMED},
	 *         {@link #DEFINED}, {@link #TRANSFORMING_FAILED} or
	 *         {@link #DEFINE_FAILED}.
	 * @since 1.1
	 */
	public int getState();
}
