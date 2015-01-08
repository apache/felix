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
package org.apache.felix.framework;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.felix.framework.util.ImmutableList;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

class WovenClassImpl implements WovenClass, List<String>
{
    private final String m_className;
    private final BundleWiring m_wiring;
    private byte[] m_bytes;
    private List<String> m_imports = new ArrayList<String>();
    private Class m_definedClass = null;
    private boolean m_isComplete = false;
    private int m_state;

    /* package */WovenClassImpl(String className, BundleWiring wiring,
            byte[] bytes)
    {
        m_className = className;
        m_wiring = wiring;
        m_bytes = bytes;
        m_state = TRANSFORMING;
    }

    synchronized void complete(Class definedClass, byte[] bytes,
            List<String> imports)
    {
        completeDefine(definedClass);
        m_bytes = (bytes == null) ? m_bytes : bytes;
        completeImports(imports);
    }

    synchronized void completeImports(List<String> imports)
    {
        m_imports = (imports == null) ? ImmutableList.newInstance(m_imports)
                : ImmutableList.newInstance(imports);
    }

    synchronized void completeDefine(Class definedClass)
    {
        m_definedClass = definedClass;
    }

    public synchronized byte[] getBytes()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        byte[] bytes = m_bytes;
        if (m_isComplete)
        {
            bytes = new byte[m_bytes.length];
            System.arraycopy(m_bytes, 0, bytes, 0, m_bytes.length);
        }
        return bytes;
    }

    public synchronized void setBytes(byte[] bytes)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        if (m_state >= TRANSFORMED)
        {
            throw new IllegalStateException(
                    "Cannot change bytes after class weaving is completed.");
        } else
        {
            m_bytes = bytes;
        }
    }

    synchronized List<String> getDynamicImportsInternal()
    {
        return m_imports;
    }

    public synchronized List<String> getDynamicImports()
    {
        return this;
    }

    public synchronized boolean isWeavingComplete()
    {
        return m_isComplete;
    }

    public String getClassName()
    {
        return m_className;
    }

    public ProtectionDomain getProtectionDomain()
    {
        return ((BundleImpl) m_wiring.getRevision().getBundle())
                .getProtectionDomain();
    }

    public synchronized Class<?> getDefinedClass()
    {
        return m_definedClass;
    }

    public BundleWiring getBundleWiring()
    {
        return m_wiring;
    }

    //
    // List<String> implementation for dynamic imports.
    //
    // Design-wise this could be separated out into a separate type,
    // but since it will only ever be used for this purpose it didn't
    // appear to make much sense to introduce another type for it.

    public synchronized int size()
    {
        return m_imports.size();
    }

    public synchronized boolean isEmpty()
    {
        return m_imports.isEmpty();
    }

    public synchronized boolean contains(Object o)
    {
        return m_imports.contains(o);
    }

    public synchronized Iterator<String> iterator()
    {
        return m_imports.iterator();
    }

    public synchronized Object[] toArray()
    {
        return m_imports.toArray();
    }

    public synchronized <T> T[] toArray(T[] ts)
    {
        return m_imports.toArray(ts);
    }

    public synchronized boolean add(String s)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        if (s != null)
        {
            try
            {
                List<BundleRequirement> reqs = ManifestParser
                        .parseDynamicImportHeader(null, null, s);
            } catch (Exception ex)
            {
                RuntimeException re = new IllegalArgumentException(
                        "Unable to parse dynamic import.");
                re.initCause(ex);
                throw re;
            }
            checkImport(s);
            return m_imports.add(s);
        }
        return false;
    }

    private void checkImport(String s)
    {
        SecurityManager sm = System.getSecurityManager();

        if (sm != null)
        {
            sm.checkPermission(new PackagePermission(s, PackagePermission.IMPORT));
        }
    }

    public synchronized boolean remove(Object o)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        return m_imports.remove(o);
    }

    public synchronized boolean containsAll(Collection<?> collection)
    {
        return m_imports.containsAll(collection);
    }

    public synchronized boolean addAll(Collection<? extends String> collection)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        for (String s : collection)
        {
            try
            {
                List<BundleRequirement> reqs = ManifestParser
                        .parseDynamicImportHeader(null, null, s);
            } catch (Exception ex)
            {
                RuntimeException re = new IllegalArgumentException(
                        "Unable to parse dynamic import.");
                re.initCause(ex);
                throw re;
            }
            checkImport(s);
        }
        return m_imports.addAll(collection);
    }

    public synchronized boolean addAll(int i,
            Collection<? extends String> collection)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        for (String s : collection)
        {
            try
            {
                List<BundleRequirement> reqs = ManifestParser
                        .parseDynamicImportHeader(null, null, s);
            } catch (Exception ex)
            {
                RuntimeException re = new IllegalArgumentException(
                        "Unable to parse dynamic import.");
                re.initCause(ex);
                throw re;
            }
            checkImport(s);
        }
        return m_imports.addAll(i, collection);
    }

    public synchronized boolean removeAll(Collection<?> collection)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        return m_imports.removeAll(collection);
    }

    public synchronized boolean retainAll(Collection<?> collection)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        return m_imports.retainAll(collection);
    }

    public synchronized void clear()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        m_imports.clear();
    }

    public synchronized String get(int i)
    {
        return m_imports.get(i);
    }

    public synchronized String set(int i, String s)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        try
        {
            List<BundleRequirement> reqs = ManifestParser
                    .parseDynamicImportHeader(null, null, s);
        } catch (Exception ex)
        {
            RuntimeException re = new IllegalArgumentException(
                    "Unable to parse dynamic import.");
            re.initCause(ex);
            throw re;
        }
        checkImport(s);
        return m_imports.set(i, s);
    }

    public synchronized void add(int i, String s)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        try
        {
            List<BundleRequirement> reqs = ManifestParser
                    .parseDynamicImportHeader(null, null, s);
        } catch (Exception ex)
        {
            RuntimeException re = new IllegalArgumentException(
                    "Unable to parse dynamic import.");
            re.initCause(ex);
            throw re;
        }
        checkImport(s);
        m_imports.add(i, s);
    }

    public synchronized String remove(int i)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new AdminPermission(m_wiring.getBundle(),
                    AdminPermission.WEAVE));
        }
        return m_imports.remove(i);
    }

    public synchronized int indexOf(Object o)
    {
        return m_imports.indexOf(o);
    }

    public synchronized int lastIndexOf(Object o)
    {
        return m_imports.lastIndexOf(o);
    }

    public synchronized ListIterator<String> listIterator()
    {
        return m_imports.listIterator();
    }

    public synchronized ListIterator<String> listIterator(int i)
    {
        return m_imports.listIterator(i);
    }

    public synchronized List<String> subList(int i, int i1)
    {
        return m_imports.subList(i, i1);
    }

    byte[] _getBytes()
    {
        byte[] bytes = m_bytes;
        if (m_isComplete)
        {
            bytes = new byte[m_bytes.length];
            System.arraycopy(m_bytes, 0, bytes, 0, m_bytes.length);
        }
        return bytes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.hooks.weaving.WovenClass#getState()
     */
    public synchronized int getState()
    {
        return m_state;
    }

    public synchronized void setState(int state)
    {
        // Per 56.6.4.13 Weaving complete if state is DEFINED, DEFINE_FAILED, or
        // TRANSFORMING_FAILED
        if (!m_isComplete
                && (state == DEFINED || state == DEFINE_FAILED || state == TRANSFORMING_FAILED))
        {
            m_isComplete = true;
            if (state == DEFINED || state == DEFINE_FAILED)
            {
                BundleProtectionDomain pd = (BundleProtectionDomain)
                    ((BundleRevisionImpl) m_wiring.getRevision()).getProtectionDomain();
                for (String s : m_imports)
                {
                    pd.addWoven(s);
                }
            }
        }
        if(state == TRANSFORMED)
        {
            completeImports(null);
        }
        m_state = state;
    }

}