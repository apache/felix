package org.apache.felix.ipojo.util;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;

/**
 * Test the {@link ManifestMetadataParser}
 */
public class ManifestMetadataParserTest extends TestCase {

	/**
	 * Test FELIX-2685.
	 * The element name contains ':' as in:
	 * "//jasmine.ow2.org/rules/1.0.0:configuration"
	 * @throws ParseException
	 */
	public void testNameWithColumn() throws ParseException {
		// Create a test element
		String header = "mynamespace://jasmine.ow2.org/rules/1.0.0:configuration {}";
		Element elem = ManifestMetadataParser.parse(header);

		Assert.assertEquals("mynamespace", elem.getNameSpace());
		Assert.assertEquals("//jasmine.ow2.org/rules/1.0.0:configuration", elem.getName());
	}

	/**
	 * Check the parsing of handler element using {@link ManifestMetadataParser#parseHeader(String)}
	 * @throws ParseException
	 */
	public void testHandlerHeader() throws ParseException {
		String header = "handler { $name=\"wbp\" $classname=\"org.apache.felix.ipojo.handler.wbp.WhiteBoardPatternHandler\"" +
				" $namespace=\"org.apache.felix.ipojo.whiteboard\" manipulation { $super=\"org.apache.felix.ipojo.PrimitiveHandler\"" +
				" field { $name=\"m_managers\" $type=\"java.util.List\" }method { $name=\"$init\" }method { $arguments=" +
				"\"{org.apache.felix.ipojo.metadata.Element,java.util.Dictionary}\" $name=\"configure\" }method { $name=\"start\"" +
				" }method { $arguments=\"{int}\" $name=\"stateChanged\" }method { $name=\"stop\" }}}";

		ManifestMetadataParser parser = new ManifestMetadataParser();
		parser.parseHeader(header);

		Element[] elems = parser.getComponentsMetadata();
		Assert.assertEquals(1, elems.length);

		Element element = elems[0];
		Assert.assertEquals("handler", element.getName());
		Assert.assertNull(element.getNameSpace());

		Assert.assertEquals("wbp", element.getAttribute("name"));
		Assert.assertEquals("org.apache.felix.ipojo.handler.wbp.WhiteBoardPatternHandler", element.getAttribute("classname"));
		Assert.assertEquals("org.apache.felix.ipojo.whiteboard", element.getAttribute("namespace"));

		// Check the manipulation element
		Element[] manip = element.getElements("manipulation");
		Assert.assertNotNull(manip[0]);

		Element[] methods = manip[0].getElements("method");
		Assert.assertEquals(5, methods.length);
	}

	/**
	 * Check the parsing of handler element using {@link ManifestMetadataParser#parseHeaderMetadata(String)}
	 * @throws ParseException
	 */
	public void testHandlerHeader2() throws ParseException {
		String header = "handler { $name=\"wbp\" $classname=\"org.apache.felix.ipojo.handler.wbp.WhiteBoardPatternHandler\"" +
				" $namespace=\"org.apache.felix.ipojo.whiteboard\" manipulation { $super=\"org.apache.felix.ipojo.PrimitiveHandler\"" +
				" field { $name=\"m_managers\" $type=\"java.util.List\" }method { $name=\"$init\" }method { $arguments=" +
				"\"{org.apache.felix.ipojo.metadata.Element,java.util.Dictionary}\" $name=\"configure\" }method { $name=\"start\"" +
				" }method { $arguments=\"{int}\" $name=\"stateChanged\" }method { $name=\"stop\" }}}";

		// This method returns an iPOJO root element
		Element elem = ManifestMetadataParser.parseHeaderMetadata(header);
		Element element = elem.getElements("handler")[0];

		Assert.assertEquals("handler", element.getName());
		Assert.assertNull(element.getNameSpace());

		Assert.assertEquals("wbp", element.getAttribute("name"));
		Assert.assertEquals("org.apache.felix.ipojo.handler.wbp.WhiteBoardPatternHandler", element.getAttribute("classname"));
		Assert.assertEquals("org.apache.felix.ipojo.whiteboard", element.getAttribute("namespace"));

		// Check the manipulation element
		Element[] manip = element.getElements("manipulation");
		Assert.assertNotNull(manip[0]);

		Element[] methods = manip[0].getElements("method");
		Assert.assertEquals(5, methods.length);
	}

}
