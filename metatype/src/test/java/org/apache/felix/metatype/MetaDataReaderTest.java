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
package org.apache.felix.metatype;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.metatype.AttributeDefinition;
import org.xmlpull.v1.XmlPullParserException;

import junit.framework.TestCase;

/**
 * The <code>MetaDataReaderTest</code> class tests the
 * <code>MetaDataReader</code> class.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MetaDataReaderTest extends TestCase
{
    private MetaDataReader reader;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        reader = new MetaDataReader();
    }

    @Override
    protected void tearDown() throws Exception
    {
        reader = null;

        super.tearDown();
    }

    public void testEmpty() throws IOException, XmlPullParserException
    {
        String empty = "<MetaData />";
        MetaData mti = read(empty);

        // Implicitly assume the lowest version...
        assertEquals(MetaDataReader.NAMESPACE_1_0, mti.getNamespace());
        assertNull(mti.getLocalePrefix());
        assertNull(mti.getObjectClassDefinitions());
    }

    public void testOptionalAttributesInMetaData() throws IOException, XmlPullParserException
    {
        String name = "myattribute";
        String value = "working";
        String localization = "test";
        String empty = "<MetaData " + name + "=\"" + value + "\" localization=\"" + localization + "\" />";
        MetaData mti = read(empty);

        assertEquals(localization, mti.getLocalePrefix());
        assertNull(mti.getObjectClassDefinitions());
        assertNotNull(mti.getOptionalAttributes());
        assertEquals(1, mti.getOptionalAttributes().size());
        assertEquals(value, mti.getOptionalAttributes().get(name));
    }

    public void testWithNamespace_1_0_0() throws IOException, XmlPullParserException
    {
        String empty = "<metatype:MetaData xmlns:metatype=\"" + MetaDataReader.NAMESPACE_1_0 + "\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></metatype:MetaData>";
        MetaData mti = read(empty);

        assertNotNull(mti);
        assertEquals(MetaDataReader.NAMESPACE_1_0, mti.getNamespace());
        assertNull(mti.getLocalePrefix());
        assertNull(mti.getObjectClassDefinitions());
    }

    public void testWithNamespace_1_1_0() throws IOException, XmlPullParserException
    {
        String empty = "<metatype:MetaData xmlns:metatype=\"" + MetaDataReader.NAMESPACE_1_1 + "\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></metatype:MetaData>";
        MetaData mti = read(empty);

        assertNotNull(mti);
        assertEquals(MetaDataReader.NAMESPACE_1_1, mti.getNamespace());
        assertNull(mti.getLocalePrefix());
        assertNull(mti.getObjectClassDefinitions());
    }

    public void testWithNamespace_1_2_0() throws IOException, XmlPullParserException
    {
        String empty = "<metatype:MetaData xmlns:metatype=\"" + MetaDataReader.NAMESPACE_1_2 + "\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></metatype:MetaData>";
        MetaData mti = read(empty);

        assertNotNull(mti);
        assertEquals(MetaDataReader.NAMESPACE_1_2, mti.getNamespace());
        assertNull(mti.getLocalePrefix());
        assertNull(mti.getObjectClassDefinitions());
    }

    public void testWithNamespace_1_3_0() throws IOException, XmlPullParserException
    {
        String empty = "<metatype:MetaData xmlns:metatype=\"" + MetaDataReader.NAMESPACE_1_3 + "\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></metatype:MetaData>";
        MetaData mti = read(empty);

        assertNotNull(mti);
        assertEquals(MetaDataReader.NAMESPACE_1_3, mti.getNamespace());
        assertNull(mti.getLocalePrefix());
        assertNull(mti.getObjectClassDefinitions());
    }

    public void testWithInvalidNamespaceUri()
    {
        String empty = "<metatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/datatype/v1.0.0\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></metatype:MetaData>";

        try
        {
            read(empty);
            fail("Parse failure expected for unsupported namespace URI");
        }
        catch (IOException e)
        {
            // expected due to unsupported namespace URI
        }
    }

    public void testWithInvalidNamespaceName()
    {
        String empty = "<datatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/metatype/v1.0.0\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></datatype:MetaData>";

        try
        {
            read(empty);
            fail("Parse failure expected for undefined namespace prefix");
        }
        catch (IOException e)
        {
            // expected due to undefined namespace prefix used
        }
    }

    public void testEmptyLocalization() throws IOException, XmlPullParserException
    {
        String testLoc = "OSGI-INF/folder/base";
        String empty = "<MetaData localization=\"" + testLoc + "\"/>";
        MetaData mti = read(empty);

        assertEquals(testLoc, mti.getLocalePrefix());
    }

    public void testSingleOCDOk() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";

        String empty = "<MetaData><OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription + "\"><AD id=\"attr\" type=\"String\" required=\"false\" /></OCD></MetaData>";
        MetaData mti = read(empty);

        assertNull(mti.getLocalePrefix());
        assertNotNull(mti.getObjectClassDefinitions());
        assertEquals(1, mti.getObjectClassDefinitions().size());

        OCD ocd = (OCD) mti.getObjectClassDefinitions().values().iterator().next();
        assertEquals(ocdId, ocd.getID());
        assertEquals(ocdName, ocd.getName());
        assertEquals(ocdDescription, ocd.getDescription());

        assertNotNull(ocd.getAttributeDefinitions());
    }

    /**
     * FELIX-4665 - Default values can be empty.
     */
    public void testAttributeWithDefaultValueOk() throws IOException, XmlPullParserException
    {
        String xml;

        xml = "<MetaData><OCD id=\"ocd\" name=\"ocd\"><AD id=\"attr\" type=\"String\" required=\"false\" default=\"\" /></OCD></MetaData>";
        MetaData mti = read(xml);
        OCD ocd = (OCD) mti.getObjectClassDefinitions().get("ocd");
        AD ad = (AD) ocd.getAttributeDefinitions().get("attr");
        // Cardinality = 0, so *always* return a array with length 1...
        assertTrue(Arrays.deepEquals(new String[] { "" }, ad.getDefaultValue()));

        xml = "<MetaData><OCD id=\"ocd\" name=\"ocd\"><AD id=\"attr\" cardinality=\"1\" type=\"String\" required=\"false\" /></OCD></MetaData>";
        mti = read(xml);
        ocd = (OCD) mti.getObjectClassDefinitions().get("ocd");
        ad = (AD) ocd.getAttributeDefinitions().get("attr");
        // Cardinality = 1, return an array with *up to* 1 elements...
        assertNull(ad.getDefaultValue());

        xml = "<MetaData><OCD id=\"ocd\" name=\"ocd\"><AD id=\"attr\" cardinality=\"1\" type=\"String\" required=\"false\" default=\"\" /></OCD></MetaData>";
        mti = read(xml);
        ocd = (OCD) mti.getObjectClassDefinitions().get("ocd");
        ad = (AD) ocd.getAttributeDefinitions().get("attr");
        // Cardinality = 1, return an array with *up to* 1 elements...
        assertTrue(Arrays.deepEquals(new String[] { "" }, ad.getDefaultValue()));

        // The metatype spec defines that getDefaultValue should never have more entries than defined in its (non-zero) cardinality...
        xml = "<MetaData><OCD id=\"ocd\" name=\"ocd\"><AD id=\"attr\" cardinality=\"1\" type=\"String\" required=\"false\" default=\",\" /></OCD></MetaData>";
        mti = read(xml);
        ocd = (OCD) mti.getObjectClassDefinitions().get("ocd");
        ad = (AD) ocd.getAttributeDefinitions().get("attr");
        // Cardinality = 1, return an array with *up to* 1 elements...
        assertTrue(Arrays.deepEquals(new String[] { "" }, ad.getDefaultValue()));
    }

    /**
     * FELIX-4644 - Enforce that we can only have one Object in a Designate element.
     */
    public void testOCDWithoutADFail() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";

        String xml = "<MetaData><OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription + "\" /></MetaData>";

        final MetaData md = read(xml);
        assertNull(md.getObjectClassDefinitions());
    }

    /**
     * FELIX-4644 - MetaType v1.3 allows OCDs without ADs.
     */
    public void testOCDWithoutADOk_v13() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";

        String xml = "<metatype:MetaData xmlns:metatype=\"" + MetaDataReader.NAMESPACE_1_3 + "\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ><OCD id=\"" + ocdId + "\" name=\""
            + ocdName + "\" description=\"" + ocdDescription + "\" /></metatype:MetaData>";

        MetaData mti = read(xml);

        assertNull(mti.getLocalePrefix());
        assertNotNull(mti.getObjectClassDefinitions());
        assertEquals(1, mti.getObjectClassDefinitions().size());

        OCD ocd = (OCD) mti.getObjectClassDefinitions().values().iterator().next();
        assertEquals(ocdId, ocd.getID());
        assertEquals(ocdName, ocd.getName());
        assertEquals(ocdDescription, ocd.getDescription());

        assertNull(ocd.getAttributeDefinitions());
    }

    /**
     * FELIX-4644 - Enforce that we can only have one Object in a Designate element.
     */
    public void testDesignateWithoutObjectFail() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";
        String pid = "myPID";

        String xml = "<MetaData><OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription + "\"><AD id=\"attr\" type=\"String\" required=\"false\" /></OCD><Designate pid=\""
            + pid + "\" bundle=\"*\"></Designate></MetaData>";

        final MetaData md = read(xml);
        assertNull(md.getDesignates());
        assertEquals(1, md.getObjectClassDefinitions().size());
    }

    /**
     * FELIX-4644 - Enforce that we can only have one Object in a Designate element.
     */
    public void testDesignateWithObjectOk() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";
        String pid = "myPID";

        String xml = "<MetaData><OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription + "\"><AD id=\"attr\" type=\"String\" required=\"false\" /></OCD><Designate pid=\""
            + pid + "\" bundle=\"*\"><Object ocdref=\"" + ocdId + "\"></Object></Designate></MetaData>";
        MetaData mti = read(xml);

        assertNull(mti.getLocalePrefix());
        assertNotNull(mti.getObjectClassDefinitions());
        assertEquals(1, mti.getObjectClassDefinitions().size());

        OCD ocd = (OCD) mti.getObjectClassDefinitions().values().iterator().next();
        assertEquals(ocdId, ocd.getID());
        assertEquals(ocdName, ocd.getName());
        assertEquals(ocdDescription, ocd.getDescription());

        assertNotNull(ocd.getAttributeDefinitions());

        List designates = ocd.getMetadata().getDesignates();
        assertEquals(1, designates.size());

        Designate designate = (Designate) designates.get(0);
        assertEquals(pid, designate.getPid());
        assertNotNull(designate.getObject());
    }

    /**
     * FELIX-4644 - Enforce that we can only have one Object in a Designate element.
     */
    public void testDesignateWithTwoObjectsFail() throws IOException, XmlPullParserException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";
        String pid = "myPID";

        String xml = "<MetaData><OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription + "\"><AD id=\"attr\" type=\"String\" required=\"false\" /></OCD><Designate pid=\""
            + pid + "\" bundle=\"*\"><Object ocdref=\"" + ocdId + "\"></Object><Object ocdref=\"" + ocdId + "\"></Object></Designate></MetaData>";

        try
        {
            read(xml); // should fail!
            fail("IOException expected!");
        }
        catch (IOException e)
        {
            assertTrue(e.getMessage().contains("Unexpected element Object"));
        }
    }

    /**
     * FELIX-4799 - tests that we can have multiple designates for the same factory PID in one MetaData configuration.
     */
    public void testMultipleFactoryDesignatesWithSamePidOk() throws IOException, XmlPullParserException
    {
        String xml = "<MetaData><OCD id=\"ocd\" name=\"ocd\"><AD id=\"attr\" type=\"String\" required=\"false\" /></OCD>"
            + "<Designate factoryPid=\"factoryA\" pid=\"A\" bundle=\"*\"><Object ocdref=\"ocd\"><Attribute adref=\"attr\" value=\"foo\" /></Object></Designate>"
            + "<Designate factoryPid=\"factoryA\" pid=\"B\" bundle=\"*\"><Object ocdref=\"ocd\"><Attribute adref=\"attr\" value=\"bar\" /></Object></Designate>" + "</MetaData>";

        MetaData metadata = read(xml);
        assertNotNull(metadata);

        assertEquals(1, metadata.getObjectClassDefinitions().size());
        assertEquals(2, metadata.getDesignates().size());
    }

    /**
     * FELIX-4799 - tests that we can have multiple designates for the same factory PID in one MetaData configuration.
     */
    public void testMultipleFactoryDesignatesWithDifferentPidsOk() throws IOException, XmlPullParserException
    {
        String xml = "<MetaData><OCD id=\"ocd\" name=\"ocd\"><AD id=\"attr\" type=\"String\" required=\"false\" /></OCD>"
            + "<Designate factoryPid=\"factoryA\" pid=\"A\" bundle=\"*\"><Object ocdref=\"ocd\"><Attribute adref=\"attr\" value=\"foo\" /></Object></Designate>"
            + "<Designate factoryPid=\"factoryB\" pid=\"A\" bundle=\"*\"><Object ocdref=\"ocd\"><Attribute adref=\"attr\" value=\"bar\" /></Object></Designate>" + "</MetaData>";

        MetaData metadata = read(xml);
        assertNotNull(metadata);

        assertEquals(1, metadata.getObjectClassDefinitions().size());
        assertEquals(2, metadata.getDesignates().size());
    }

    public void testSingleOCDSingleRequiredAttr() throws IOException, XmlPullParserException
    {
        testSingleOCDSingleRequiredAttr("String", AttributeDefinition.STRING, MetaDataReader.NAMESPACE_1_0);
        testSingleOCDSingleRequiredAttr("String", AttributeDefinition.STRING, MetaDataReader.NAMESPACE_1_1);
        testSingleOCDSingleRequiredAttr("String", AttributeDefinition.STRING, MetaDataReader.NAMESPACE_1_2);
        testSingleOCDSingleRequiredAttr("String", AttributeDefinition.STRING, MetaDataReader.NAMESPACE_1_3);

        testSingleOCDSingleRequiredAttr("Char", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_0);
        testSingleOCDSingleRequiredAttr("Char", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_1);
        testSingleOCDSingleRequiredAttr("Char", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_2);
        testSingleOCDSingleRequiredAttr("Char", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_3);

        testSingleOCDSingleRequiredAttr("Character", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_0);
        testSingleOCDSingleRequiredAttr("Character", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_1);
        testSingleOCDSingleRequiredAttr("Character", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_2);
        testSingleOCDSingleRequiredAttr("Character", AttributeDefinition.CHARACTER, MetaDataReader.NAMESPACE_1_3);

    }

    private void testSingleOCDSingleRequiredAttr(String adType, int typeCode, String namespace) throws IOException
    {
        String ocdName = "ocd0";
        String ocdId = "id.ocd0";
        String ocdDescription = "ocd0 description";

        String adId = "id.ad0";
        String adName = "ad0";
        String adDescription = "ad0 description";
        int adCardinality = 789;
        String adDefault = "    a    ,   b    ,    c    ";

        String empty = "<metatype:MetaData xmlns:metatype=\"" + namespace + "\">" + "<OCD id=\"" + ocdId + "\" name=\"" + ocdName + "\" description=\"" + ocdDescription + "\">" + "<AD id=\"" + adId
            + "\" name=\"" + adName + "\" type=\"" + adType + "\" description=\"" + adDescription + "\" cardinality=\"" + adCardinality + "\" default=\"" + adDefault + "\">" + "</AD>" + "</OCD>"
            + "</metatype:MetaData>";
        MetaData mti = read(empty);

        assertNull(mti.getLocalePrefix());
        assertNotNull(mti.getObjectClassDefinitions());
        assertEquals(1, mti.getObjectClassDefinitions().size());

        OCD ocd = (OCD) mti.getObjectClassDefinitions().values().iterator().next();

        assertNotNull(ocd.getAttributeDefinitions());
        assertEquals(1, ocd.getAttributeDefinitions().size());

        AD ad = (AD) ocd.getAttributeDefinitions().values().iterator().next();
        assertEquals(adId, ad.getID());
        assertEquals(adName, ad.getName());
        assertEquals(adDescription, ad.getDescription());
        assertEquals(typeCode, ad.getType());
        assertEquals(adCardinality, ad.getCardinality());
        assertNotNull(ad.getDefaultValue());
        assertEquals(3, ad.getDefaultValue().length);

        String[] defaultValue = ad.getDefaultValue();
        assertEquals("a", defaultValue[0]);
        assertEquals("b", defaultValue[1]);
        assertEquals("c", defaultValue[2]);
    }

    private MetaData read(String data) throws IOException
    {
        InputStream input = new ByteArrayInputStream(data.getBytes("UTF-8"));
        return reader.parse(input);
    }
}
