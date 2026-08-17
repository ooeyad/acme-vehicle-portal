package com.acme.web.filter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.servlet.Filter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the web.xml registration of NoStoreCacheFilter.
 *
 * The unit tests in NoStoreCacheFilterTest construct the filter directly, so they prove the
 * filter behaves - not that anything invokes it. Delete the filter-mapping, or misspell the
 * filter-class as com.acme.web.filters.NoStoreCacheFilter, and every one of those tests still
 * passes while no vehicle response carries a single cache header. That gap is what this closes.
 *
 * Same idea as JdbcVehicleDaoSqlConventionsTest: pin a cross-cutting rule that no ordinary unit
 * test would catch, in the normal build, so it fails on the developer's machine rather than in
 * a reviewer's queue. web.xml is human-edited under security review (see CLAUDE.md), which is
 * precisely why the registration needs an automated check - the reviewer signs off on the
 * descriptor, this asserts the descriptor still says what the code depends on it saying.
 */
public class NoStoreCacheFilterDescriptorTest {

    private static final String FILTER_CLASS = "com.acme.web.filter.NoStoreCacheFilter";
    private static final String URL_PATTERN = "/vehicle/*";

    private static Document descriptor;

    @BeforeClass
    public static void parseDescriptor() throws Exception {
        File file = locateDescriptor();
        assertTrue("web.xml not found - looked at " + file.getAbsolutePath(), file.isFile());

        // Namespace-unaware on purpose: the descriptor declares the javaee namespace by default,
        // and matching literal tag names keeps the assertions readable.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Never reach the network for a schema or DTD during a unit test.
        builder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) {
                return new InputSource(new StringReader(""));
            }
        });
        descriptor = builder.parse(file);
    }

    /** Surefire runs from the module directory; an IDE may run from the repository root. */
    private static File locateDescriptor() {
        String relative = "src/main/webapp/WEB-INF/web.xml";
        File fromModule = new File(relative);
        return fromModule.isFile() ? fromModule : new File("legacy-web", relative);
    }

    /**
     * Guards the parsing itself. Without this, a descriptor that failed to load - or loaded
     * empty - would leave every other assertion in this class passing against nothing. The
     * same reason JdbcVehicleDaoSqlConventionsTest checks that its reflection found queries.
     */
    @Test
    public void theDescriptorParsedAndIsTheRealOne() {
        assertNotNull("descriptor did not parse", descriptor);
        assertEquals("root element is not web-app",
                "web-app", descriptor.getDocumentElement().getNodeName());
        assertTrue("no <servlet> found - this is not the portal's web.xml",
                descriptor.getElementsByTagName("servlet").getLength() >= 1);
    }

    @Test
    public void theNoStoreFilterIsDeclared() {
        assertNotNull("no <filter> declares filter-class " + FILTER_CLASS
                    + " - the filter is dead code until web.xml names it",
                declaredFilterName());
    }

    @Test
    public void theDeclaredFilterClassActuallyExistsAndIsAFilter() throws Exception {
        // Catches the typo case the unit tests cannot: web.xml naming a class that is not there.
        Class<?> declared = Class.forName(FILTER_CLASS);
        assertTrue(FILTER_CLASS + " is declared in web.xml but does not implement javax.servlet.Filter",
                Filter.class.isAssignableFrom(declared));
    }

    @Test
    public void theFilterIsMappedToTheVehicleScreens() {
        String filterName = declaredFilterName();
        assertNotNull("filter is not declared, so it cannot be mapped", filterName);

        List<String> patterns = new ArrayList<String>();
        NodeList mappings = descriptor.getElementsByTagName("filter-mapping");
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            if (filterName.equals(childText(mapping, "filter-name"))) {
                NodeList urls = mapping.getElementsByTagName("url-pattern");
                for (int j = 0; j < urls.getLength(); j++) {
                    patterns.add(text(urls.item(j)));
                }
            }
        }

        assertTrue("no <filter-mapping> binds filter '" + filterName + "' to " + URL_PATTERN
                 + " - vehicle responses would carry no cache headers. Found: " + patterns,
                patterns.contains(URL_PATTERN));
    }

    /** @return the filter-name of the filter declaring FILTER_CLASS, or null if absent. */
    private static String declaredFilterName() {
        NodeList filters = descriptor.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (FILTER_CLASS.equals(childText(filter, "filter-class"))) {
                return childText(filter, "filter-name");
            }
        }
        return null;
    }

    /** @return the trimmed text of the first child element with this tag, or null. */
    private static String childText(Element parent, String tag) {
        NodeList found = parent.getElementsByTagName(tag);
        return found.getLength() == 0 ? null : text(found.item(0));
    }

    private static String text(Node node) {
        String content = node.getTextContent();
        return content == null ? null : content.trim();
    }
}
