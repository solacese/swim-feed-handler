package com.solace.swim.util;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

public class MessageUtil {
    private static final Logger logger = LoggerFactory.getLogger(MessageUtil.class);

    public static String getHeaders(Map<String, ?> headers) {
        StringBuilder builder = new StringBuilder();
        for (String key: headers.keySet()) {
            builder.append(key).append("=").append(headers.get(key)).append("\n");
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

    public static String getHeaders(org.springframework.messaging.Message<?> msg) {
        return getHeaders(msg.getHeaders());
    }

    public static String getHeaders(javax.jms.Message msg) {
        try {
            StringBuilder builder = new StringBuilder();

            Enumeration<String> propertyNames = msg.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String property = propertyNames.nextElement();
                builder.append(property).append("=").append(msg.getStringProperty(property)).append("\n");
            }
            builder.append("jms_deliveryMode=").append(msg.getJMSDeliveryMode()).append("\n");
            builder.append("jms_messageID=").append(msg.getJMSMessageID()).append("\n");

            return builder.toString();
        } catch (Exception ex) {
            return "Error in generating headers";
        }
    }

    public static String getHeadersAsJSON(Map<String, ?> headers) {
        String json;
        try {
            json = new ObjectMapper().writeValueAsString(new TreeMap<String, Object>(headers));
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert header map to JSON: {}", headers);
            json = "";
        }
        return json;
    }


}
