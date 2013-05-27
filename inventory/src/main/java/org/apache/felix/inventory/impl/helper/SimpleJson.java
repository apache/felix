/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.inventory.impl.helper;

/**
 * The <code>SimpleJson</code> is an extremely simple and very limited
 * helper class to create JSON formatted output. The limits are as follows:
 * <ol>
 * <li>There is no error checking</li>
 * <li>Arrays are always expected to be inside an object</li>
 * <li>At most one level of object nesting is supported</li>
 * <li>Strings are not escaped</li>
 * <li>Only string values are supported</li>
 * </ol>
 */
class SimpleJson
{

    private StringBuffer index = new StringBuffer();

    /*
     * "o" - object; require ";" separator
     * "f" - object; no separator; next "o"
     * "a" - array; require "," separator
     * "i" - array; no separator; next "a"
     */
    private char mode = 0;

    SimpleJson object()
    {
        this.index.append('{');
        this.mode = 'f';
        return this;
    }

    SimpleJson endObject()
    {
        this.index.append('}');
        this.mode = 'o';
        return this;
    }

    SimpleJson array()
    {
        this.index.append('[');
        this.mode = 'i';
        return this;
    }

    SimpleJson endArray()
    {
        this.index.append(']');
        this.mode = 'o';
        return this;
    }

    SimpleJson key(final String key)
    {
        if (this.mode == 'f')
        {
            this.mode = 'o';
        }
        else if (mode == 'o')
        {
            this.index.append(',');
        }
        this.index.append('"').append(key).append("\":");
        return this;
    }

    SimpleJson value(final String value)
    {
        if (this.mode == 'i')
        {
            this.mode = 'a';
        }
        else if (mode == 'a')
        {
            this.index.append(',');
        }
        this.index.append('"').append(value).append('"');
        return this;
    }

    public String toString()
    {
        return this.index.toString();
    }
}
