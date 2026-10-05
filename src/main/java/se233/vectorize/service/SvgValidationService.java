// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se233.vectorize.exception.ConversionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/** Phase 4/7 gate shared by preview and export; FR-017, NFR-005. */
public final class SvgValidationService {
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final List<String> FORBIDDEN_TAGS =
            List.of("image", "script", "foreignobject", "iframe", "object", "embed");
    private static final List<String> SHAPE_TAGS =
            List.of("path", "rect", "circle", "ellipse", "polygon", "polyline", "line");

    /**
     * Reject malformed, empty, unsafe, or raster-wrapped results.
     * Checks, in order: XML parses; root is an SVG-namespace svg element with a
     * size; no raster/script/embedded elements, event attributes or external
     * links; at least one real vector shape. Run off the FX thread.
     */
    public void validate(String svg) throws ConversionException {
        if (svg == null || svg.isBlank()) {
            throw fail("The engine returned an empty SVG.");
        }

        Document document;
        try {
            document = parse(svg);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConversionException(ConversionException.Kind.SVG, "The engine returned an " +
                    "SVG that could not be read (broken XML).", e);
        }

        Element root = document.getDocumentElement();
        if (!"svg".equals(root.getLocalName()) || !SVG_NAMESPACE.equals(root.getNamespaceURI())) {
            throw fail("The result is not an SVG document.");
        }
        if (!hasSize(root)) {
            throw fail("The SVG has no usable width/height or viewBox.");
        }

        int shapes = 0;
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            String tag = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
            tag = tag.toLowerCase();

            if (FORBIDDEN_TAGS.contains(tag)) {
                throw fail("The SVG contains a <" + tag + "> element, which is not allowed (raster or unsafe content).");
            }
            checkAttributes(element, tag);

            if (SHAPE_TAGS.contains(tag)) {
                if (!tag.equals("path") || !element.getAttribute("d").isBlank()) shapes++;
            }
        }
        if (shapes == 0) {
            throw fail("The SVG has no vector shapes.");
        }
    }

    /** Parse without loading DTDs or external entities (blocks XXE tricks). */
    private Document parse(String svg) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        return builder.parse(new InputSource(new StringReader(svg)));
    }

    /** True when the root has a positive width and height, or a viewBox. */
    private boolean hasSize(Element root) {
        if (!root.getAttribute("viewBox").isBlank()) return true;
        return positiveNumber(root.getAttribute("width")) && positiveNumber(root.getAttribute("height"));
    }

    private boolean positiveNumber(String value) {
        String number = value.trim().replace("px", "");
        if (number.isEmpty()) return false;
        try {
            return Double.parseDouble(number) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** No onload/onclick handlers, no links leaving the file, no data:/javascript: URLs. */
    private void checkAttributes(Element element, String tag) throws ConversionException {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String name = attribute.getLocalName() == null ? attribute.getNodeName() : attribute.getLocalName();
            name = name.toLowerCase();
            String value = attribute.getNodeValue().trim().toLowerCase();

            if (name.startsWith("on")) {
                throw fail("The SVG contains a script handler (" + name + ") on <" + tag + ">.");
            }
            if (name.equals("href") && !value.startsWith("#")) {
                throw fail("The SVG links to outside content, which is not allowed.");
            }
            if (value.contains("javascript:") || value.contains("data:")) {
                throw fail("The SVG contains embedded or script content, which is not allowed.");
            }
        }
    }

    private ConversionException fail(String message) {
        return new ConversionException(ConversionException.Kind.SVG, message);
    }
}
