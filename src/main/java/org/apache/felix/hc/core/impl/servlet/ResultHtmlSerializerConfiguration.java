/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.core.impl.servlet;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Apache Felix Health Check Result HTML Serializer", description = "Serializer for health check results in HTML format")
@interface ResultHtmlSerializerConfiguration {

    String CSS_STYLE_DEFAULT = "body { font-size:12px; font-family:arial,verdana,sans-serif;background-color:#FFFDF1; }\n"
            + "h1 { font-size:20px;}\n"
            + "table { font-size:12px; border:#ccc 1px solid; border-radius:3px; }\n"
            + "table th { padding:5px; text-align: left; background: #ededed; }\n"
            + "table td { padding:5px; border-top: 1px solid #ffffff; border-bottom:1px solid #e0e0e0; border-left: 1px solid #e0e0e0; }\n"
            + ".statusOK { background-color:#CCFFCC;}\n"
            + ".statusWARN { background-color:#FFE569;}\n"
            + ".statusTEMPORARILY_UNAVAILABLE { background-color:#dab6fc;}\n"
            + ".statusCRITICAL { background-color:#F0975A;}\n"
            + ".statusHEALTH_CHECK_ERROR { background-color:#F16D4E;}\n"
            + ".helpText { color:grey; font-size:80%; }\n";

    @AttributeDefinition(name = "CSS Style", description = "CSS Style - can be configured to change the look and feel of the html result page.")
    String styleString() default CSS_STYLE_DEFAULT;

}
