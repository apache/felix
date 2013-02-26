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
package org.apache.felix.inventory;

/**
 * Enumeration for the different printer modes.
 */
public final class PrinterMode {

    // plain text
    public static PrinterMode TEXT = new PrinterMode("TEXT");

    // HTML which can be placed inside a HTML body element (no external references)
    public static PrinterMode HTML_BODY = new PrinterMode("HTML_BODY");

    // JSON output
    public static PrinterMode JSON = new PrinterMode("JSON");

    // file content for a zip
    public static PrinterMode ZIP_FILE_TEXT = new PrinterMode("ZIP_FILE_TEXT");

    // json file content for a zip
    public static PrinterMode ZIP_FILE_JSON = new PrinterMode("ZIP_FILE_JSON");

    private final String mode;

    private PrinterMode(final String mode) {
        this.mode = mode;
    }

    public static PrinterMode valueOf(final String m) {
        if ( TEXT.name().equals(m) ) {
            return TEXT;
        } else if ( HTML_BODY.equals(m) ) {
            return HTML_BODY;
        } else if ( JSON.equals(m) ) {
            return JSON;
        } else if ( ZIP_FILE_TEXT.equals(m) ) {
            return ZIP_FILE_TEXT;
        } else if ( ZIP_FILE_JSON.equals(m) ) {
            return ZIP_FILE_JSON;
        }
        return null;
    }

    public String name() {
        return this.mode;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return mode.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PrinterMode other = (PrinterMode) obj;
        return mode.equals(other.mode);
    }
}
