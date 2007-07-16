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
package org.apache.felix.sandbox.preferences.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.sandbox.preferences.BackingStore;
import org.apache.felix.sandbox.preferences.PreferencesDescription;
import org.apache.felix.sandbox.preferences.PreferencesImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This is an abstract implementation of a backing store
 * which uses streams to read/write the preferences and
 * stores a complete preferences tree in a single stream.
 */
public abstract class StreamBackingStoreImpl implements BackingStore {

    /** The bundle context. */
    protected final BundleContext bundleContext;

    public StreamBackingStoreImpl(BundleContext context) {
        this.bundleContext = context;
    }

    /**
     * This method is invoked to check if the backing store is accessible right now.
     * @throws BackingStoreException
     */
    protected abstract void checkAccess() throws BackingStoreException;

    /**
     * Get the output stream to write the preferences.
     */
    protected abstract OutputStream getOutputStream(PreferencesDescription desc)
    throws IOException;

    /**
     * @see org.apache.felix.sandbox.preferences.BackingStore#store(org.apache.felix.sandbox.preferences.PreferencesImpl)
     */
    public void store(PreferencesImpl prefs) throws BackingStoreException {
        // do we need to store at all?
        if ( !this.hasChanges(prefs) ) {
            return;
        }
        this.checkAccess();
        final PreferencesImpl root = prefs.getRoot();
        try {
            final OutputStream os = this.getOutputStream(root.getDescription());
            this.write(root, os);
            os.close();
        } catch (IOException ioe) {
            throw new BackingStoreException("Unable to store preferences.", ioe);
        }
    }

    /**
     * Has the tree changes?
     */
    protected boolean hasChanges(PreferencesImpl prefs) {
        if ( prefs.getChangeSet().hasChanges() ) {
            return true;
        }
        final Iterator i = prefs.getChildren().iterator();
        while ( i.hasNext() ) {
            final PreferencesImpl current = (PreferencesImpl) i.next();
            if ( this.hasChanges(current) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Write the preferences recursively to the output stream.
     * @param prefs
     * @param os
     * @throws IOException
     */
    protected void write(PreferencesImpl prefs, OutputStream os)
    throws IOException {
        prefs.write(os);
        final ObjectOutputStream oos = new ObjectOutputStream(os);
        final Collection children = prefs.getChildren();
        oos.writeInt(children.size());
        oos.flush();
        final Iterator i = children.iterator();
        while ( i.hasNext() ) {
            final PreferencesImpl child = (PreferencesImpl) i.next();
            final byte[] name = child.name().getBytes("utf-8");
            oos.writeInt(name.length);
            oos.write(name);
            oos.flush();
            this.write(child, os);
        }
    }

    /**
     * Read the preferences recursively from the input stream.
     * @param prefs
     * @param is
     * @throws IOException
     */
    protected void read(PreferencesImpl prefs, InputStream is)
    throws IOException {
        prefs.read(is);
        final ObjectInputStream ois = new ObjectInputStream(is);
        final int numberOfChilren = ois.readInt();
        for(int i=0; i<numberOfChilren; i++) {
            int length = ois.readInt();
            final byte[] name = new byte[length];
            ois.readFully(name);
            final PreferencesImpl impl = (PreferencesImpl)prefs.node(new String(name, "utf-8"));
            this.read(impl, is);
        }
    }
}
