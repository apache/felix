/*
 * Copyright (c) OSGi Alliance (2004, 2010). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.service.coordinator;

import java.security.BasicPermission;

/**
 * The name parameter of the Permission is a filter expression. It asserts the
 * bundle that is associated with the coordination. Additionally, the following
 * attributes can be asserted:
 * <ol>
 * <li>coordination.name - The name of the coordination
 * <table>
 * <tr>
 * <td>Coordinator</td>
 * <td>INITIATE</td>
 * <td>PARTICIPATE</td>
 * <td>ADMIN</td>
 * <td>NONE</td>
 * </tr>
 * <tr>
 * <td>alwaysFail(String)</td>
 * <td></td>
 * <td></td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>begin(String) + -</td>
 * </tr>
 * <tr>
 * <td>getCoordinations() + -</td>
 * </tr>
 * <tr>
 * <td>isActive() + + + -</td>
 * </tr>
 * <tr>
 * <td>isFailed() + + + -</td>
 * </tr>
 * <tr>
 * <td>participate(Participant) + -</td>
 * </tr>
 * <tr>
 * <td>participateOrBegin(Participant) + and + -</td>
 * </tr>
 * <tr>
 * <td>Coordination</td>
 * </tr>
 * <tr>
 * <td>end() +</td>
 * </tr>
 * <tr>
 * <td>fail(String) +</td>
 * </tr>
 * <tr>
 * <td>getName() +</td>
 * </tr>
 * <tr>
 * <td>getParticipants() + -</td>
 * </tr>
 * <tr>
 * <td>isFailed() +</td>
 * </tr>
 * <tr>
 * <td>setTimeout(long) + + -</td>
 * </tr>
 * <tr>
 * <td>terminate() +</td>
 * </tr>
 * </table>
 * </li>
 * </ol>
 *
 * @Provisional
 */
@Deprecated
public class CoordinationPermission extends BasicPermission
{

    private static final long serialVersionUID = 1566605398519619478L;

    /**
     * Initiate a Coordination. An owner of this permission can initiate, end,
     * fail, and terminate a Coordination.
     */
    public static final String INITIATE = "initiate";

    /**
     * The action string admin.
     */
    public static final String ADMIN = "admin";

    /**
     * The action string participate.
     */
    public static final String PARTICIPATE = "participate";

    /**
     * The name parameter specifies a filter condition. The filter asserts the
     * bundle that initiated the Coordination. An implicit grant is made for a
     * bundle's own coordinations.
     *
     * @param filterExpression A filter expression asserting the bundle
     *            associated with the coordination.
     * @param actions A comma separated combination of {@link #INITIATE},
     *            {@link #ADMIN}, {@link #PARTICIPATE}.
     */
    public CoordinationPermission(String filterExpression, String actions)
    {
        super(filterExpression, actions);
    }

    /**
     * The verification permission
     *
     * @param bundle The bundle that will be the target of the filter
     *            expression.
     * @param coordinationName The name of the coordination or <code>null</code>
     * @param actions The set of actions required, which is a combination of
     *            {@link #INITIATE}, {@link #ADMIN}, {@link #PARTICIPATE}.
     */
    public CoordinationPermission(org.osgi.framework.Bundle bundle, String coordinationName, String actions)
    {
        super(coordinationName, actions);
    }
}
