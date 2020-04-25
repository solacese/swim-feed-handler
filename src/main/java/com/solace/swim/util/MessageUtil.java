package com.solace.swim.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

public class MessageUtil {
    private static final Logger logger = LoggerFactory.getLogger(MessageUtil.class);

    public static String getHeaders(Map<String, ?> headers) {
        StringBuilder builder = new StringBuilder();
        for (String key: headers.keySet()) {
            builder.append(key + "=" + headers.get(key));
            builder.append("\n");
        }
        return builder.toString();
    }

    public static String getHeaderValue(Map<String, ?> headers, String Key)
    {
        try {
            if(headers.containsKey(Key))
                return headers.get(Key).toString();
        } catch (Exception ex) {
            return "Error in generating header value for Key " + Key;
        }
        return Key + " not found.";
    }

    public static String getHeaders(org.springframework.messaging.Message msg) {
        try {
            StringBuilder builder = new StringBuilder();

            Enumeration propertyNames = null;
            Set<String> headers = msg.getHeaders().keySet();

            for(String key: headers) {
                builder.append(key + "=" + msg.getHeaders().get(key));
                builder.append("\n");
            }
            return builder.toString();
        } catch (Exception ex) {
            return "Error in generating headers";
        }
    }

    public static String getHeaders(javax.jms.Message msg) {
        try {
            StringBuilder builder = new StringBuilder();

            Enumeration<String> propertyNames = msg.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String property = propertyNames.nextElement();
                builder.append(property + "=" + msg.getStringProperty(property));
                builder.append("\n");
            }
            builder.append("jms_deliveryMode=" + msg.getJMSDeliveryMode());
            builder.append("jms_messageID=" + msg.getJMSMessageID());

            return builder.toString();
        } catch (Exception ex) {
            return "Error in generating headers";
        }
    }

    public static String getHeadersAsJSON(Map<String, ?> headers) {
        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert header map to JSON: {}", headers);
            json = "";
        }
        return json;
    }


}
