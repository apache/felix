/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.installer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.RequirementBuilder;

class RequirementParser {

    static List<Requirement> parseRequireBundle(String header) throws IllegalArgumentException {
        if (header == null) {
            return Collections.emptyList();
        }

        Clause[] clauses = Parser.parseHeader(header);
        List<Requirement> requirements = new ArrayList<>(clauses.length);
        for (Clause requireClause : clauses) {
            String bsn = requireClause.getName();
            String versionRangeStr = requireClause.getAttribute(org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE);

            String filter = toBundleFilter(bsn, versionRangeStr);
            Requirement requirement = new RequirementBuilder(BundleNamespace.BUNDLE_NAMESPACE)
                    .addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter).buildSyntheticRequirement();
            requirements.add(requirement);
        }
        return requirements;
    }

    static List<Requirement> parseRequireCapability(String header) throws IllegalArgumentException {
        if (header == null) {
            return Collections.emptyList();
        }

        Clause[] clauses = Parser.parseHeader(header);
        List<Requirement> reqs = new ArrayList<>(clauses.length);
        for (Clause clause : clauses) {
            String namespace = clause.getName();

            RequirementBuilder reqBuilder = new RequirementBuilder(namespace);
            for (Attribute attrib : clause.getAttributes()) {
                try {
                    reqBuilder.addAttribute(attrib.getName(), attrib.getValue());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid requirement attribute", e);
                }
            }
            for (Directive directive : clause.getDirectives()) {
                reqBuilder.addDirective(directive.getName(), directive.getValue());
            }

            reqs.add(reqBuilder.buildSyntheticRequirement());
        }

        return reqs;
    }

    private static String toBundleFilter(String bsn, String versionRangeStr) {
        final String filterStr;

        String bsnFilter = String.format("(%s=%s)", BundleNamespace.BUNDLE_NAMESPACE, bsn);

        if (versionRangeStr == null) {
            filterStr = bsnFilter;
        } else {
            VersionRange versionRange = new VersionRange(versionRangeStr);
            if (versionRange.isExact()) {
                String exactVersionFilter = String.format("(%s=%s)",
                        BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, versionRange.getLeft());
                filterStr = String.format("(&%s%s)", bsnFilter, exactVersionFilter);
            } else if (versionRange.getRight() == null) {
                filterStr = String.format("(&%s%s)", bsnFilter, lowerVersionFilter(versionRange));
            } else {
                filterStr = String.format("(&%s%s%s)", bsnFilter, lowerVersionFilter(versionRange),
                        upperVersionFilter(versionRange));
            }
        }
        return filterStr;
    }

    private static String upperVersionFilter(VersionRange versionRange) {
        String upperVersionFilter;
        if (versionRange.getRightType() == VersionRange.RIGHT_CLOSED) {
            upperVersionFilter = String.format("(%s<=%s)", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                    versionRange.getRight());
        } else {
            upperVersionFilter = String.format("(!(%s>=%s))", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                    versionRange.getRight());
        }
        return upperVersionFilter;
    }

    private static String lowerVersionFilter(VersionRange versionRange) {
        String lowerVersionFilter;
        if (versionRange.getLeftType() == VersionRange.LEFT_CLOSED) {
            lowerVersionFilter = String.format("(%s>=%s)", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                    versionRange.getLeft());
        } else {
            lowerVersionFilter = String.format("(!(%s<=%s))", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                    versionRange.getLeft());
        }
        return lowerVersionFilter;
    }

}
