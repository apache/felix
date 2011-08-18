/**
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2011 Bull S.A.S.
 * Contact: jonas-team@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 * --------------------------------------------------------------------------
 * $Id: $
 * --------------------------------------------------------------------------
 */


package org.apache.felix.ipojo.manipulator.render;

import org.apache.felix.ipojo.manipulation.MethodCreator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code ManipulatedMetadataFilter} is ...
 *
 * @author Guillaume Sauthier
 */
public class ManipulatedMetadataFilter implements MetadataFilter {

    public boolean accept(Element element) {

        // TODO I'm sure we can do better then testing blindly all attributes
        // iPOJO manipulated elements filter
        for (Attribute attribute : element.getAttributes()) {
            String value = attribute.getValue();

            // Filters:
            // * manipulated methods
            // * fields for the InstanceManager
            // * InstanceManager setter
            if (value.startsWith(MethodCreator.PREFIX)
                    || value.contains("org.apache.felix.ipojo.InstanceManager")
                    || value.contains("_setInstanceManager")) {
                return true;
            }
        }

        return false;
    }
}
