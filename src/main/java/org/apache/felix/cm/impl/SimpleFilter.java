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
package org.apache.felix.cm.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.InvalidSyntaxException;

public class SimpleFilter
{
    public static final int MATCH_ALL = 0;
    public static final int AND = 1;
    public static final int OR = 2;
    public static final int NOT = 3;
    public static final int EQ = 4;
    public static final int LTE = 5;
    public static final int GTE = 6;
    public static final int SUBSTRING = 7;
    public static final int PRESENT = 8;
    public static final int APPROX = 9;

    private final String m_name;
    private final Object m_value;
    private final int m_op;

    public SimpleFilter(String attr, Object value, int op)
    {
        m_name = attr;
        m_value = value;
        m_op = op;
    }

    public String getName()
    {
        return m_name;
    }

    public Object getValue()
    {
        return m_value;
    }

    public int getOperation()
    {
        return m_op;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    private void toString(StringBuilder sb)
    {
        switch (m_op)
        {
            case AND:
                sb.append("(&");
                toString(sb, (List) m_value);
                sb.append(")");
                break;
            case OR:
                sb.append("(|");
                toString(sb, (List) m_value);
                sb.append(")");
                break;
            case NOT:
                sb.append("(!");
                toString(sb, (List) m_value);
                sb.append(")");
                break;
            case EQ:
                sb.append("(")
                        .append(m_name)
                        .append("=");
                toEncodedString(sb, m_value);
                sb.append(")");
                break;
            case LTE:
                sb.append("(")
                        .append(m_name)
                        .append("<=");
                toEncodedString(sb, m_value);
                sb.append(")");
                break;
            case GTE:
                sb.append("(")
                        .append(m_name)
                        .append(">=");
                toEncodedString(sb, m_value);
                sb.append(")");
                break;
            case SUBSTRING:
                sb.append("(").append(m_name).append("=");
                unparseSubstring(sb, (List) m_value);
                sb.append(")");
                break;
            case PRESENT:
                sb.append("(").append(m_name).append("=*)");
                break;
            case APPROX:
                sb.append("(").append(m_name).append("~=");
                toEncodedString(sb, m_value);
                sb.append(")");
                break;
            case MATCH_ALL:
                sb.append("(*)");
                break;
        }
    }

    private static void toString(StringBuilder sb, List list)
    {
        for (Object o : list)
        {
            SimpleFilter sf = (SimpleFilter) o;
            sf.toString(sb);
        }
    }

    private static String toDecodedString(String s, int startIdx, int endIdx)
    {
        StringBuilder sb = new StringBuilder(endIdx - startIdx);
        boolean escaped = false;
        for (int i = 0; i < (endIdx - startIdx); i++)
        {
            char c = s.charAt(startIdx + i);
            if (!escaped && (c == '\\'))
            {
                escaped = true;
            }
            else
            {
                escaped = false;
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static void toEncodedString(StringBuilder sb, Object o)
    {
        if (o instanceof String)
        {
            String s = (String) o;
            for (int i = 0; i < s.length(); i++)
            {
                char c = s.charAt(i);
                if ((c == '\\') || (c == '(') || (c == ')') || (c == '*'))
                {
                    sb.append('\\');
                }
                sb.append(c);
            }
        }
        else
        {
            sb.append(o);
        }
    }

    public static SimpleFilter parse(final String filter) throws InvalidSyntaxException
    {
        int idx = skipWhitespace(filter, 0);

        if ((filter == null) || (filter.length() == 0) || (idx >= filter.length()))
        {
            throw new InvalidSyntaxException("Null or empty filter.", filter);
        }
        else if (filter.charAt(idx) != '(')
        {
            throw new InvalidSyntaxException("Missing opening parenthesis", filter);
        }

        SimpleFilter sf = null;
        List<Object> stack = new ArrayList<Object>();
        boolean isEscaped = false;
        while (idx < filter.length())
        {
            if (sf != null)
            {
                throw new InvalidSyntaxException(
                        "Only one top-level operation allowed", filter);
            }

            if (!isEscaped && (filter.charAt(idx) == '('))
            {
                // Skip paren and following whitespace.
                idx = skipWhitespace(filter, idx + 1);

                if (filter.charAt(idx) == '&')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.AND));
                    }
                    else
                    {
                        stack.add(0, idx);
                    }
                }
                else if (filter.charAt(idx) == '|')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.OR));
                    }
                    else
                    {
                        stack.add(0, idx);
                    }
                }
                else if (filter.charAt(idx) == '!')
                {
                    int peek = skipWhitespace(filter, idx + 1);
                    if (filter.charAt(peek) == '(')
                    {
                        idx = peek - 1;
                        stack.add(0, new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT));
                    }
                    else
                    {
                        stack.add(0, idx);
                    }
                }
                else
                {
                    stack.add(0, idx);
                }
            }
            else if (!isEscaped && (filter.charAt(idx) == ')'))
            {
                Object top = stack.remove(0);
                if (top instanceof SimpleFilter)
                {
                    if (!stack.isEmpty() && (stack.get(0) instanceof SimpleFilter))
                    {
                        ((List) ((SimpleFilter) stack.get(0)).m_value).add(top);
                    }
                    else
                    {
                        sf = (SimpleFilter) top;
                    }
                }
                else if (!stack.isEmpty() && (stack.get(0) instanceof SimpleFilter))
                {
                    ((List) ((SimpleFilter) stack.get(0)).m_value).add(
                            SimpleFilter.subfilter(filter, (Integer) top, idx));
                }
                else
                {
                    sf = SimpleFilter.subfilter(filter, (Integer) top, idx);
                }
            }
            else if (!isEscaped && (filter.charAt(idx) == '\\'))
            {
                isEscaped = true;
            }
            else
            {
                isEscaped = false;
            }

            idx = skipWhitespace(filter, idx + 1);
        }

        if (sf == null)
        {
            throw new InvalidSyntaxException("Missing closing parenthesis", filter);
        }

        return sf;
    }

    private static SimpleFilter subfilter(String filter, int startIdx, int endIdx) throws InvalidSyntaxException
    {
        final String opChars = "=<>~";

        // Determine the ending index of the attribute name.
        int attrEndIdx = startIdx;
        for (int i = 0; i < (endIdx - startIdx); i++)
        {
            char c = filter.charAt(startIdx + i);
            if (opChars.indexOf(c) >= 0)
            {
                break;
            }
            else if (!Character.isWhitespace(c))
            {
                attrEndIdx = startIdx + i + 1;
            }
        }
        if (attrEndIdx == startIdx)
        {
            throw new InvalidSyntaxException(
                    "Missing attribute name: " + filter.substring(startIdx, endIdx), filter);
        }
        String attr = filter.substring(startIdx, attrEndIdx);

        // Skip the attribute name and any following whitespace.
        startIdx = skipWhitespace(filter, attrEndIdx);

        // Determine the operator type.
        int op;
        switch (filter.charAt(startIdx))
        {
            case '=':
                op = EQ;
                startIdx++;
                break;
            case '<':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new InvalidSyntaxException(
                            "Unknown operator: " + filter.substring(startIdx, endIdx), filter);
                }
                op = LTE;
                startIdx += 2;
                break;
            case '>':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new InvalidSyntaxException(
                            "Unknown operator: " + filter.substring(startIdx, endIdx), filter);
                }
                op = GTE;
                startIdx += 2;
                break;
            case '~':
                if (filter.charAt(startIdx + 1) != '=')
                {
                    throw new InvalidSyntaxException(
                            "Unknown operator: " + filter.substring(startIdx, endIdx), filter);
                }
                op = APPROX;
                startIdx += 2;
                break;
            default:
                throw new InvalidSyntaxException(
                        "Unknown operator: " + filter.substring(startIdx, endIdx), filter);
        }

        // Parse value.
        Object value = toDecodedString(filter, startIdx, endIdx);

        // Check if the equality comparison is actually a substring
        // or present operation.
        if (op == EQ)
        {
            String valueStr = filter.substring(startIdx, endIdx);
            List<String> values = parseSubstring(valueStr);
            if ((values.size() == 2)
                    && (values.get(0).length() == 0)
                    && (values.get(1).length() == 0))
            {
                op = PRESENT;
            }
            else if (values.size() > 1)
            {
                op = SUBSTRING;
                value = values;
            }
        }

        return new SimpleFilter(attr, value, op);
    }

    public static List<String> parseSubstring(String value)
    {
        List<String> pieces = new ArrayList<String>();
        StringBuilder ss = new StringBuilder();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
        boolean escaped = false;
        loop:   for (;;)
        {
            if (idx >= value.length())
            {
                if (wasStar)
                {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                }
                else
                {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break loop;
            }

            // Read the next character and account for escapes.
            char c = value.charAt(idx++);
            if (!escaped && (c == '*'))
            {
                // If we have successive '*' characters, then we can
                // effectively collapse them by ignoring succeeding ones.
                if (!wasStar)
                {
                    if (ss.length() > 0)
                    {
                        pieces.add(ss.toString()); // accumulate the pieces
                        // between '*' occurrences
                    }
                    ss.setLength(0);
                    // if this is a leading star, then track it
                    if (pieces.isEmpty())
                    {
                        leftstar = true;
                    }
                    wasStar = true;
                }
            }
            else if (!escaped && (c == '\\'))
            {
                escaped = true;
            }
            else
            {
                escaped = false;
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1)
        {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar)
            {
                pieces.add("");
            }
            if (leftstar)
            {
                pieces.add(0, "");
            }
        }
        return pieces;
    }

    public static void unparseSubstring(StringBuilder sb, List<String> pieces)
    {
        for (int i = 0; i < pieces.size(); i++)
        {
            if (i > 0)
            {
                sb.append("*");
            }
            toEncodedString(sb, pieces.get(i));
        }
    }

    public static boolean compareSubstring(List<String> pieces, String s)
    {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = true;
        int len = pieces.size();

        // Special case, if there is only one piece, then
        // we must perform an equality test.
        if (len == 1)
        {
            return s.equals(pieces.get(0));
        }

        // Otherwise, check whether the pieces match
        // the specified string.

        int index = 0;

        loop:   for (int i = 0; i < len; i++)
        {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break loop;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == (len - 1))
            {
                if (s.endsWith(piece) && (s.length() >= (index + piece.length())))
                {
                    result = true;
                }
                else
                {
                    result = false;
                }
                break loop;
            }

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1)))
            {
                index = s.indexOf(piece, index);
                if (index < 0)
                {
                    result = false;
                    break loop;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }

    private static int skipWhitespace(String s, int startIdx)
    {
        int len = s.length();
        while ((startIdx < len) && Character.isWhitespace(s.charAt(startIdx)))
        {
            startIdx++;
        }
        return startIdx;
    }

    public boolean matches(Dictionary dict)
    {
        boolean matched = true;

        if (getOperation() == SimpleFilter.MATCH_ALL)
        {
            matched = true;
        }
        else if (getOperation() == SimpleFilter.AND)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For AND we calculate the intersection of each subfilter.
            // We can short-circuit the AND operation if there are no
            // remaining capabilities.
            List<SimpleFilter> sfs = (List<SimpleFilter>) getValue();
            for (int i = 0; matched && (i < sfs.size()); i++)
            {
                matched = sfs.get(i).matches(dict);
            }
        }
        else if (getOperation() == SimpleFilter.OR)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            matched = false;
            List<SimpleFilter> sfs = (List<SimpleFilter>) getValue();
            for (int i = 0; !matched && (i < sfs.size()); i++)
            {
                matched = sfs.get(i).matches(dict);
            }
        }
        else if (getOperation() == SimpleFilter.NOT)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            List<SimpleFilter> sfs = (List<SimpleFilter>) getValue();
            for (int i = 0; i < sfs.size(); i++)
            {
                matched = !sfs.get(i).matches(dict);
            }
        }
        else
        {
            matched = false;
            Object lhs = dict.get( getName() );
            if (lhs != null)
            {
                matched = compare(lhs, getValue(), getOperation());
            }
        }

        return matched;
    }

    private static final Class<?>[] STRING_CLASS = new Class[] { String.class };

    private static boolean compare(Object lhs, Object rhsUnknown, int op)
    {
        if (lhs == null)
        {
            return false;
        }

        // If this is a PRESENT operation, then just return true immediately
        // since we wouldn't be here if the attribute wasn't present.
        if (op == SimpleFilter.PRESENT)
        {
            return true;
        }

        // If the type is comparable, then we can just return the
        // result immediately.
        if (lhs instanceof Comparable)
        {
            // Spec says SUBSTRING is false for all types other than string.
            if ((op == SimpleFilter.SUBSTRING) && !(lhs instanceof String))
            {
                return false;
            }

            Object rhs;
            if (op == SimpleFilter.SUBSTRING)
            {
                rhs = rhsUnknown;
            }
            else
            {
                try
                {
                    rhs = coerceType(lhs, (String) rhsUnknown);
                }
                catch (Exception ex)
                {
                    return false;
                }
            }

            switch (op)
            {
            case SimpleFilter.EQ :
                try
                {
                    return (((Comparable) lhs).compareTo(rhs) == 0);
                }
                catch (Exception ex)
                {
                    return false;
                }
            case SimpleFilter.GTE :
                try
                {
                    return (((Comparable) lhs).compareTo(rhs) >= 0);
                }
                catch (Exception ex)
                {
                    return false;
                }
            case SimpleFilter.LTE :
                try
                {
                    return (((Comparable) lhs).compareTo(rhs) <= 0);
                }
                catch (Exception ex)
                {
                    return false;
                }
            case SimpleFilter.APPROX :
                return compareApproximate((lhs), rhs);
            case SimpleFilter.SUBSTRING :
                return SimpleFilter.compareSubstring((List<String>) rhs, (String) lhs);
            default:
                throw new RuntimeException(
                        "Unknown comparison operator: " + op);
            }
        }
        // Booleans do not implement comparable, so special case them.
        else if (lhs instanceof Boolean)
        {
            Object rhs;
            try
            {
                rhs = coerceType(lhs, (String) rhsUnknown);
            }
            catch (Exception ex)
            {
                return false;
            }

            switch (op)
            {
            case SimpleFilter.EQ :
            case SimpleFilter.GTE :
            case SimpleFilter.LTE :
            case SimpleFilter.APPROX :
                return (lhs.equals(rhs));
            default:
                throw new RuntimeException(
                        "Unknown comparison operator: " + op);
            }
        }

        // If the LHS is not a comparable or boolean, check if it is an
        // array. If so, convert it to a list so we can treat it as a
        // collection.
        if (lhs.getClass().isArray())
        {
            lhs = convertArrayToList(lhs);
        }

        // If LHS is a collection, then call compare() on each element
        // of the collection until a match is found.
        if (lhs instanceof Collection)
        {
            for (Iterator iter = ((Collection) lhs).iterator(); iter.hasNext(); )
            {
                if (compare(iter.next(), rhsUnknown, op))
                {
                    return true;
                }
            }

            return false;
        }

        // Spec says SUBSTRING is false for all types other than string.
        if ((op == SimpleFilter.SUBSTRING) && !(lhs instanceof String))
        {
            return false;
        }

        // Since we cannot identify the LHS type, then we can only perform
        // equality comparison.
        try
        {
            return lhs.equals(coerceType(lhs, (String) rhsUnknown));
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private static boolean compareApproximate(Object lhs, Object rhs)
    {
        if (rhs instanceof String)
        {
            return removeWhitespace((String) lhs)
                    .equalsIgnoreCase(removeWhitespace((String) rhs));
        }
        else if (rhs instanceof Character)
        {
            return Character.toLowerCase(((Character) lhs))
                    == Character.toLowerCase(((Character) rhs));
        }
        return lhs.equals(rhs);
    }

    private static String removeWhitespace(String s)
    {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            if (!Character.isWhitespace(s.charAt(i)))
            {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static Object coerceType(Object lhs, String rhsString) throws Exception
    {
        // If the LHS expects a string, then we can just return
        // the RHS since it is a string.
        if (lhs.getClass() == rhsString.getClass())
        {
            return rhsString;
        }

        // Try to convert the RHS type to the LHS type by using
        // the string constructor of the LHS class, if it has one.
        Object rhs = null;
        try
        {
            // The Character class is a special case, since its constructor
            // does not take a string, so handle it separately.
            if (lhs instanceof Character)
            {
                rhs = rhsString.charAt(0);
            }
            else
            {
                // Spec says we should trim number types.
                if ((lhs instanceof Number) || (lhs instanceof Boolean))
                {
                    rhsString = rhsString.trim();
                }
                Constructor ctor = lhs.getClass().getConstructor(STRING_CLASS);
                ctor.setAccessible(true);
                rhs = ctor.newInstance(rhsString);
            }
        }
        catch (Exception ex)
        {
            throw new Exception(
                    "Could not instantiate class "
                            + lhs.getClass().getName()
                            + " from string constructor with argument '"
                            + rhsString + "' because " + ex);
        }

        return rhs;
    }

    /**
     * This is an ugly utility method to convert an array of primitives
     * to an array of primitive wrapper objects. This method simplifies
     * processing LDAP filters since the special case of primitive arrays
     * can be ignored.
     * @param array An array of primitive types.
     * @return An corresponding array using pritive wrapper objects.
     **/
    private static List convertArrayToList(Object array)
    {
        int len = Array.getLength(array);
        List<Object> list = new ArrayList<Object>(len);
        for (int i = 0; i < len; i++)
        {
            list.add(Array.get(array, i));
        }
        return list;
    }
}
