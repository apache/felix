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
package org.apache.felix.scr.impl.metadata;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetadataStoreHelper
{
    static final byte STRING_NULL = 0;
    static final byte STRING_OBJECT = 1;
    static final byte STRING_INDEX = 2;
    static final byte STRING_LONG = 3;

    public static class MetaDataReader
    {
        private final List<String> stringTable = new ArrayList<String>();

        public String readIndexedString(DataInputStream in) throws IOException
        {
            String s = readString(in);
            addToStringTable(s, in.readInt());
            return s;
        }

        public String readString(DataInputStream in) throws IOException
        {
            byte type = in.readByte();
            if (type == STRING_INDEX)
            {
                int index = in.readInt();
                return (String) stringTable.get(index);
            }
            if (type == STRING_NULL)
            {
                return null;
            }
            String s;
            if (type == STRING_LONG)
            {
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);
                s = new String(data, "UTF-8");
            }
            else
            {
                s = in.readUTF();
            }
            return s;
        }

        private void addToStringTable(String s, int index)
        {
            if (index == stringTable.size())
            {
                stringTable.add(s);
            }
            else if (index < stringTable.size())
            {
                stringTable.set(index, s);
            }
            else
            {
                while (stringTable.size() < index)
                {
                    stringTable.add(null);
                }
                stringTable.add(s);
            }
        }
    }

    public static class MetaDataWriter
    {
        private final Map<String, Integer> stringTable = new HashMap<>();

        public void writeIndexedString(String s, DataOutputStream out) throws IOException
        {
            writeString(s, out);
            out.writeInt(addToStringTable(s));
        }

        public void writeString(String s, DataOutputStream out) throws IOException
        {
            Integer index = s != null ? stringTable.get(s) : null;
            if (index != null)
            {
                out.writeByte(STRING_INDEX);
                out.writeInt(index);
                return;
            }

            if (s == null)
                out.writeByte(STRING_NULL);
            else
            {
                byte[] data = s.getBytes("UTF-8");

                if (data.length > 65535)
                {
                    out.writeByte(STRING_LONG);
                    out.writeInt(data.length);
                    out.write(data);
                }
                else
                {
                    out.writeByte(STRING_OBJECT);
                    out.writeUTF(s);
                }
            }
        }

        private int addToStringTable(String s)
        {
            if (s == null)
            {
                throw new NullPointerException();
            }
            Integer cur = stringTable.get(s);
            if (cur != null)
                throw new IllegalStateException(
                    "String is already in the write table: " + s);
            int index = stringTable.size();
            stringTable.put(s, Integer.valueOf(index));
            // return the index of the object just added
            return index;
        }
    }

    public static void addString(String s, Set<String> strings)
    {
        if (s != null)
        {
            strings.add(s);
        }
    }
}
