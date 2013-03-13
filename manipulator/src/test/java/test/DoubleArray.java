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

package test;

/**
 * Checks a component with a double array
 * To reproduce https://issues.apache.org/jira/browse/FELIX-3621
 */
public class DoubleArray {

    public boolean start() {
        System.out.println("Start");

        int[][] matrix = new int[20][20];

        this.testArray(matrix);

        matrix[0][0] = 2;
        return true;
    }

    private void testArray(int[][] matrix) {
        System.out.println("Test Array");
    }


}
