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


import java.util.Comparator;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;


/**
 * The <code>RankingComparator</code> may be used to maintain sorted
 * sets or to sort arrays such that the first element in the set or
 * array is the one to use first and the last elements the one to
 * use last.
 */
public abstract class RankingComparator implements Comparator<ServiceReference<?>>
{

    /**
     * Implements a comparator to sort arrays and sets according to the
     * specification of the <code>service.ranking</code> property. This
     * results in collections whose first element has the highest ranking
     * and the last element has the lowest ranking. Thus the results of
     * this comparator are as follows:
     * <ul>
     * <li><code>&lt; 0</code> if obj1 has higher ranking than obj2</li>
     * <li><code>== 0</code> if obj1 and obj2 reference the same service</li>
     * <li><code>&gt; 0</code> if obj1 has lower ranking than obj2</li>
     * </ul>
     */
    public static Comparator<ServiceReference<?>> SRV_RANKING = new RankingComparator()
    {
        public int compare( ServiceReference<?> obj1, ServiceReference<?> obj2 )
        {
            final long id1 = this.getLong( obj1, Constants.SERVICE_ID );
            final long id2 = this.getLong( obj2, Constants.SERVICE_ID );

            if ( id1 == id2 )
            {
                return 0;
            }

            final int rank1 = this.getInteger( obj1, Constants.SERVICE_RANKING );
            final int rank2 = this.getInteger( obj2, Constants.SERVICE_RANKING );

            if ( rank1 == rank2 )
            {
                return ( id1 < id2 ) ? -1 : 1;
            }

            return ( rank1 > rank2 ) ? -1 : 1;
        }

    };


    /**
     * Implements a comparator to sort arrays and sets according to the
     * specification of the <code>service.cmRanking</code> property in
     * the Configuration Admin specification. This results in collections
     * where the first element has the lowest ranking value and the last
     * element has the highest ranking value. Order amongst elements with
     * the same ranking value is left undefined, however we order it
     * by service id, lowest last. Thus the results of this
     * comparator are as follows:
     * <ul>
     * <li><code>&lt; 0</code> if obj1 has lower ranking than obj2</li>
     * <li><code>== 0</code> if obj1 and obj2 have the same ranking</li>
     * <li><code>&gt; 0</code> if obj1 has higher ranking than obj2</li>
     * </ul>
     */
    public static Comparator<ServiceReference<?>> CM_RANKING = new RankingComparator()
    {
        public int compare( ServiceReference<?> obj1, ServiceReference<?> obj2 )
        {
            final long id1 = this.getLong( obj1, Constants.SERVICE_ID );
            final long id2 = this.getLong( obj2, Constants.SERVICE_ID );

            final int rank1 = this.getInteger( obj1, ConfigurationPlugin.CM_RANKING );
            final int rank2 = this.getInteger( obj2, ConfigurationPlugin.CM_RANKING );

            if ( rank1 == rank2 )
            {
                if ( id1 == id2 )
                {
                    return 0;
                }

                return ( id1 > id2 ) ? -1 : 1;
            }

            return ( rank1 < rank2 ) ? -1 : 1;
        }

    };


    protected int getInteger( ServiceReference<?> sr, String property )
    {
        Object rankObj = sr.getProperty( property );
        if ( rankObj instanceof Integer )
        {
            return ( ( Integer ) rankObj ).intValue();
        }

        // null or not an integer
        return 0;
    }

    protected long getLong( ServiceReference<?> sr, String property )
    {
        Object rankObj = sr.getProperty( property );
        if ( rankObj instanceof Long )
        {
            return ( ( Long ) rankObj ).longValue();
        }

        // null or not a long
        return 0;
    }
}
