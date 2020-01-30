package com.solace.swim.util;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

public class MessageUtil {

    public static String getHeaders(Map<String, ?> headers) {
        StringBuilder builder = new StringBuilder();
        for (String key: headers.keySet()) {
            builder.append(key + "=" + headers.get(key));
            builder.append("\n");
        }
        return builder.toString();
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


}
