package com.solace.swim.util;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class DestinationResolver implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(DestinationResolver.class);
    private static final long serialVersionUID = 1L;

    @Autowired
    ResourceLoader resourceLoader;

    private String xslFileName;
    private StreamSource styleSource;
    private Transformer transformer;

    private DocumentBuilder builder;
    private XPath xPath;

    public DestinationResolver(String stylesheetTransformationFileName) throws TransformerConfigurationException, IOException {
        xslFileName = "classpath:" + stylesheetTransformationFileName;
    }

    @PostConstruct
    public void init() throws IOException, TransformerConfigurationException, ParserConfigurationException {
        Resource xslResource = resourceLoader.getResource(xslFileName);
        //styleSource = new StreamSource(new File((getClass().getClassLoader().getResource(stylesheetTransformationFileName)).getFile()));
        styleSource = new StreamSource(xslResource.getInputStream());
        TransformerFactory factory = TransformerFactory.newInstance();
        transformer = factory.newTransformer(styleSource);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builder = builderFactory.newDocumentBuilder();

        xPath = XPathFactory.newInstance().newXPath();
    }

    public String computeDestination(String payload) {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering computeDestination...");
        }

        StringBuffer topic = new StringBuffer();
        StreamSource source = new StreamSource(new ByteArrayInputStream(payload.getBytes()));

        StreamResult result = null;
        try {
            result = new StreamResult(new ByteArrayOutputStream());
            transformer.transform(source, result);

            // Remove all empty spaces in the topic name
            topic.append(result.getOutputStream().toString().replaceAll("\\s+", ""));
            topic.append("/");
            topic.append(calculateHeading(payload));

            if (logger.isDebugEnabled()) {
                logger.debug("Destination: " + topic.toString());
            }
        } catch (TransformerException e) {
            logger.error(e.getMessage());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
        return topic.toString();
    }

    private String calculateHeading(String source) throws IOException, SAXException, XPathExpressionException {
        Document doc = builder.parse(new InputSource(new StringReader(source)));

        Number xVelo = (Number) xPath.evaluate("message/flight/enRoute/position/trackVelocity/x", doc, XPathConstants.NUMBER);
        Number yVelo = (Number) xPath.evaluate("message/flight/enRoute/position/trackVelocity/y", doc, XPathConstants.NUMBER);

        double theta = Math.atan2(xVelo.doubleValue(), yVelo.doubleValue());

        double heading = (theta * 180) / Math.PI;

        if (heading < 0) {
            heading += 360.0;
        }

        return String.format("%03d", (int)heading);
    }
}
