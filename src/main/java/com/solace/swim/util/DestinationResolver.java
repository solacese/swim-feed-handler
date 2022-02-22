package com.solace.swim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PostConstruct;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class DestinationResolver implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(DestinationResolver.class);
    private static final long serialVersionUID = 1L;

    @Autowired
    ResourceLoader resourceLoader;

    private String xslFileName;
    private StreamSource styleSource;
    private Transformer transformer;

    public DestinationResolver(String stylesheetTransformationFileName) throws TransformerConfigurationException, IOException {
        xslFileName = "classpath:" + stylesheetTransformationFileName;
    }

    @PostConstruct
    public void init() throws IOException, TransformerConfigurationException {
        Resource xslResource = resourceLoader.getResource(xslFileName);
        //styleSource = new StreamSource(new File((getClass().getClassLoader().getResource(stylesheetTransformationFileName)).getFile()));
        styleSource = new StreamSource(xslResource.getInputStream());
        TransformerFactory factory = TransformerFactory.newInstance();
        transformer = factory.newTransformer(styleSource);
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

            if (logger.isDebugEnabled()) {
                logger.debug("Destination: " + topic.toString());
            }
        } catch (TransformerException e) {
            logger.error(e.getMessage());
        }
        return topic.toString();
    }
}
