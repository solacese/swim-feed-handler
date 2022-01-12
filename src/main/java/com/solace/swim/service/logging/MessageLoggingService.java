package com.solace.swim.service.logging;
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


import com.solace.swim.service.IService;
import com.solace.swim.util.MessageUtil;
import com.solacesystems.jms.message.SolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "service.message-logging", value = "enabled", havingValue = "true")
public class MessageLoggingService implements IService {

    private static final Logger logger = LoggerFactory.getLogger(MessageLoggingService.class);

    // Determine if the message headers should also be written to file
    @Value("${service.message-logging.write-headers:true}")
    private boolean writeHeaders;


    @Override
    public void invoke(Message<?> message) {
        String payload;
        if (message.getPayload() instanceof String) {
            payload = (String)message.getPayload();
        } else if (message.getPayload() instanceof SolMessage) {
            SolMessage obj = (SolMessage) message.getPayload();
            payload = obj.dump();
        } else {
            payload = message.getPayload().toString();
        }

        StringBuilder builder = new StringBuilder();
        if (writeHeaders) {
            builder.append("<!--");
            builder.append(MessageUtil.getHeadersAsJSON(message.getHeaders()));
            builder.append("-->\n");
        }
        builder.append(payload);

        logger.info(builder.toString());
    }

}
