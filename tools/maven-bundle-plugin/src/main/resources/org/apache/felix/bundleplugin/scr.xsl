<?xml version="1.0" encoding="UTF-8"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:scr10="http://www.osgi.org/xmlns/scr/v1.0.0"
                xmlns:scr11="http://www.osgi.org/xmlns/scr/v1.1.0"
                xmlns:scr12="http://www.osgi.org/xmlns/scr/v1.2.0">
    <xsl:output method="text" />

    <xsl:template match="/">

        <xsl:for-each select="//scr10:component[service/provide/@interface] |
                              //scr11:component[service/provide/@interface] |
                              //scr12:component[service/provide/@interface]">
            <xsl:value-of select="'Provide-Capability: osgi.service;effective:=active;'" />
            <xsl:choose>
                <xsl:when test="count(service/provide/@interface) = 1">
                    <xsl:value-of select="'objectClass=&quot;'" />
                    <xsl:value-of select="service/provide/@interface"/>
                    <xsl:value-of select="'&quot;'" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="'objectClass:List&lt;String&gt;=&quot;'" />
                    <xsl:for-each select="service/provide/@interface">
                        <xsl:value-of select="."/>
                        <xsl:if test="position() != last()">
                            <xsl:value-of select="','" />
                        </xsl:if>
                    </xsl:for-each>
                    <xsl:value-of select="'&quot;'" />
                </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="property[@name != 'service.pid' and @value and not(contains(@value, '$'))]">
                <xsl:value-of select="';'" />
                <xsl:value-of select="@name"/>
                <xsl:value-of select="'=&quot;'" />
                <xsl:value-of select="@value"/>
                <xsl:value-of select="'&quot;'" />
            </xsl:for-each>
            <xsl:text>
            </xsl:text>
        </xsl:for-each>

        <xsl:for-each select="//scr10:component/reference |
                              //scr11:component/reference |
                              //scr12:component/reference">
            <xsl:value-of select="'Require-Capability: osgi.service;effective:=active;'" />
            <xsl:choose>
                <xsl:when test="@cardinality = '0..1'">
                    <xsl:value-of select="'resolution:=optional;'" />
                </xsl:when>
                <xsl:when test="@cardinality = '0..n'">
                    <xsl:value-of select="'resolution:=optional;cardinality:=multiple;'" />
                </xsl:when>
                <xsl:when test="@cardinality = '1..n'">
                    <xsl:value-of select="'cardinality:=multiple;'" />
                </xsl:when>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="@target">
                    <xsl:choose>
                        <xsl:when test="starts-with(@target, '(')">
                            <xsl:value-of select="concat('filter:=&quot;(&amp;(objectClass=', @interface, ')', @target, ')&quot;')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="concat('filter:=&quot;(&amp;(objectClass=', @interface, ')(', @target, '))&quot;')"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat('filter:=&quot;(objectClass=', @interface, ')&quot;')"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>
            </xsl:text>
        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>

