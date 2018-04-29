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

package org.apache.felix.sigil.eclipse.internal.repository.manager;

import java.util.HashSet;
import java.util.Properties;

import org.apache.felix.sigil.common.repository.AbstractBundleRepository;
import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IBundleRepositoryListener;
import org.apache.felix.sigil.common.repository.IRepositoryProvider;
import org.apache.felix.sigil.common.repository.IRepositoryVisitor;
import org.apache.felix.sigil.eclipse.internal.model.repository.RepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;

/**
 * @author dave
 *
 */
public class EclipseBundleRepository extends AbstractBundleRepository implements IEclipseBundleRepository
{
    
    private final IRepositoryProvider provider;
    private final Properties properties;
    private final IRepositoryType type;
    
    private final HashSet<IBundleRepositoryListener> listeners = new HashSet<IBundleRepositoryListener>();
    
    private Exception exception;
    private IBundleRepository delegate;

    /**
     * @param provider
     * @param type 
     * @param id
     * @param properties
     */
    public EclipseBundleRepository(IRepositoryProvider provider, IRepositoryType type, String id, Properties properties)
    {
        super(id);
        this.provider = provider;
        this.type = type;
        this.properties = properties;
    }
    
    private synchronized IBundleRepository getDelegate() {
        if (delegate == null && exception == null) {
            try
            {
                delegate = provider.createRepository(getId(), properties);
                for (IBundleRepositoryListener listener : listeners) {
                    delegate.addBundleRepositoryListener(listener);
                }
                exception = null;
            }
            catch (Exception e)
            {
                delegate = null;
                exception = e;
            }
        }
        
        return delegate;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.repository.IBundleRepository#accept(org.apache.felix.sigil.common.repository.IRepositoryVisitor, int)
     */
    public void accept(IRepositoryVisitor visitor, int options)
    {
        IBundleRepository delegate = getDelegate();
        if ( delegate != null ) {
            delegate.accept(visitor, options);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.repository.IBundleRepository#addBundleRepositoryListener(org.apache.felix.sigil.common.repository.IBundleRepositoryListener)
     */
    public synchronized void addBundleRepositoryListener(IBundleRepositoryListener listener)
    {
        IBundleRepository delegate = getDelegate();
        if ( delegate != null ) {
            delegate.addBundleRepositoryListener(listener);
        }
        else {
            listeners.add(listener);
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.repository.IBundleRepository#removeBundleRepositoryListener(org.apache.felix.sigil.common.repository.IBundleRepositoryListener)
     */
    public synchronized void removeBundleRepositoryListener(IBundleRepositoryListener listener)
    {
        IBundleRepository delegate = getDelegate();
        if ( delegate != null ) {
            delegate.removeBundleRepositoryListener(listener);
        }
        else {
            listeners.remove(listener);
        }
    }    

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.repository.IBundleRepository#refresh()
     */
    public void refresh()
    {
        IBundleRepository delegate = getDelegate();
        if ( delegate == null ) {
            exception = null;
            getDelegate();
        }
        else {
            delegate.refresh();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.eclipse.internal.repository.manager.IEclipseBundleRepository#getModel()
     */
    public IRepositoryModel getModel()
    {
        return new RepositoryModel(getId(), type, properties, exception);
    }

}
