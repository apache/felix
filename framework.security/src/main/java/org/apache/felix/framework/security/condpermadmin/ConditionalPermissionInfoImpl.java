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
package org.apache.felix.framework.security.condpermadmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.felix.framework.security.util.Permissions;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * Simple storage class for condperminfos. Additionally, this class can be used
 * to encode and decode infos.
 */
public final class ConditionalPermissionInfoImpl implements
    ConditionalPermissionInfo
{
    private static final Random RANDOM = new Random();
    static final ConditionInfo[] CONDITION_INFO = new ConditionInfo[0];
    static final PermissionInfo[] PERMISSION_INFO = new PermissionInfo[0];
    private final Object m_lock = new Object();
    private final String m_name;
    private final boolean m_allow;
    private volatile ConditionalPermissionAdminImpl m_cpai;
    private ConditionInfo[] m_conditions;
    private PermissionInfo[] m_permissions;
    
    private int parseConditionInfo(char[] encoded, int idx, List conditions) {
        String type;
        String[] args;
        try {
            int pos = idx;

            /* skip whitespace */
            while (Character.isWhitespace(encoded[pos])) {
                    pos++;
            }

            /* the first character must be '[' */
            if (encoded[pos] != '[') {
                    throw new IllegalArgumentException("expecting open bracket");
            }
            pos++;

            /* skip whitespace */
            while (Character.isWhitespace(encoded[pos])) {
                    pos++;
            }

            /* type is not quoted or encoded */
            int begin = pos;
            while (!Character.isWhitespace(encoded[pos])
                            && (encoded[pos] != ']')) {
                    pos++;
            }
            if (pos == begin || encoded[begin] == '"') {
                    throw new IllegalArgumentException("expecting type");
            }
            type = new String(encoded, begin, pos - begin);

            /* skip whitespace */
            while (Character.isWhitespace(encoded[pos])) {
                    pos++;
            }

            /* type may be followed by args which are quoted and encoded */
            ArrayList argsList = new ArrayList();
            while (encoded[pos] == '"') {
                    pos++;
                    begin = pos;
                    while (encoded[pos] != '"') {
                            if (encoded[pos] == '\\') {
                                    pos++;
                            }
                            pos++;
                    }
                    argsList.add(unescapeString(encoded, begin, pos));
                    pos++;

                    if (Character.isWhitespace(encoded[pos])) {
                            /* skip whitespace */
                            while (Character.isWhitespace(encoded[pos])) {
                                    pos++;
                            }
                    }
            }
            args = (String[]) argsList
                            .toArray(new String[argsList.size()]);

            /* the final character must be ']' */
            char c = encoded[pos++];
            if (c != ']') {
                    throw new IllegalArgumentException("expecting close bracket");
            }
            conditions.add(new ConditionInfo(type, args));
            return pos;
    }
    catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("parsing terminated abruptly");
    }
    }
    
    private int parsePermissionInfo(char[] encoded, int idx, List permissions) 
    {
        String parsedType = null;
        String parsedName = null;
        String parsedActions = null;
        try {
                int pos = idx;

                /* skip whitespace */
                while (Character.isWhitespace(encoded[pos])) {
                        pos++;
                }

                /* the first character must be '(' */
                if (encoded[pos] != '(') {
                        throw new IllegalArgumentException("expecting open parenthesis");
                }
                pos++;

                /* skip whitespace */
                while (Character.isWhitespace(encoded[pos])) {
                        pos++;
                }

                /* type is not quoted or encoded */
                int begin = pos;
                while (!Character.isWhitespace(encoded[pos])
                                && (encoded[pos] != ')')) {
                        pos++;
                }
                if (pos == begin || encoded[begin] == '"') {
                        throw new IllegalArgumentException("expecting type");
                }
                parsedType = new String(encoded, begin, pos - begin);

                /* skip whitespace */
                while (Character.isWhitespace(encoded[pos])) {
                        pos++;
                }

                /* type may be followed by name which is quoted and encoded */
                if (encoded[pos] == '"') {
                        pos++;
                        begin = pos;
                        while (encoded[pos] != '"') {
                                if (encoded[pos] == '\\') {
                                        pos++;
                                }
                                pos++;
                        }
                        parsedName = unescapeString(encoded, begin, pos);
                        pos++;

                        if (Character.isWhitespace(encoded[pos])) {
                                /* skip whitespace */
                                while (Character.isWhitespace(encoded[pos])) {
                                        pos++;
                                }

                                /*
                                 * name may be followed by actions which is quoted and
                                 * encoded
                                 */
                                if (encoded[pos] == '"') {
                                        pos++;
                                        begin = pos;
                                        while (encoded[pos] != '"') {
                                                if (encoded[pos] == '\\') {
                                                        pos++;
                                                }
                                                pos++;
                                        }
                                        parsedActions = unescapeString(encoded, begin, pos);
                                        pos++;

                                        /* skip whitespace */
                                        while (Character.isWhitespace(encoded[pos])) {
                                                pos++;
                                        }
                                }
                        }
                }

                /* the final character must be ')' */
                char c = encoded[pos++];
                if (c != ')') {
                        throw new IllegalArgumentException(
                                        "expecting close parenthesis");
                }
                permissions.add(new PermissionInfo(parsedType,parsedName, parsedActions));
                return pos;
        }
        catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("parsing terminated abruptly");
        }
    }
    /**
     * Takes an encoded character array and decodes it into a new String.
     */
    private static String unescapeString(char[] str, int begin, int end) {
            StringBuffer output = new StringBuffer(end - begin);
            for (int i = begin; i < end; i++) {
                    char c = str[i];
                    if (c == '\\') {
                            i++;
                            if (i < end) {
                                    c = str[i];
                                    switch (c) {
                                            case '"' :
                                            case '\\' :
                                                    break;
                                            case 'r' :
                                                    c = '\r';
                                                    break;
                                            case 'n' :
                                                    c = '\n';
                                                    break;
                                            default :
                                                    c = '\\';
                                                    i--;
                                                    break;
                                    }
                            }
                    }
                    output.append(c);
            }

            return output.toString();
    }

    public ConditionalPermissionInfoImpl(String encoded)
    {
        encoded = encoded.trim();
        String toUpper = encoded.toUpperCase();
        if (!(toUpper.startsWith("ALLOW {") || toUpper.startsWith("DENY {")))
        {
            throw new IllegalArgumentException();
        }
        m_allow = toUpper.startsWith("ALLOW {");
        m_cpai = null;
        List conditions = new ArrayList();
        List permissions = new ArrayList();
        try {
        char[] chars = encoded.substring((m_allow ? "ALLOW {".length() : "DENY {".length())).toCharArray();
        int idx = 0;
        while (idx <  chars.length)
        {
            if (Character.isWhitespace(chars[idx])) {
                idx++;
            }
            else if (chars[idx] == '[')
            {
                idx = parseConditionInfo(chars, idx, conditions);
            }
            else if (chars[idx] == '(')
            {
                idx = parsePermissionInfo(chars, idx, permissions);
            }
            else
            {
                if (chars[idx] != '}')
                {
                    throw new IllegalArgumentException("Expected } but was: " + chars[idx]);
                }
                idx++;
                break;
            }
        }
        while (Character.isWhitespace(chars[idx])) {
            idx++;
        }
        if (chars[idx] == '"') {
            idx++;
            int begin = idx;
            while (chars[idx] != '"') {
                    if (chars[idx] == '\\') {
                            idx++;
                    }
                    idx++;
            }
            m_name = unescapeString(chars, begin, idx);
        }
        else {
            m_name = Long.toString(RANDOM.nextLong() ^ System.currentTimeMillis());
        }
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException("Unable to parse conditional permission info: " + ex.getMessage());
        }
        m_conditions = conditions.isEmpty() ? CONDITION_INFO
            : (ConditionInfo[]) conditions.toArray(new ConditionInfo[conditions
                .size()]);
        m_permissions = permissions.isEmpty() ? PERMISSION_INFO
            : (PermissionInfo[]) permissions
                .toArray(new PermissionInfo[permissions.size()]);
    }

    public ConditionalPermissionInfoImpl(ConditionalPermissionAdminImpl cpai,
        String name, boolean access)
    {
        m_allow = access;
        m_name = name;
        m_cpai = cpai;
        m_conditions = CONDITION_INFO;
        m_permissions = PERMISSION_INFO;
    }

    public ConditionalPermissionInfoImpl(ConditionInfo[] conditions,
        PermissionInfo[] permisions, ConditionalPermissionAdminImpl cpai,
        boolean access)
    {
        m_allow = access;
        m_name = Long.toString(RANDOM.nextLong() ^ System.currentTimeMillis());
        m_cpai = cpai;
        m_conditions = conditions == null ? CONDITION_INFO : conditions;
        m_permissions = permisions == null ? PERMISSION_INFO : permisions;
    }

    public ConditionalPermissionInfoImpl(String name,
        ConditionInfo[] conditions, PermissionInfo[] permisions,
        ConditionalPermissionAdminImpl cpai, boolean access)
    {
        m_allow = access;
        m_name = (name != null) ? name : Long.toString(RANDOM.nextLong()
            ^ System.currentTimeMillis());
        m_conditions = conditions == null ? CONDITION_INFO : conditions;
        m_permissions = permisions == null ? PERMISSION_INFO : permisions;
        m_cpai = cpai;
    }

    public void delete()
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }

        synchronized (m_lock)
        {
            m_cpai.write(m_name, null);
            m_conditions = CONDITION_INFO;
            m_permissions = PERMISSION_INFO;
        }
    }

    public ConditionInfo[] getConditionInfos()
    {
        synchronized (m_lock)
        {
            return (ConditionInfo[]) m_conditions.clone();
        }
    }

    ConditionInfo[] _getConditionInfos()
    {
        synchronized (m_lock)
        {
            return m_conditions;
        }
    }

    void setConditionsAndPermissions(ConditionInfo[] conditions,
        PermissionInfo[] permissions)
    {
        synchronized (m_lock)
        {
            m_conditions = conditions;
            m_permissions = permissions;
        }
    }

    public String getName()
    {
        return m_name;
    }

    public PermissionInfo[] getPermissionInfos()
    {
        synchronized (m_lock)
        {
            return (PermissionInfo[]) m_permissions.clone();
        }
    }

    PermissionInfo[] _getPermissionInfos()
    {
        synchronized (m_lock)
        {
            return m_permissions;
        }
    }

    public String getEncoded()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(m_allow ? "ALLOW " : "DENY ");
        buffer.append('{');
        buffer.append(' ');
        synchronized (m_lock)
        {
            writeTo(m_conditions, buffer);
            writeTo(m_permissions, buffer);
        }
        buffer.append('}');
        buffer.append(' ');
        buffer.append('"');
        escapeString(m_name, buffer);
        buffer.append('"');
        return buffer.toString();
    }
    
    /**
     * This escapes the quotes, backslashes, \n, and \r in the string using a
     * backslash and appends the newly escaped string to a StringBuffer.
     */
    private static void escapeString(String str, StringBuffer output) {
            int len = str.length();
            for (int i = 0; i < len; i++) {
                    char c = str.charAt(i);
                    switch (c) {
                            case '"' :
                            case '\\' :
                                    output.append('\\');
                                    output.append(c);
                                    break;
                            case '\r' :
                                    output.append("\\r");
                                    break;
                            case '\n' :
                                    output.append("\\n");
                                    break;
                            default :
                                    output.append(c);
                                    break;
                    }
            }
    }

    private void writeTo(Object[] elements, StringBuffer buffer)
    {
        for (int i = 0; i < elements.length; i++)
        {
            buffer.append(elements[i]);
            buffer.append(' ');
        }
    }

    public String toString()
    {
        return getEncoded();
    }

    public String getAccessDecision()
    {
        return m_allow ? ConditionalPermissionInfo.ALLOW
            : ConditionalPermissionInfo.DENY;
    }

    public boolean isAllow()
    {
        return m_allow;
    }
}
