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
package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test unacceptable configuration.
 */
public class TestUnacceptableConfiguration extends Common {

    /**
     * Configuration without the name property.
     */
    @Test
    public void testWithoutName() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");

        Properties p = new Properties();
        p.put("int", 3);
        p.put("long", (long) 42);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("an acceptable configuration is refused : " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Configuration without the name property.
     */
    @Test
    public void testWithoutNameOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2opt");

        Properties p = new Properties();
        p.put("int", 3);
        p.put("long", (long) 42);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("an acceptable configuration is refused : " + e.getMessage());
        }

    }

    /**
     * Empty configuration.
     */
    @Test
    public void testEmptyConfiguration() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");
        Properties p = new Properties();

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }
    }

    /**
     * Empty configuration.
     */
    @Test
    public void testEmptyConfigurationOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2opt");
        Properties p = new Properties();

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }
    }

    /**
     * Empty configuration (just the name).
     */
    @Test
    public void testEmptyConfiguration2() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");
        Properties p = new Properties();
        p.put("instance.name", "ko");
        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            return;
        }

        fail("An unacceptable configuration is accepted");
    }

    /**
     * Empty configuration (just the name).
     */
    @Test
    public void testEmptyConfiguration2opt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");
        Properties p = new Properties();
        p.put("instance.name", "ko");
        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }

    }

    /**
     * Null configuration (accept).
     */
    @Test
    public void testNull() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(null);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }
    }

    /**
     * Null configuration (accept).
     */
    @Test
    public void testNullOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2opt");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(null);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }
    }

    /**
     * Null configuration (fail).
     */
    @Test
    public void testNull2() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(null);
            ci.dispose();
        } catch (Exception e) {
            return;
        }

        fail("An unacceptable configuration is accepted");
    }

    /**
     * Null configuration (success).
     */
    @Test
    public void testNull2Opt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(null);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }


    }

    /**
     * Check static properties.
     */
    @Test
    public void testStaticOK() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("long", 42l);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check static properties.
     */
    @Test
    public void testStaticOKopt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("long", 42l);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check dynamic properties.
     */
    @Test
    public void testDynamicOK() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }


    /**
     * Check dynamic properties.
     */
    @Test
    public void testDynamicOKopt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dynopt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }

        p = new Properties();
        p.put("instance.name", "ok");
        p.put("boolean", true);
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            fail("An acceptable configuration is rejected (2) : " + e.getMessage());
        }
    }

    /**
     * Check inconsistent types.
     */
    @Test
    public void testDynamicBadType() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("long", (long) 42);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check inconsistent types.
     */
    @Test
    public void testDynamicBadTypeOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dynopt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("long", (long) 42);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }

        p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected (2) : " + e.getMessage());
        }
    }

    /**
     * Check good configuration (with overriding).
     */
    @Test
    public void testDynamicComplete() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check good configuration (with overriding).
     */
    @Test
    public void testDynamicCompleteOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }


        p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected (2) : " + e.getMessage());
        }
    }

    /**
     * Check good configuration.
     */
    @Test
    public void testDynamicJustEnough() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check good configuration.
     */
    @Test
    public void testDynamicJustEnoughOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }

        p = new Properties();
        p.put("instance.name", "ok");
        p.put("boolean", true);
        p.put("strAProp", new String[]{"a"});

        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check good configuration.
     */
    @Test
    public void testDynamicMix() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check good configuration.
     */
    @Test
    public void testDynamicMixOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }

        p = new Properties();
        p.put("instance.name", "ok");
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check uncomplete configuration.
     */
    @Test
    public void testDynamicUncomplete() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            return;
        }

        fail("An unacceptable configuration is accepted");
    }

    /**
     * Check uncomplete configuration.
     */
    @Test
    public void testDynamicUncompleteOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }


    }

    /**
     * Check good configuration (more properties).
     */
    @Test
    public void testDynamicMore() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});
        p.put("tralala", "foo");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check good configuration (more properties).
     */
    @Test
    public void testDynamicMoreOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});
        p.put("tralala", "foo");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check properties affecting services and component.
     */
    @Test
    public void testDoubleProps() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});
        p.put("boolean", false);
        p.put("string", "toto");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check properties affecting services and component.
     */
    @Test
    public void testDoublePropsOpt() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2opt");

        Properties p = new Properties();
        p.put("instance.name", "ok");
        p.put("int", 3);
        p.put("boolean", true);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});
        p.put("boolean", false);
        p.put("string", "toto");

        ComponentInstance ci;
        try {
            ci = f.createComponentInstance(p);
            ci.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }

    /**
     * Check instance name unicity.
     */
    @Test
    public void testUnicity1() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");

        ComponentInstance ci1, ci2, ci3;
        try {
            ci1 = f.createComponentInstance(null);
            ci2 = f.createComponentInstance(null);
            ci3 = f.createComponentInstance(null);
            assertThat("Check name ci1, ci2", ci1.getInstanceName(), not(ci2.getInstanceName()));
            assertThat("Check name ci1, ci3", ci1.getInstanceName(), not(ci3.getInstanceName()));
            assertThat("Check name ci3, ci2", ci3.getInstanceName(), not(ci2.getInstanceName()));
            ci1.dispose();
            ci2.dispose();
            ci3.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }
    }

    /**
     * Check instance name unicity.
     */
    @Test
    public void testUnicity2() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");

        ComponentInstance ci1, ci2, ci3;
        try {
            Properties p1 = new Properties();
            p1.put("instance.name", "name1");
            ci1 = f.createComponentInstance(p1);
            Properties p2 = new Properties();
            p2.put("instance.name", "name2");
            ci2 = f.createComponentInstance(p2);
            Properties p3 = new Properties();
            p3.put("instance.name", "name3");
            ci3 = f.createComponentInstance(p3);
            assertThat("Check name ci1, ci2", ci1.getInstanceName(), not(ci2.getInstanceName()));
            assertThat("Check name ci1, ci3", ci1.getInstanceName(), not(ci3.getInstanceName()));
            assertThat("Check name ci3, ci2", ci3.getInstanceName(), not(ci2.getInstanceName()));
            ci1.dispose();
            ci2.dispose();
            ci3.dispose();
        } catch (Exception e) {
            fail("An acceptable configuration is refused");
        }
    }

    /**
     * Check instance name unicity.
     */
    @Test
    public void testUnicity3() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");

        ComponentInstance ci1 = null, ci2;
        try {
            Properties p1 = new Properties();
            p1.put("instance.name", "name1");
            ci1 = f.createComponentInstance(p1);
            Properties p2 = new Properties();
            p2.put("instance.name", "name1");
            ci2 = f.createComponentInstance(p2);
            assertThat("Check name ci1, ci2", ci1.getInstanceName(), not(ci2.getInstanceName()));
            ci1.dispose();
            ci2.dispose();
        } catch (Exception e) {
            if (ci1 != null) {
                ci1.dispose();
            }
            // OK.
            return;
        }

        fail("An unacceptable configuration is acceptable");
    }

    /**
     * Check instance name unicity.
     */
    @Test
    public void testUnicity4() {
        Factory f = ipojoHelper.getFactory("Factories-FooProviderType-2");
        Factory f2 = ipojoHelper.getFactory("Factories-FooProviderType-1");

        ComponentInstance ci1 = null, ci2;
        try {
            Properties p1 = new Properties();
            p1.put("instance.name", "name1");
            ci1 = f.createComponentInstance(p1);
            Properties p2 = new Properties();
            p2.put("instance.name", "name1");
            ci2 = f2.createComponentInstance(p2);
            assertThat("Check name ci1, ci2", ci1.getInstanceName(), not(ci2.getInstanceName()));
            ci1.dispose();
            ci2.dispose();
        } catch (Exception e) {
            if (ci1 != null) {
                ci1.dispose();
            }
            return;
        }

        fail("An unacceptable configuration is acceptable");
    }


}
